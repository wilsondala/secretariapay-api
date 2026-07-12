package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayDispatchBatchResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecretariaPayMessageDispatchService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final Pattern PDF_URL_PATTERN = Pattern.compile("(https?://\\S+/pdf(?:\\?\\S*)?)");

    private final SecretariaPayMessageRepository repository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final boolean whatsappEnabled;

    public SecretariaPayMessageDispatchService(
            SecretariaPayMessageRepository repository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            @Value("${secretariapay.whatsapp.enabled:false}") boolean whatsappEnabled
    ) {
        this.repository = repository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.whatsappEnabled = whatsappEnabled;
    }

    @Transactional
    public SecretariaPayMessageDispatchResult queue(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId);
        message.setStatus(SecretariaPayMessageStatus.QUEUED).setFailureReason(null);
        return toResult(repository.save(message), LocalDateTime.now());
    }

    @Transactional
    public SecretariaPayMessageDispatchResult dispatch(UUID messageId) {
        return dispatchMessage(findMessage(messageId));
    }

    @Transactional
    public SecretariaPayDispatchBatchResponse processQueue(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        List<SecretariaPayMessage> queuedMessages = repository
                .findTop20ByStatusOrderByCreatedAtAsc(SecretariaPayMessageStatus.QUEUED)
                .stream()
                .limit(safeLimit)
                .toList();

        List<SecretariaPayMessageDispatchResult> results = new ArrayList<>();
        int sent = 0;
        int failed = 0;

        for (SecretariaPayMessage message : queuedMessages) {
            SecretariaPayMessageDispatchResult result = dispatchMessage(message);
            results.add(result);
            if (SecretariaPayMessageStatus.SENT.name().equals(result.getStatus())) sent++;
            if (SecretariaPayMessageStatus.FAILED.name().equals(result.getStatus())) failed++;
        }

        return new SecretariaPayDispatchBatchResponse()
                .setProcessed(results.size())
                .setSent(sent)
                .setFailed(failed)
                .setResults(results);
    }

    private SecretariaPayMessageDispatchResult dispatchMessage(SecretariaPayMessage message) {
        LocalDateTime now = LocalDateTime.now();

        if (message.getStatus() == SecretariaPayMessageStatus.SENT) {
            return toResult(message, now);
        }

        if (message.getRecipientPhone() == null || message.getRecipientPhone().isBlank()) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Mensagem sem número de WhatsApp do destinatário.");
            return toResult(repository.save(message), now);
        }

        if (!whatsappEnabled) {
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId("mock-whatsapp-" + message.getId())
                    .setFailureReason(null)
                    .setSentAt(now);
            return toResult(repository.save(message), now);
        }

        WhatsAppCloudSendResult sendResult = sendByMessageType(message);
        if (sendResult.isSuccess()) {
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId(sendResult.getProviderMessageId())
                    .setFailureReason(null)
                    .setSentAt(now);
            return toResult(repository.save(message), now);
        }

        message.setStatus(SecretariaPayMessageStatus.FAILED)
                .setFailureReason(sendResult.getErrorMessage());
        return toResult(repository.save(message), now);
    }

    private WhatsAppCloudSendResult sendByMessageType(SecretariaPayMessage message) {
        if ("PAYMENT_GUIDE".equalsIgnoreCase(safe(message.getType()))
                && !safe(message.getChargeCode()).isBlank()) {
            return sendPaymentGuide(message);
        }

        if ("RECEIPT_ISSUED".equalsIgnoreCase(safe(message.getType()))
                && !safe(message.getReceiptCode()).isBlank()) {
            String pdfUrl = withCacheBuster(resolveReceiptPdfUrl(message));
            return whatsAppCloudApiClient.sendDocumentByLink(
                    message.getRecipientPhone(),
                    pdfUrl,
                    "Comprovativo_Pagamentos_" + safeFilePart(message.getStudentNumber()) + "_" + safeFilePart(message.getReceiptCode()) + ".pdf",
                    buildReceiptCaption(message, pdfUrl)
            );
        }

        return whatsAppCloudApiClient.sendText(message.getRecipientPhone(), message.getMessage());
    }

    private WhatsAppCloudSendResult sendPaymentGuide(SecretariaPayMessage message) {
        String originalPdfUrl = resolvePaymentGuidePdfUrl(message);
        String freshPdfUrl = withCacheBuster(originalPdfUrl);
        String fileName = "Guia_Pagamento_Academico_"
                + safeFilePart(message.getStudentNumber()) + "_"
                + safeFilePart(message.getChargeCode()) + ".pdf";

        WhatsAppCloudSendResult documentResult = whatsAppCloudApiClient.sendDocumentByLink(
                message.getRecipientPhone(),
                freshPdfUrl,
                fileName,
                "Guia de Pagamento Académico oficial em PDF."
        );

        if (!documentResult.isSuccess()) {
            return documentResult;
        }

        String fullMessage = buildPaymentGuideMessage(message, originalPdfUrl, freshPdfUrl);
        WhatsAppCloudSendResult textResult = whatsAppCloudApiClient.sendText(
                message.getRecipientPhone(),
                fullMessage
        );

        if (!textResult.isSuccess()) {
            return WhatsAppCloudSendResult.failed(
                    "O PDF foi enviado, mas a mensagem institucional falhou: " + textResult.getErrorMessage(),
                    textResult.getHttpStatus()
            );
        }

        return WhatsAppCloudSendResult.sent(
                documentResult.getProviderMessageId() + "," + textResult.getProviderMessageId(),
                textResult.getHttpStatus()
        );
    }

    private String resolvePaymentGuidePdfUrl(SecretariaPayMessage message) {
        Matcher matcher = PDF_URL_PATTERN.matcher(safe(message.getMessage()));
        if (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (candidate.contains("/api/v1/public/payment-guides/")) {
                int queryIndex = candidate.indexOf('?');
                return queryIndex >= 0 ? candidate.substring(0, queryIndex) : candidate;
            }
        }

        return API_BASE_URL + "/api/v1/public/payment-guides/" + message.getChargeCode() + "/pdf";
    }

    private String resolveReceiptPdfUrl(SecretariaPayMessage message) {
        Matcher matcher = PDF_URL_PATTERN.matcher(safe(message.getMessage()));
        if (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (candidate.contains("/api/v1/public/receipts/")) {
                int queryIndex = candidate.indexOf('?');
                return queryIndex >= 0 ? candidate.substring(0, queryIndex) : candidate;
            }
        }

        return API_BASE_URL + "/api/v1/public/receipts/" + message.getReceiptCode() + "/pdf";
    }

    private String buildPaymentGuideMessage(
            SecretariaPayMessage message,
            String originalPdfUrl,
            String freshPdfUrl
    ) {
        String configuredMessage = safe(message.getMessage()).trim();
        if (!configuredMessage.isBlank()) {
            if (configuredMessage.contains(originalPdfUrl)) {
                return configuredMessage.replace(originalPdfUrl, freshPdfUrl);
            }
            return configuredMessage + "\n\nGuia digital atualizada:\n" + freshPdfUrl;
        }

        return ("Guia de Pagamento Académico disponível.\n\n"
                + "Estudante: %s\n"
                + "Matrícula: %s\n"
                + "Cobrança: %s\n\n"
                + "O PDF oficial segue em anexo. Efetue o pagamento até ao vencimento para evitar multas, bloqueios ou outros constrangimentos académicos.\n\n"
                + "Caso o pagamento já tenha sido efetuado, envie o comprovativo por este canal.\n\n"
                + "Guia digital:\n%s\n\n"
                + "Secretaria Financeira\n"
                + "IMETRO\n"
                + "Powered by SecretáriaPay")
                .formatted(
                        safe(message.getStudentName()),
                        safe(message.getStudentNumber()),
                        safe(message.getChargeCode()),
                        freshPdfUrl
                );
    }

    private String buildReceiptCaption(SecretariaPayMessage message, String pdfUrl) {
        return ("Comprovativo de Pagamentos emitido.\n\n"
                + "Comprovativo nº: %s\n"
                + "Estudante: %s\n"
                + "Cobrança: %s\n\n"
                + "O PDF oficial segue em anexo.\n"
                + "Link público: %s\n\n"
                + "SecretáriaPay Académico")
                .formatted(
                        safe(message.getReceiptCode()),
                        safe(message.getStudentName()),
                        safe(message.getChargeCode()),
                        pdfUrl
                );
    }

    private String withCacheBuster(String url) {
        if (url == null || url.isBlank()) return url;
        return url + (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
    }

    private String safeFilePart(String value) {
        String sanitized = safe(value)
                .trim()
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    private SecretariaPayMessage findMessage(UUID messageId) {
        return repository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem do SecretáriaPay não encontrada."));
    }

    private SecretariaPayMessageDispatchResult toResult(
            SecretariaPayMessage message,
            LocalDateTime processedAt
    ) {
        return new SecretariaPayMessageDispatchResult()
                .setMessageId(message.getId())
                .setStatus(message.getStatus() != null ? message.getStatus().name() : null)
                .setProviderMessageId(message.getProviderMessageId())
                .setFailureReason(message.getFailureReason())
                .setRecipientPhone(message.getRecipientPhone())
                .setProcessedAt(processedAt);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
