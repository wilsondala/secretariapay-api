package com.vairapido.api.controller;

import com.vairapido.api.dto.whatsappsession.WhatsappSessionResponse;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionStartRequest;
import com.vairapido.api.service.WhatsappSessionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp/sessions")
public class WhatsappSessionController {

    private final WhatsappSessionService service;

    public WhatsappSessionController(WhatsappSessionService service) {
        this.service = service;
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public WhatsappSessionResponse startSession(
            @Valid @RequestBody WhatsappSessionStartRequest request
    ) {
        return service.startSession(request);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public WhatsappSessionResponse findActiveSession(
            @RequestParam String phoneNumber
    ) {
        return service.findActiveSession(phoneNumber);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<WhatsappSessionResponse> findByPhoneNumber(
            @RequestParam String phoneNumber
    ) {
        return service.findByPhoneNumber(phoneNumber);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public WhatsappSessionResponse closeSession(@PathVariable UUID id) {
        return service.closeSession(id);
    }

    @PostMapping("/expire-overdue")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public int expireOverdueSessions() {
        return service.expireOverdueSessions();
    }
}