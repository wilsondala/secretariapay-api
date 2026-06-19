package com.vairapido.api.controller;

import com.vairapido.api.dto.boarding.TicketBoardingResponse;
import com.vairapido.api.dto.ticket.TicketRequest;
import com.vairapido.api.dto.ticket.TicketResponse;
import com.vairapido.api.service.TicketBoardingService;
import com.vairapido.api.service.TicketPdfService;
import com.vairapido.api.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService service;
    private final TicketPdfService ticketPdfService;
    private final TicketBoardingService ticketBoardingService;

    public TicketController(
            TicketService service,
            TicketPdfService ticketPdfService,
            TicketBoardingService ticketBoardingService
    ) {
        this.service = service;
        this.ticketPdfService = ticketPdfService;
        this.ticketBoardingService = ticketBoardingService;
    }

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse issue(@Valid @RequestBody TicketRequest request) {
        return service.issue(request);
    }

    @GetMapping
    public List<TicketResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdf = ticketPdfService.generateTicketPdf(id);

        String fileName = "bilhete-vairapido-" + id + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString()
                )
                .body(pdf);
    }

    @GetMapping("/code/{ticketCode}")
    public TicketResponse findByCode(@PathVariable String ticketCode) {
        return service.findByCode(ticketCode);
    }

    /**
     * Embarque operacional.
     * Rota protegida por JWT.
     * Marca o bilhete como USED e grava usedAt.
     */
    @PatchMapping("/{ticketCode}/board")
    public TicketBoardingResponse boardByTicketCode(@PathVariable String ticketCode) {
        return ticketBoardingService.boardByTicketCode(ticketCode);
    }

    @PatchMapping("/{id}/use")
    public TicketResponse useTicket(@PathVariable UUID id) {
        return service.useTicket(id);
    }

    @PatchMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}