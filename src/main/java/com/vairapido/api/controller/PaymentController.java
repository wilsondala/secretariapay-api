package com.vairapido.api.controller;

import com.vairapido.api.dto.payment.PaymentRequest;
import com.vairapido.api.dto.payment.PaymentResponse;
import com.vairapido.api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<PaymentResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/code/{paymentCode}")
    public PaymentResponse findByCode(@PathVariable String paymentCode) {
        return service.findByCode(paymentCode);
    }

    @PatchMapping("/{id}/confirm")
    public PaymentResponse confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }

    @PatchMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PatchMapping("/{id}/expire")
    public PaymentResponse expire(@PathVariable UUID id) {
        return service.expire(id);
    }
}