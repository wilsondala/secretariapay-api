package com.secretariapay.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.config.WhatsAppCloudProperties;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudStatusResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppCloudClient {

    public static final String PROVIDER_NAME = "WHATSAPP_CLOUD_API";

    private final WhatsAppCloudProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WhatsAppCloudClient(
            WhatsAppCloudProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public WhatsAppCloudStatusResponse getStatus() {
        boolean enabled = properties.isEnabled();
        boolean accessTokenConfigured = properties.hasAccessToken();
        boolean phoneNumberIdConfigured = properties.hasPhoneNumberId();
        boolean configured = properties.isConfigured();

        String message;

        if (configured) {
            message = "WhatsApp Cloud API configurado para envio real.";
        } else if (!enabled) {
            message = "Envio real desativado. O sistema continua usando WhatsApp simulado.";
        } else if (!accessTokenConfigured && !phoneNumberIdConfigured) {
            message = "Envio real ativado, mas faltam access token e phone number ID.";
        } else if (!accessTokenConfigured) {
            message = "Envio real ativado, mas falta access token.";
        } else {
            message = "Envio real ativado, mas falta phone number ID.";
        }

        return new WhatsAppCloudStatusResponse()
                .setEnabled(enabled)
                .setConfigured(configured)
                .setAccessTokenConfigured(accessTokenConfigured)
                .setPhoneNumberIdConfigured(phoneNumberIdConfigured)
                .setProviderName(PROVIDER_NAME)
                .setGraphApiVersion(properties.getNormalizedGraphApiVersion())
                .setBaseUrl(properties.getNormalizedBaseUrl())
                .setMessage(message);
    }

    public boolean isReady() {
        return properties.isConfigured();
    }

    public WhatsAppCloudSendResult sendTextMessage(String toPhone, String messageBody) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("WhatsApp Cloud API não está configurado.");
        }

        if (toPhone == null || toPhone.isBlank()) {
            throw new IllegalArgumentException("Telefone de destino não informado.");
        }

        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("Corpo da mensagem não informado.");
        }

        String url = "%s/%s/%s/messages".formatted(
                properties.getNormalizedBaseUrl(),
                properties.getNormalizedGraphApiVersion(),
                properties.getPhoneNumberId()
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", normalizePhone(toPhone));
        payload.put("type", "text");

        Map<String, Object> text = new HashMap<>();
        text.put("preview_url", false);
        text.put("body", messageBody);

        payload.put("text", text);

        try {
            Map<String, Object> response = restClient
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            String providerMessageId = extractProviderMessageId(response);

            return new WhatsAppCloudSendResult()
                    .setSuccess(true)
                    .setProviderMessageId(providerMessageId)
                    .setRawResponse(writeJson(response));

        } catch (RestClientResponseException exception) {
            return new WhatsAppCloudSendResult()
                    .setSuccess(false)
                    .setErrorMessage(extractErrorMessage(exception))
                    .setRawResponse(exception.getResponseBodyAsString());

        } catch (RuntimeException exception) {
            return new WhatsAppCloudSendResult()
                    .setSuccess(false)
                    .setErrorMessage(exception.getMessage());
        }
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    @SuppressWarnings("unchecked")
    private String extractProviderMessageId(Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object messagesObject = response.get("messages");

        if (messagesObject instanceof List<?> messages && !messages.isEmpty()) {
            Object firstMessage = messages.get(0);

            if (firstMessage instanceof Map<?, ?> firstMessageMap) {
                Object id = firstMessageMap.get("id");
                return id != null ? String.valueOf(id) : null;
            }
        }

        return null;
    }

    private String extractErrorMessage(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();

        if (body == null || body.isBlank()) {
            return exception.getMessage();
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    body,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    }
            );

            Object errorObject = parsed.get("error");

            if (errorObject instanceof Map<?, ?> errorMap) {
                Object message = errorMap.get("message");

                if (message != null) {
                    return String.valueOf(message);
                }
            }

            return body;
        } catch (JsonProcessingException ignored) {
            return body;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}

