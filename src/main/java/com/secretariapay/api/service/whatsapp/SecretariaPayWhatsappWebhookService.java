package com.secretariapay.api.service.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SecretariaPayWhatsappWebhookService {

    private final String verifyToken;

    public SecretariaPayWhatsappWebhookService(
            @Value("${secretariapay.whatsapp.verify-token:secretariapay-dev-token}")
            String verifyToken
    ) {
        this.verifyToken = verifyToken;
    }

    public String verifyWebhook(
            String mode,
            String token,
            String challenge
    ) {
        boolean isSubscribeMode = "subscribe".equals(mode);
        boolean isValidToken = verifyToken != null
                && !verifyToken.isBlank()
                && verifyToken.equals(token);

        if (!isSubscribeMode || !isValidToken || challenge == null || challenge.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Webhook SecretáriaPay WhatsApp não autorizado."
            );
        }

        return challenge;
    }

    public Map<String, Object> receiveWebhookPayload(Map<String, Object> payload) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("received", true);
        response.put("processed", false);
        response.put("module", "SECRETARIAPAY_ACADEMICO_WHATSAPP_WEBHOOK");
        response.put("status", "RECEIVED");
        response.put("reason", "Payload recebido com sucesso. Processamento de mensagens/status será evoluído na próxima fase.");
        response.put("payloadPresent", payload != null && !payload.isEmpty());
        response.put("receivedAt", LocalDateTime.now().toString());

        return response;
    }
}