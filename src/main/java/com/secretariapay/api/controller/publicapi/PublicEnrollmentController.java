package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.service.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/enrollments")
public class PublicEnrollmentController {

    private final EnrollmentService service;

    public PublicEnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @PostMapping("/invoices/{invoiceId}/payment-proofs")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentDto.PaymentProofResponse submitPaymentProof(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody EnrollmentDto.PaymentProofRequest request
    ) {
        return service.submitPaymentProof(invoiceId, request);
    }
}
