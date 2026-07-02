package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.service.whatsapp.SecretariaPayWhatsappWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secretariapay/whatsapp/webhook")
public class SecretariaPayWhatsappWebhookController {

    private final SecretariaPayWhatsappWebhookService webhookService;

    public SecretariaPayWhatsappWebhookController(
            SecretariaPayWhatsappWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        String response = webhookService.verifyWebhook(mode, verifyToken, challenge);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhookPayload(
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Map<String, Object> response = webhookService.receiveWebhookPayload(payload);
        return ResponseEntity.ok(response);
    }
}