package com.vairapido.api.service;

import com.vairapido.api.config.WhatsappCloudApiProperties;
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

        WhatsappOutboundMessageResult result = new WhatsappOutboundMessageResult()
                .setEnabled(properties.isEnabled())
                .setAttempted(false)
                .setSent(false)
                .setPhoneNumber(normalizedPhone)
                .setSentAt(LocalDateTime.now());

        if (!properties.isEnabled()) {
            logger.info(
                    "Envio real WhatsApp desativado. Destino: {}. Mensagem simulada: {}",
                    normalizedPhone,
                    messageText
            );

            return result.setErrorMessage(
                    "Envio real WhatsApp desativado por configuração."
            );
        }

        if (!properties.isConfigured()) {
            logger.warn(
                    "Envio WhatsApp ativado, mas configuração incompleta. " +
                            "Verifique VAIRAPIDO_WHATSAPP_PHONE_NUMBER_ID, " +
                            "VAIRAPIDO_WHATSAPP_ACCESS_TOKEN, " +
                            "VAIRAPIDO_WHATSAPP_GRAPH_API_VERSION e base URL."
            );

            return result.setErrorMessage(
                    "Configuração da WhatsApp Cloud API incompleta."
            );
        }

        if (normalizedPhone.isBlank()) {
            return result.setErrorMessage(
                    "Número de telefone inválido para envio WhatsApp."
            );
        }

        if (messageText == null || messageText.isBlank()) {
            return result.setErrorMessage(
                    "Mensagem vazia. Nada foi enviado para o WhatsApp."
            );
        }

        try {
            WhatsappCloudTextMessageRequest request =
                    WhatsappCloudTextMessageRequest.textMessage(
                            normalizedPhone,
                            messageText
                    );

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
                    "Mensagem WhatsApp enviada com sucesso. Destino: {}. ProviderMessageId: {}",
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
                    "Falha HTTP ao enviar mensagem WhatsApp. Status: {}. Body: {}",
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
                    "Falha inesperada ao enviar mensagem WhatsApp.",
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