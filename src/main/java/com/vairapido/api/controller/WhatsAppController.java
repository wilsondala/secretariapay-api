package com.vairapido.api.controller;

import com.vairapido.api.dto.whatsapp.WhatsAppMessageResponse;
import com.vairapido.api.service.WhatsAppService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {

    private final WhatsAppService service;

    public WhatsAppController(WhatsAppService service) {
        this.service = service;
    }

    @PostMapping("/send-payment-instructions/{bookingId}")
    public WhatsAppMessageResponse sendPaymentInstructions(@PathVariable UUID bookingId) {
        return service.sendPaymentInstructions(bookingId);
    }

    @PostMapping("/send-ticket/{ticketId}")
    public WhatsAppMessageResponse sendTicket(@PathVariable UUID ticketId) {
        return service.sendTicket(ticketId);
    }

    @GetMapping("/messages")
    public List<WhatsAppMessageResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/messages/{id}")
    public WhatsAppMessageResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PatchMapping("/messages/{id}/mark-sent")
    public WhatsAppMessageResponse markAsSent(@PathVariable UUID id) {
        return service.markAsSent(id);
    }

    @PatchMapping("/messages/{id}/mark-failed")
    public WhatsAppMessageResponse markAsFailed(@PathVariable UUID id) {
        return service.markAsFailed(id);
    }
}