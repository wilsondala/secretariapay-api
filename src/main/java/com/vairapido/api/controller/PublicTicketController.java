package com.vairapido.api.controller;

import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.service.PublicTicketValidationPageService;
import com.vairapido.api.service.PublicTicketValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/tickets")
public class PublicTicketController {

    private final PublicTicketValidationService publicTicketValidationService;
    private final PublicTicketValidationPageService publicTicketValidationPageService;

    public PublicTicketController(
            PublicTicketValidationService publicTicketValidationService,
            PublicTicketValidationPageService publicTicketValidationPageService
    ) {
        this.publicTicketValidationService = publicTicketValidationService;
        this.publicTicketValidationPageService = publicTicketValidationPageService;
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

    @GetMapping(
            value = "/validate/{ticketCode}/page",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<String> validateTicketPage(
            @PathVariable String ticketCode,
            HttpServletRequest request
    ) {
        String html = publicTicketValidationPageService.renderValidationPage(
                ticketCode,
                getClientIp(request),
                getUserAgent(request)
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
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