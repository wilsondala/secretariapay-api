package com.secretariapay.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "project", "SecretáriaPay API",
                "product", "SecretáriaPay Académico",
                "status", "online",
                "description", "Backend institucional para automação de propinas, cobranças, comprovativos, recibos digitais e atendimento académico via WhatsApp.",
                "countryFocus", "Angola",
                "timestamp", LocalDateTime.now()
        );
    }

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "secretariapay-api",
                "database", "postgresql",
                "timestamp", LocalDateTime.now()
        );
    }
}
