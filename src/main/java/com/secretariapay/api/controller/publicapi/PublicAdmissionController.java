package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.service.admission.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/admissions")
public class PublicAdmissionController {

    private final AdmissionService service;

    public PublicAdmissionController(AdmissionService service) {
        this.service = service;
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

    @PostMapping("/invoices/{invoiceId}/payment-proofs")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PaymentProofResponse submitPaymentProof(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody AdmissionDto.PaymentProofRequest request
    ) {
        return service.submitPaymentProof(invoiceId, request);
    }
}
