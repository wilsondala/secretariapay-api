package com.vairapido.api.controller;

import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.service.PublicTicketValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/tickets")
public class PublicTicketController {

    private final PublicTicketValidationService publicTicketValidationService;

    public PublicTicketController(
            PublicTicketValidationService publicTicketValidationService
    ) {
        this.publicTicketValidationService = publicTicketValidationService;
    }

    @GetMapping("/validate/{ticketCode}")
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @PathVariable String ticketCode,
            HttpServletRequest request
    ) {
        TicketValidationResponse response =
                publicTicketValidationService.validateByCode(
                        ticketCode,
                        getClientIp(request),
                        getUserAgent(request)
                );

        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}