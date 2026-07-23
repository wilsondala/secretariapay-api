package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.service.admission.AdmissionPaymentGuidePdfService;
import com.secretariapay.api.service.admission.AdmissionPaymentProofFileStorageService;
import com.secretariapay.api.service.admission.AdmissionPublicApplicationWorkflowService;
import com.secretariapay.api.service.admission.AdmissionPublicPaymentService;
import com.secretariapay.api.service.admission.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/admissions")
public class PublicAdmissionController {

    private final AdmissionService service;
    private final AdmissionPublicApplicationWorkflowService applicationWorkflowService;
    private final AdmissionPublicPaymentService publicPaymentService;
    private final AdmissionPaymentGuidePdfService paymentGuidePdfService;
    private final AdmissionPaymentProofFileStorageService paymentProofFileStorageService;

    public PublicAdmissionController(
            AdmissionService service,
            AdmissionPublicApplicationWorkflowService applicationWorkflowService,
            AdmissionPublicPaymentService publicPaymentService,
            AdmissionPaymentGuidePdfService paymentGuidePdfService,
            AdmissionPaymentProofFileStorageService paymentProofFileStorageService
    ) {
        this.service = service;
        this.applicationWorkflowService = applicationWorkflowService;
        this.publicPaymentService = publicPaymentService;
        this.paymentGuidePdfService = paymentGuidePdfService;
        this.paymentProofFileStorageService = paymentProofFileStorageService;
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
        return applicationWorkflowService.createApplication(request);
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

    @PostMapping("/applications/{applicationCode}/payment-guide")
    public ResponseEntity<byte[]> paymentGuide(
            @PathVariable String applicationCode,
            @Valid @RequestBody AdmissionDto.PublicApplicationAccessRequest request
    ) {
        byte[] pdf = paymentGuidePdfService.generate(applicationCode, request);
        String fileName = "Guia_Inscricao_IMETRO_" + applicationCode.replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/applications/{applicationCode}/payment-proof")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PublicPaymentResponse submitApplicationPaymentProof(
            @PathVariable String applicationCode,
            @Valid @RequestBody AdmissionDto.PublicPaymentProofRequest request
    ) {
        return publicPaymentService.submitPaymentProof(applicationCode, request);
    }

    @PostMapping(
            value = "/applications/{applicationCode}/payment-proof/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDto.PublicPaymentResponse uploadApplicationPaymentProof(
            @PathVariable String applicationCode,
            @RequestPart("documentNumber") String documentNumber,
            @RequestPart("file") MultipartFile file
    ) {
        return paymentProofFileStorageService.storeAndSubmit(
                applicationCode,
                documentNumber,
                file
        );
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
