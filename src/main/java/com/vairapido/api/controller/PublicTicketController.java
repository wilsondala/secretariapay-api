package com.vairapido.api.controller;

import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.service.PublicTicketValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @PathVariable String ticketCode
    ) {
        TicketValidationResponse response =
                publicTicketValidationService.validateByCode(ticketCode);

        return ResponseEntity.ok(response);
    }
}