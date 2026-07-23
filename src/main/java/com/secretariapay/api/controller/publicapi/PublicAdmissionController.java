package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.service.admission.AdmissionPublicPaymentService;
import com.secretariapay.api.service.admission.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/admissions")
public class PublicAdmissionController {

    private final AdmissionService service;
    private final AdmissionPublicPaymentService publicPaymentService;

    public PublicAdmissionController(
            AdmissionService service,
            AdmissionPublicPaymentService publicPaymentService
    ) {
        this.service = service;
        this.publicPaymentService = publicPaymentService;
    }

    @GetMapping("/catalog")
    public AdmissionDto.CatalogResponse catalog(@RequestParam UUID institutionId) {
        return service.getCatalog(institutionId);
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.LeadResponse createLead(@Valid @RequestBody AdmissionDto.LeadRequest request) {
        return service.createLead(request, AdmissionSourceChannel.FORM.name());
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.ApplicationResponse createApplication(@Valid @RequestBody AdmissionDto.ApplicationRequest request) {
        return service.createApplication(request, AdmissionSourceChannel.FORM);
    }

    @PostMapping("/applications/{applicationCode}/payment/status")
    public AdmissionDto.PublicPaymentResponse paymentStatus(
            @PathVariable String applicationCode,
            @Valid @RequestBody AdmissionDto.PublicApplicationAccessRequest request
    ) {
        return publicPaymentService.getStatus(applicationCode, request);
    }

    @PostMapping("/applications/{applicationCode}/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PublicPaymentResponse issuePayment(
            @PathVariable String applicationCode,
            @Valid @RequestBody AdmissionDto.PublicApplicationAccessRequest request
    ) {
        return publicPaymentService.issueOrGetInvoice(applicationCode, request);
    }

    @PostMapping("/applications/{applicationCode}/payment-proof")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PublicPaymentResponse submitApplicationPaymentProof(
            @PathVariable String applicationCode,
            @Valid @RequestBody AdmissionDto.PublicPaymentProofRequest request
    ) {
        return publicPaymentService.submitPaymentProof(applicationCode, request);
    }

    @PostMapping("/invoices/{invoiceId}/payment-proofs")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PaymentProofResponse submitPaymentProof(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody AdmissionDto.PaymentProofRequest request
    ) {
        return service.submitPaymentProof(invoiceId, request);
    }
}
