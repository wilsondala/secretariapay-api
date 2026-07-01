package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppDiagnosticsResponse;
import com.secretariapay.api.service.whatsapp.WhatsAppDiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secretariapay/whatsapp")
public class WhatsAppDiagnosticsController {

    private final WhatsAppDiagnosticsService service;

    public WhatsAppDiagnosticsController(WhatsAppDiagnosticsService service) {
        this.service = service;
    }

    @GetMapping("/diagnostics")
    public WhatsAppDiagnosticsResponse diagnostics() {
        return service.getDiagnostics();
    }
}
