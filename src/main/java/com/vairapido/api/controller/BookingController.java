package com.vairapido.api.controller;

import com.vairapido.api.dto.booking.BookingExpirationResponse;
import com.vairapido.api.dto.booking.BookingRequest;
import com.vairapido.api.dto.booking.BookingResponse;
import com.vairapido.api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<BookingResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BookingResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/code/{bookingCode}")
    public BookingResponse findByCode(@PathVariable String bookingCode) {
        return service.findByCode(bookingCode);
    }

    @PatchMapping("/{id}/confirm-payment")
    public BookingResponse confirmPayment(@PathVariable UUID id) {
        return service.confirmPayment(id);
    }

    @PatchMapping("/{id}/issue-ticket")
    public BookingResponse issueTicket(@PathVariable UUID id) {
        return service.issueTicket(id);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PostMapping("/expire-overdue")
    public BookingExpirationResponse expireOverdueBookings() {
        return service.expireOverdueBookings();
    }
}