package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayDispatchBatchResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageDispatchService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/message-dispatch")
public class SecretariaPayMessageDispatchController {

    private final SecretariaPayMessageDispatchService service;

    public SecretariaPayMessageDispatchController(SecretariaPayMessageDispatchService service) {
        this.service = service;
    }

    @PatchMapping("/{messageId}/queue")
    public SecretariaPayMessageDispatchResult queue(@PathVariable UUID messageId) {
        return service.queue(messageId);
    }

    @PostMapping("/{messageId}/dispatch")
    public SecretariaPayMessageDispatchResult dispatch(@PathVariable UUID messageId) {
        return service.dispatch(messageId);
    }

    @PostMapping("/process-queue")
    public SecretariaPayDispatchBatchResponse processQueue(@RequestParam(required = false) Integer limit) {
        return service.processQueue(limit);
    }
}
