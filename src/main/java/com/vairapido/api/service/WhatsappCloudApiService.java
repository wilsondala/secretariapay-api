package com.vairapido.api.service;

import com.vairapido.api.config.WhatsappCloudApiProperties;
import com.vairapido.api.dto.whatsappcloud.WhatsappCloudDocumentMessageRequest;
import com.vairapido.api.dto.whatsappcloud.WhatsappCloudMessageResponse;
import com.vairapido.api.dto.whatsappcloud.WhatsappCloudTextMessageRequest;
import com.vairapido.api.dto.whatsappcloud.WhatsappOutboundMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;

@Service
public class WhatsappCloudApiService {

    private static final Logger logger =
            LoggerFactory.getLogger(WhatsappCloudApiService.class);

    private final WhatsappCloudApiProperties properties;
    private final RestClient restClient;

    public WhatsappCloudApiService(
            WhatsappCloudApiProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(removeTrailingSlash(properties.getGraphApiBaseUrl()))
                .build();
    }

    public WhatsappOutboundMessageResult sendTextMessage(
            String phoneNumber,
            String messageText
    ) {
        String normalizedPhone = normalizePhoneForCloudApi(phoneNumber);

        WhatsappOutboundMessageResult result = baseResult(normalizedPhone);

        String validationError = validateBaseSend(
                normalizedPhone,
                "text",
                messageText
        );

        if (validationError != null) {
            logValidationFailure("texto", normalizedPhone, messageText, validationError);
            return result.setErrorMessage(validationError);
        }

        WhatsappCloudTextMessageRequest request =
                WhatsappCloudTextMessageRequest.textMessage(
                        normalizedPhone,
                        messageText
                );

        return sendPayload(
                normalizedPhone,
                request,
                result,
                "mensagem de texto"
        );
    }

    public WhatsappOutboundMessageResult sendDocumentMessage(
            String phoneNumber,
            String documentUrl,
            String fileName,
            String caption
    ) {
        String normalizedPhone = normalizePhoneForCloudApi(phoneNumber);

        WhatsappOutboundMessageResult result = baseResult(normalizedPhone);

        String validationError = validateBaseSend(
                normalizedPhone,
                "document",
                documentUrl
        );

        if (validationError != null) {
            logValidationFailure("documento", normalizedPhone, documentUrl, validationError);
            return result.setErrorMessage(validationError);
        }

        String normalizedFileName = normalizeFileName(fileName);
        String normalizedCaption = caption == null ? "" : caption.trim();

        WhatsappCloudDocumentMessageRequest request =
                WhatsappCloudDocumentMessageRequest.documentMessage(
                        normalizedPhone,
                        documentUrl,
                        normalizedFileName,
                        normalizedCaption
                );

        return sendPayload(
                normalizedPhone,
                request,
                result,
                "documento PDF"
        );
    }

    private WhatsappOutboundMessageResult sendPayload(
            String normalizedPhone,
            Object request,
            WhatsappOutboundMessageResult result,
            String messageType
    ) {
        try {
            WhatsappCloudMessageResponse response = restClient
                    .post()
                    .uri(
                            "/{version}/{phoneNumberId}/messages",
                            properties.getGraphApiVersion(),
                            properties.getPhoneNumberId()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getAccessToken()
                    )
                    .body(request)
                    .retrieve()
                    .body(WhatsappCloudMessageResponse.class);

            String providerMessageId = extractProviderMessageId(response);

            logger.info(
                    "WhatsApp {} enviado com sucesso. Destino: {}. ProviderMessageId: {}",
                    messageType,
                    normalizedPhone,
                    providerMessageId
            );

            return result
                    .setAttempted(true)
                    .setSent(true)
                    .setProviderMessageId(providerMessageId);

        } catch (RestClientResponseException exception) {
            String responseBody = safeResponseBody(exception.getResponseBodyAsString());

            logger.warn(
                    "Falha HTTP ao enviar WhatsApp {}. Status: {}. Body: {}",
                    messageType,
                    exception.getStatusCode().value(),
                    responseBody
            );

            return result
                    .setAttempted(true)
                    .setSent(false)
                    .setErrorMessage(
                            "Falha HTTP ao enviar WhatsApp. Status "
                                    + exception.getStatusCode().value()
                                    + ": "
                                    + responseBody
                    );

        } catch (Exception exception) {
            logger.error(
                    "Falha inesperada ao enviar WhatsApp {}.",
                    messageType,
                    exception
            );

            return result
                    .setAttempted(true)
                    .setSent(false)
                    .setErrorMessage(
                            "Falha inesperada ao enviar WhatsApp: "
                                    + exception.getMessage()
                    );
        }
    }

    private WhatsappOutboundMessageResult baseResult(String normalizedPhone) {
        return new WhatsappOutboundMessageResult()
                .setEnabled(properties.isEnabled())
                .setAttempted(false)
                .setSent(false)
                .setPhoneNumber(normalizedPhone)
                .setSentAt(LocalDateTime.now());
    }

    private String validateBaseSend(
            String normalizedPhone,
            String payloadType,
            String requiredValue
    ) {
        if (!properties.isEnabled()) {
            return "Envio real WhatsApp desativado por configuração.";
        }

        if (!properties.isConfigured()) {
            return "Configuração da WhatsApp Cloud API incompleta.";
        }

        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return "Número de telefone inválido para envio WhatsApp.";
        }

        if (requiredValue == null || requiredValue.isBlank()) {
            if ("document".equals(payloadType)) {
                return "URL do documento vazia. Nada foi enviado para o WhatsApp.";
            }

            return "Mensagem vazia. Nada foi enviado para o WhatsApp.";
        }

        return null;
    }

    private void logValidationFailure(
            String messageType,
            String normalizedPhone,
            String payload,
            String validationError
    ) {
        if (!properties.isEnabled()) {
            logger.info(
                    "Envio real WhatsApp desativado. Tipo: {}. Destino: {}. Payload simulado: {}",
                    messageType,
                    normalizedPhone,
                    payload
            );
            return;
        }

        logger.warn(
                "WhatsApp {} não enviado. Destino: {}. Motivo: {}",
                messageType,
                normalizedPhone,
                validationError
        );
    }

    private String extractProviderMessageId(
            WhatsappCloudMessageResponse response
    ) {
        if (response == null
                || response.getMessages() == null
                || response.getMessages().isEmpty()
                || response.getMessages().get(0) == null) {
            return null;
        }

        return response.getMessages().get(0).getId();
    }

    private String normalizePhoneForCloudApi(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "";
        }

        return phoneNumber.replaceAll("\\D", "");
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "bilhete-vairapido.pdf";
        }

        String normalized = fileName
                .trim()
                .replace("\\", "-")
                .replace("/", "-")
                .replace(":", "-")
                .replace("*", "-")
                .replace("?", "-")
                .replace("\"", "")
                .replace("<", "-")
                .replace(">", "-")
                .replace("|", "-");

        if (!normalized.toLowerCase().endsWith(".pdf")) {
            normalized = normalized + ".pdf";
        }

        return normalized;
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://graph.facebook.com";
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String safeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "[sem corpo de resposta]";
        }

        if (responseBody.length() <= 500) {
            return responseBody;
        }

        return responseBody.substring(0, 500) + "...";
    }
}