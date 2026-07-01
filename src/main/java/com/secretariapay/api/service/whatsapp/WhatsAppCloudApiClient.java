package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class WhatsAppCloudApiClient {

    private final RestClient restClient;
    private final String graphApiBaseUrl;
    private final String graphApiVersion;
    private final String phoneNumberId;
    private final String accessToken;

    public WhatsAppCloudApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl,
            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}") String graphApiVersion,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken
    ) {
        this.restClient = restClientBuilder.build();
        this.graphApiBaseUrl = stripTrailingSlash(graphApiBaseUrl);
        this.graphApiVersion = graphApiVersion;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    public WhatsAppCloudSendResult sendText(String recipientPhone, String messageBody) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: phone-number-id não configurado.", null);
        }

        if (accessToken == null || accessToken.isBlank()) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: access-token não configurado.", null);
        }

        String to = normalizePhone(recipientPhone);

        if (to.isBlank()) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: número do destinatário inválido.", null);
        }

        if (messageBody == null || messageBody.isBlank()) {
            return WhatsAppCloudSendResult.failed("WhatsApp Cloud API: mensagem vazia.", null);
        }

        String url = "%s/%s/%s/messages".formatted(graphApiBaseUrl, graphApiVersion, phoneNumberId);

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of(
                        "preview_url", true,
                        "body", messageBody
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String providerMessageId = extractProviderMessageId(response);

            if (providerMessageId == null || providerMessageId.isBlank()) {
                return WhatsAppCloudSendResult.failed("WhatsApp Cloud API respondeu sem ID da mensagem.", 200);
            }

            return WhatsAppCloudSendResult.sent(providerMessageId, 200);
        } catch (RestClientResponseException exception) {
            return WhatsAppCloudSendResult.failed(
                    "WhatsApp Cloud API erro HTTP %d: %s".formatted(
                            exception.getStatusCode().value(),
                            safeBody(exception.getResponseBodyAsString())
                    ),
                    exception.getStatusCode().value()
            );
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

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) return digits.substring(2);
        return digits;
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://graph.facebook.com";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) return "sem detalhes";
        return body.length() > 700 ? body.substring(0, 700) + "..." : body;
    }
}
