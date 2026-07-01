package com.secretariapay.api.controller;

import com.secretariapay.api.service.TicketPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/tickets")
public class PublicTicketPdfController {

    private final TicketPdfService ticketPdfService;

    public PublicTicketPdfController(TicketPdfService ticketPdfService) {
        this.ticketPdfService = ticketPdfService;
    }

    @GetMapping("/{ticketId}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @PathVariable UUID ticketId
    ) {
        byte[] pdf = ticketPdfService.generateTicketPdf(ticketId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"bilhete-vairapido.pdf\""
                )
                .body(pdf);
    }
}
