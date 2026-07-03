package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.service.whatsapp.SecretariaPayPaymentGuideMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/payment-guides")
public class SecretariaPayPaymentGuideController {

    private final SecretariaPayPaymentGuideMessageService service;

    public SecretariaPayPaymentGuideController(SecretariaPayPaymentGuideMessageService service) {
        this.service = service;
    }

    @PostMapping("/charges/{chargeId}/message")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generatePaymentGuideMessage(@PathVariable UUID chargeId) {
        return service.generatePaymentGuideMessage(chargeId);
    }
}
