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
                "project", "VaiRápido API",
                "status", "online",
                "description", "Backend para compra rápida de passagens pelo WhatsApp",
                "timestamp", LocalDateTime.now()
        );
    }

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "vairapido-api",
                "database", "postgresql",
                "timestamp", LocalDateTime.now()
        );
    }
}
