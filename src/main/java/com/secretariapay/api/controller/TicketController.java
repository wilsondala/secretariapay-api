package com.secretariapay.api.controller;

import com.secretariapay.api.dto.boarding.TicketBoardingResponse;
import com.secretariapay.api.dto.ticket.TicketRequest;
import com.secretariapay.api.dto.ticket.TicketResponse;
import com.secretariapay.api.service.TicketBoardingService;
import com.secretariapay.api.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketBoardingService ticketBoardingService;

    public TicketController(
            TicketService ticketService,
            TicketBoardingService ticketBoardingService
    ) {
        this.ticketService = ticketService;
        this.ticketBoardingService = ticketBoardingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public TicketResponse issue(@Valid @RequestBody TicketRequest request) {
        return ticketService.issue(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<TicketResponse> findAll() {
        return ticketService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canAccessTicket(#p0)")
    public TicketResponse findById(@PathVariable UUID id) {
        return ticketService.findById(id);
    }

    @GetMapping("/code/{ticketCode}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canAccessTicketCode(#p0)")
    public TicketResponse findByCode(@PathVariable String ticketCode) {
        return ticketService.findByCode(ticketCode);
    }

    @GetMapping("/{ticketCode}/boarding-preview")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canBoardTicketCode(#p0)")
    public TicketBoardingResponse previewBoardingByTicketCode(@PathVariable String ticketCode) {
        return ticketBoardingService.previewByTicketCode(ticketCode);
    }

    @PatchMapping("/{id}/use")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'OPERATOR', 'ROLE_OPERATOR')")
    public TicketResponse useTicket(@PathVariable UUID id) {
        return ticketService.useTicket(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public TicketResponse cancel(@PathVariable UUID id) {
        return ticketService.cancel(id);
    }

    @PatchMapping("/{ticketCode}/board")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canBoardTicketCode(#p0)")
    public TicketBoardingResponse boardByTicketCode(
            @PathVariable String ticketCode,
            HttpServletRequest request
    ) {
        return ticketBoardingService.boardByTicketCode(
                ticketCode,
                getClientIp(request),
                getUserAgent(request)
        );
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
