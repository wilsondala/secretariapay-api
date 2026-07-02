package com.secretariapay.api.controller;

import com.secretariapay.api.dto.whatsappwebhook.WhatsappWebhookReceiveResponse;
import com.secretariapay.api.service.WhatsappWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/whatsapp/webhook")
@Deprecated(since = "2026-07-02", forRemoval = false)
public class WhatsappWebhookController {

    private final WhatsappWebhookService service;

    public WhatsappWebhookController(WhatsappWebhookService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> legacyStatus() {
        return ResponseEntity.ok(
                service.legacyStatus()
        );
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        return ResponseEntity.ok(
                service.verifyWebhook(mode, verifyToken, challenge)
        );
    }

    @PostMapping
    public ResponseEntity<WhatsappWebhookReceiveResponse> receiveMessage(
            @RequestBody Map<String, Object> payload
    ) {
        return ResponseEntity.ok(
                service.receiveMessage(payload)
        );
    }
}
