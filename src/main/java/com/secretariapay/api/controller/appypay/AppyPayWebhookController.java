package com.secretariapay.api.controller.appypay;

import com.secretariapay.api.dto.appypay.AppyPayWebhookResponse;
import com.secretariapay.api.service.appypay.AppyPayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/appypay")
public class AppyPayWebhookController {

    private final AppyPayWebhookService service;

    public AppyPayWebhookController(AppyPayWebhookService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    public ResponseEntity<AppyPayWebhookResponse> receiveWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(service.process(payload));
    }
}
