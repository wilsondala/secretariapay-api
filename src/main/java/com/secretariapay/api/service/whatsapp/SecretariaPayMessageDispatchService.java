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
    private static final Pattern PDF_URL_PATTERN = Pattern.compile("(https?://\\S+/pdf)");

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
        SecretariaPayMessage saved = repository.save(message);
        return toResult(saved, LocalDateTime.now());
    }

    @Transactional
    public SecretariaPayMessageDispatchResult dispatch(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId);
        return dispatchMessage(message);
    }

    @Transactional
    public SecretariaPayDispatchBatchResponse processQueue(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 20);
        List<SecretariaPayMessage> queuedMessages = repository.findTop20ByStatusOrderByCreatedAtAsc(SecretariaPayMessageStatus.QUEUED)
                .stream().limit(safeLimit).toList();

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

        if (message.getRecipientPhone() == null || message.getRecipientPhone().isBlank()) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Mensagem sem número de WhatsApp do destinatário.");
            return toResult(repository.save(message), now);
        }

        if (!whatsappEnabled) {
            String mockProviderMessageId = "mock-whatsapp-" + message.getId();
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId(mockProviderMessageId)
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
        if (isReceiptIssuedMessage(message)) {
            String receiptCode = safe(message.getReceiptCode());
            String pdfUrl = resolvePublicReceiptPdfUrl(message);
            String filename = receiptCode.isBlank()
                    ? "recibo-secretariapay.pdf"
                    : "recibo-" + receiptCode + ".pdf";

            String caption = buildReceiptDocumentCaption(message, pdfUrl);

            return whatsAppCloudApiClient.sendDocumentByLink(
                    message.getRecipientPhone(),
                    pdfUrl,
                    filename,
                    caption
            );
        }

        return whatsAppCloudApiClient.sendText(message.getRecipientPhone(), message.getMessage());
    }

    private boolean isReceiptIssuedMessage(SecretariaPayMessage message) {
        return message != null
                && "RECEIPT_ISSUED".equalsIgnoreCase(safe(message.getType()))
                && !safe(message.getReceiptCode()).isBlank();
    }

    private String resolvePublicReceiptPdfUrl(SecretariaPayMessage message) {
        String messageText = safe(message.getMessage());

        Matcher matcher = PDF_URL_PATTERN.matcher(messageText);
        if (matcher.find()) {
            String candidate = matcher.group(1).trim();

            if (candidate.contains("/api/v1/public/receipts/")) {
                return candidate;
            }
        }

        return API_BASE_URL + "/api/v1/public/receipts/" + message.getReceiptCode() + "/pdf";
    }

    private String buildReceiptDocumentCaption(SecretariaPayMessage message, String pdfUrl) {
        StringBuilder caption = new StringBuilder();

        caption.append("Recibo digital emitido.");
        caption.append("\n\nRecibo nº: ").append(safe(message.getReceiptCode()));

        if (!safe(message.getStudentName()).isBlank()) {
            caption.append("\nEstudante: ").append(message.getStudentName());
        }

        if (!safe(message.getChargeCode()).isBlank()) {
            caption.append("\nCobrança: ").append(message.getChargeCode());
        }

        caption.append("\n\nO PDF do recibo segue em anexo.");
        caption.append("\nLink público: ").append(pdfUrl);
        caption.append("\n\nSecretáriaPay Académico");

        return caption.toString();
    }

    private SecretariaPayMessage findMessage(UUID messageId) {
        return repository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem do SecretáriaPay não encontrada."));
    }

    private SecretariaPayMessageDispatchResult toResult(SecretariaPayMessage message, LocalDateTime processedAt) {
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
