package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.PaymentProofRequest;
import com.secretariapay.api.dto.financial.PaymentProofResponse;
import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.service.financial.PaymentProofService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-proofs")
public class PaymentProofController {

    private final PaymentProofService service;

    public PaymentProofController(PaymentProofService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentProofResponse create(@Valid @RequestBody PaymentProofRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public List<PaymentProofResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public PaymentProofResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public List<PaymentProofResponse> findByStatus(@PathVariable PaymentProofStatus status) {
        return service.findByStatus(status);
    }

    @GetMapping("/charge/{chargeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<PaymentProofResponse> findByCharge(@PathVariable UUID chargeId) {
        return service.findByCharge(chargeId);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public PaymentProofResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.approve(id, request);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public PaymentProofResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.reject(id, request);
    }
}
