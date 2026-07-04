package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
public class TestOpenRecipientWhatsAppCloudApiClient extends WhatsAppCloudApiClient {

    private final boolean testOpenRecipientEnabled;

    public TestOpenRecipientWhatsAppCloudApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl,
            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}") String graphApiVersion,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken,
            @Value("${secretariapay.whatsapp.test-open-recipient-enabled:true}") boolean testOpenRecipientEnabled
    ) {
        super(restClientBuilder, graphApiBaseUrl, graphApiVersion, phoneNumberId, accessToken);
        this.testOpenRecipientEnabled = testOpenRecipientEnabled;
    }

    @Override
    public WhatsAppCloudSendResult sendText(String recipientPhone, String messageBody) {
        return super.sendText(resolveRecipient(recipientPhone), messageBody);
    }

    @Override
    public WhatsAppCloudSendResult sendDocumentByLink(
            String recipientPhone,
            String documentUrl,
            String fileName,
            String caption
    ) {
        return super.sendDocumentByLink(
                resolveRecipient(recipientPhone),
                documentUrl,
                fileName,
                caption
        );
    }

    private String resolveRecipient(String originalRecipient) {
        if (!testOpenRecipientEnabled) {
            return originalRecipient;
        }

        return WhatsappRecipientOverrideContext.current()
                .orElse(originalRecipient);
    }
}
