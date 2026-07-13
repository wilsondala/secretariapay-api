package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WhatsAppCloudApiClient {

    private static final int MAX_INTERACTIVE_ROWS = 10;
    private static final int MAX_HEADER_LENGTH = 60;
    private static final int MAX_BODY_LENGTH = 1024;
    private static final int MAX_FOOTER_LENGTH = 60;
    private static final int MAX_BUTTON_LENGTH = 20;
    private static final int MAX_SECTION_TITLE_LENGTH = 24;
    private static final int MAX_ROW_ID_LENGTH = 200;
    private static final int MAX_ROW_TITLE_LENGTH = 24;
    private static final int MAX_ROW_DESCRIPTION_LENGTH = 72;

    private final RestClient restClient;
    private final String graphApiBaseUrl;
    private final String graphApiVersion;
    private final String phoneNumberId;
    private final String accessToken;

    public WhatsAppCloudApiClient(RestClient.Builder restClientBuilder,
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl,
            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}") String graphApiVersion,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken) {
        this.restClient = restClientBuilder.build();
        this.graphApiBaseUrl = stripTrailingSlash(graphApiBaseUrl);
        this.graphApiVersion = graphApiVersion;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    public WhatsAppCloudSendResult sendText(String recipientPhone, String messageBody) {
        WhatsAppCloudSendResult configurationError = validateBasicConfiguration(recipientPhone);
        if (configurationError != null) return configurationError;
        if (messageBody == null || messageBody.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: mensagem vazia.", null);
        String safeBody = sanitizeInstitutionalLanguage(messageBody);
        Map<String, Object> payload = Map.of("messaging_product", "whatsapp", "to", normalizePhone(recipientPhone), "type", "text", "text", Map.of("preview_url", true, "body", safeBody));
        return sendPayload(payload);
    }

    public WhatsAppCloudSendResult sendInteractiveList(String recipientPhone, WhatsappInteractiveListMessage message) {
        WhatsAppCloudSendResult configurationError = validateBasicConfiguration(recipientPhone);
        if (configurationError != null) return configurationError;
        if (message == null) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: menu interativo vazio.", null);
        if (message.body() == null || message.body().isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: corpo do menu interativo vazio.", null);
        if (message.sections() == null || message.sections().isEmpty()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: menu interativo sem opções.", null);

        List<Map<String, Object>> sectionsPayload = new ArrayList<>();
        int totalRows = 0;

        for (WhatsappInteractiveListSection section : message.sections()) {
            if (section == null || section.rows() == null || section.rows().isEmpty()) continue;

            List<Map<String, Object>> rowsPayload = new ArrayList<>();
            for (WhatsappInteractiveListRow row : section.rows()) {
                if (row == null || isBlank(row.id()) || isBlank(row.title())) continue;
                totalRows++;
                if (totalRows > MAX_INTERACTIVE_ROWS) {
                    return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: menu interativo excede o limite de 10 opções.", null);
                }

                Map<String, Object> rowPayload = new LinkedHashMap<>();
                rowPayload.put("id", limit(row.id(), MAX_ROW_ID_LENGTH));
                rowPayload.put("title", limit(row.title(), MAX_ROW_TITLE_LENGTH));
                if (!isBlank(row.description())) {
                    rowPayload.put("description", limit(row.description(), MAX_ROW_DESCRIPTION_LENGTH));
                }
                rowsPayload.add(rowPayload);
            }

            if (rowsPayload.isEmpty()) continue;
            Map<String, Object> sectionPayload = new LinkedHashMap<>();
            if (!isBlank(section.title())) {
                sectionPayload.put("title", limit(section.title(), MAX_SECTION_TITLE_LENGTH));
            }
            sectionPayload.put("rows", rowsPayload);
            sectionsPayload.add(sectionPayload);
        }

        if (sectionsPayload.isEmpty() || totalRows == 0) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: menu interativo sem opções válidas.", null);
        }

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "list");
        if (!isBlank(message.header())) {
            interactive.put("header", Map.of("type", "text", "text", limit(message.header(), MAX_HEADER_LENGTH)));
        }
        interactive.put("body", Map.of("text", limit(sanitizeInstitutionalLanguage(message.body()), MAX_BODY_LENGTH)));
        if (!isBlank(message.footer())) {
            interactive.put("footer", Map.of("text", limit(message.footer(), MAX_FOOTER_LENGTH)));
        }
        interactive.put("action", Map.of(
                "button", limit(firstNonBlank(message.buttonLabel(), "Ver opções"), MAX_BUTTON_LENGTH),
                "sections", sectionsPayload
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", normalizePhone(recipientPhone));
        payload.put("type", "interactive");
        payload.put("interactive", interactive);
        return sendPayload(payload);
    }

    public WhatsAppCloudSendResult sendDocumentByLink(String recipientPhone, String documentUrl, String fileName, String caption) {
        WhatsAppCloudSendResult configurationError = validateBasicConfiguration(recipientPhone);
        if (configurationError != null) return configurationError;
        if (documentUrl == null || documentUrl.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: URL do documento vazia.", null);
        String safeFileName = fileName == null || fileName.isBlank() ? "secretariapay-documento.pdf" : fileName.trim();
        String safeCaption = caption == null ? "" : sanitizeInstitutionalLanguage(caption.trim());
        Map<String, Object> documentPayload = safeCaption.isBlank()
                ? Map.of("link", documentUrl.trim(), "filename", safeFileName)
                : Map.of("link", documentUrl.trim(), "filename", safeFileName, "caption", limitCaption(safeCaption));
        Map<String, Object> payload = Map.of("messaging_product", "whatsapp", "to", normalizePhone(recipientPhone), "type", "document", "document", documentPayload);
        return sendPayload(payload);
    }

    public ResponseEntity<byte[]> downloadMediaByReference(String mediaReference) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("WhatsApp Cloud API: access-token não configurado.");
        }

        String mediaId = extractMediaId(mediaReference);
        if (mediaId.isBlank()) {
            throw new IllegalArgumentException("Media ID do WhatsApp inválido.");
        }

        String metadataUrl = "%s/%s/%s".formatted(graphApiBaseUrl, graphApiVersion, mediaId);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = restClient.get()
                .uri(metadataUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        Object providerUrl = metadata == null ? null : metadata.get("url");
        if (providerUrl == null || providerUrl.toString().isBlank()) {
            throw new IllegalArgumentException("WhatsApp Cloud API não retornou URL da mídia.");
        }

        return restClient.get()
                .uri(providerUrl.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toEntity(byte[].class);
    }

    private WhatsAppCloudSendResult validateBasicConfiguration(String recipientPhone) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: phone-number-id não configurado.", null);
        if (accessToken == null || accessToken.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: access-token não configurado.", null);
        String to = normalizePhone(recipientPhone);
        if (to.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: número do destinatário inválido.", null);
        return null;
    }

    private WhatsAppCloudSendResult sendPayload(Map<String, Object> payload) {
        String url = "%s/%s/%s/messages".formatted(graphApiBaseUrl, graphApiVersion, phoneNumberId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
            String providerMessageId = extractProviderMessageId(response);
            if (providerMessageId == null || providerMessageId.isBlank()) return WhatsAppCloudSendResult.failed("WhatsApp Cloud API respondeu sem ID da mensagem.", 200);
            return WhatsAppCloudSendResult.sent(providerMessageId, 200);
        } catch (RestClientResponseException exception) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API erro HTTP %d: %s".formatted(exception.getStatusCode().value(), safeBody(exception.getResponseBodyAsString())), exception.getStatusCode().value());
        } catch (Exception exception) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API falhou: " + exception.getMessage(), null);
        }
    }

    private String extractProviderMessageId(Map<String, Object> response) {
        if (response == null) return null;
        Object messagesObject = response.get("messages");
        if (!(messagesObject instanceof List<?> messages) || messages.isEmpty()) return null;
        Object first = messages.get(0);
        if (!(first instanceof Map<?, ?> messageMap)) return null;
        Object id = messageMap.get("id");
        return id != null ? id.toString() : null;
    }

    private String extractMediaId(String mediaReference) {
        if (mediaReference == null || mediaReference.isBlank()) return "";
        String value = mediaReference.trim();
        if (value.startsWith("whatsapp-cloud-media://")) {
            value = value.substring("whatsapp-cloud-media://".length());
        }
        if (value.startsWith("unknown/")) return "";
        int slashIndex = value.indexOf('/');
        return slashIndex >= 0 ? value.substring(0, slashIndex) : value;
    }

    private String sanitizeInstitutionalLanguage(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;

        String sanitized = value
                .replaceAll("(?iu)PDF da guia oficial", "PDF da guia de pagamento")
                .replaceAll("(?iu)guia oficial", "guia de pagamento")
                .replaceAll("(?iu)comprovativos oficiais", "comprovativos de pagamento")
                .replaceAll("(?iu)comprovativo oficial", "comprovativo de pagamento")
                .replaceAll("(?iu)recibo oficial", "recibo de pagamento")
                .replaceAll("(?iu)documento oficial", "documento financeiro");

        String normalized = sanitized.toLowerCase(Locale.ROOT);
        if (normalized.contains("comprovativo de pagamento emitido")) {
            sanitized = sanitized
                    .replaceAll("(?iu)SecretáriaPay Académico: comprovativo de pagamento emitido\\.", "SecretáriaPay Académico: borderô financeiro consolidado emitido.")
                    .replaceAll("(?m)^Comprovativo:\\s*", "Código do último registo: ")
                    .replaceAll("(?m)^Referência:\\s*", "Última referência atualizada: ")
                    .replaceAll("(?m)^Valor:\\s*", "Valor do último pagamento: ")
                    .replaceAll("(?m)^Link público:\\s*", "Consultar borderô: ");
        }

        return sanitized;
    }

    private String normalizePhone(String phone) { if (phone == null) return ""; String digits = phone.replaceAll("[^0-9]", ""); return digits.startsWith("00") ? digits.substring(2) : digits; }
    private String stripTrailingSlash(String value) { if (value == null || value.isBlank()) return "https://graph.facebook.com"; return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
    private String limitCaption(String caption) { if (caption == null) return ""; return caption.length() <= 1024 ? caption : caption.substring(0, 1021) + "..."; }
    private String safeBody(String body) { if (body == null || body.isBlank()) return "sem detalhes"; return body.length() > 700 ? body.substring(0, 700) + "..." : body; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String firstNonBlank(String value, String fallback) { return isBlank(value) ? fallback : value.trim(); }
    private String limit(String value, int maxLength) { if (value == null) return ""; String trimmed = value.trim(); return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength); }
}
