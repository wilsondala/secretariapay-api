package com.secretariapay.api.controller;

import com.secretariapay.api.dto.booking.BookingExpirationResponse;
import com.secretariapay.api.dto.booking.BookingRequest;
import com.secretariapay.api.dto.booking.BookingResponse;
import com.secretariapay.api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canAccessTrip(#p0.tripId)")
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<BookingResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public List<BookingResponse> findByCompanyId(@PathVariable UUID companyId) {
        return service.findByCompanyId(companyId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@companyAccessService.canAccessBooking(#p0)")
    public BookingResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/code/{bookingCode}")
    @PreAuthorize("@companyAccessService.canAccessBookingCode(#p0)")
    public BookingResponse findByCode(@PathVariable String bookingCode) {
        return service.findByCode(bookingCode);
    }

    @PatchMapping("/{id}/confirm-payment")
    @PreAuthorize("@companyAccessService.canAccessBooking(#p0)")
    public BookingResponse confirmPayment(@PathVariable UUID id) {
        return service.confirmPayment(id);
    }

    @PatchMapping("/{id}/issue-ticket")
    @PreAuthorize("@companyAccessService.canAccessBooking(#p0)")
    public BookingResponse issueTicket(@PathVariable UUID id) {
        return service.issueTicket(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("@companyAccessService.canAccessBooking(#p0)")
    public BookingResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PostMapping("/expire-overdue")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public BookingExpirationResponse expireOverdueBookings() {
        return service.expireOverdueBookings();
    }
}
