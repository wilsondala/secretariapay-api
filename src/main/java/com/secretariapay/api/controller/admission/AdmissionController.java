package com.secretariapay.api.controller.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import com.secretariapay.api.service.admission.AdmissionDocumentationService;
import com.secretariapay.api.service.admission.AdmissionEnrollmentDocumentChecklistService;
import com.secretariapay.api.service.admission.AdmissionLeadWorkflowService;
import com.secretariapay.api.service.admission.AdmissionPaymentApprovalWorkflowService;
import com.secretariapay.api.service.admission.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController {

    private static final String ADMINS = "'ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'";
    private static final String READ = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','MARKETING','ROLE_MARKETING','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC','AUDITORIA','ROLE_AUDITORIA')";
    private static final String CAPTURE = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','MARKETING','ROLE_MARKETING','OPERADOR_ATENDIMENTO','ROLE_OPERADOR_ATENDIMENTO')";
    private static final String ADMISSIONS = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','SECRETARIA','ROLE_SECRETARIA')";
    private static final String FINANCE = "hasAnyAuthority(" + ADMINS + ",'DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')";

    private final AdmissionService service;
    private final AdmissionDocumentationService documentationService;
    private final AdmissionEnrollmentDocumentChecklistService enrollmentDocumentChecklistService;
    private final AdmissionLeadWorkflowService leadWorkflowService;
    private final AdmissionPaymentApprovalWorkflowService paymentApprovalWorkflowService;

    public AdmissionController(
            AdmissionService service,
            AdmissionDocumentationService documentationService,
            AdmissionEnrollmentDocumentChecklistService enrollmentDocumentChecklistService,
            AdmissionLeadWorkflowService leadWorkflowService,
            AdmissionPaymentApprovalWorkflowService paymentApprovalWorkflowService
    ) {
        this.service = service;
        this.documentationService = documentationService;
        this.enrollmentDocumentChecklistService = enrollmentDocumentChecklistService;
        this.leadWorkflowService = leadWorkflowService;
        this.paymentApprovalWorkflowService = paymentApprovalWorkflowService;
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAPTURE)
    public AdmissionDto.LeadResponse createLead(@Valid @RequestBody AdmissionDto.LeadRequest request) {
        return service.createLead(request);
    }

    @GetMapping("/leads")
    @PreAuthorize(READ)
    public List<AdmissionDto.LeadResponse> listLeads(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) AdmissionLeadStatus status
    ) {
        return service.listLeads(institutionId, status);
    }

    @PatchMapping("/leads/{leadId}/status")
    @PreAuthorize(CAPTURE)
    public AdmissionDto.LeadResponse updateLeadStatus(
            @PathVariable UUID leadId,
            @RequestParam AdmissionLeadStatus status,
            @RequestParam(required = false) String notes
    ) {
        return leadWorkflowService.updateLeadStatus(leadId, status, notes);
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.ApplicationResponse createApplication(@Valid @RequestBody AdmissionDto.ApplicationRequest request) {
        return leadWorkflowService.createApplication(request);
    }

    @GetMapping("/applications")
    @PreAuthorize(READ)
    public List<AdmissionDto.ApplicationResponse> listApplications(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) AdmissionApplicationStatus status,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String shift
    ) {
        return service.listApplications(institutionId, status, courseId, shift);
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize(READ)
    public AdmissionDto.ApplicationResponse getApplication(@PathVariable UUID applicationId) {
        return service.getApplication(applicationId);
    }

    @PostMapping("/applications/{applicationId}/submit")
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.ApplicationResponse submitApplication(@PathVariable UUID applicationId) {
        return service.submitApplication(applicationId);
    }

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.ApplicationResponse updateApplicationStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody AdmissionDto.ApplicationStatusRequest request
    ) {
        return service.updateApplicationStatus(applicationId, request);
    }

    @PatchMapping("/applications/{applicationId}/documents")
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.ApplicationResponse reviewDocuments(
            @PathVariable UUID applicationId,
            @Valid @RequestBody AdmissionDto.ApplicationDocumentsRequest request
    ) {
        return documentationService.reviewDocuments(applicationId, request);
    }

    @GetMapping("/applications/{applicationId}/enrollment-documents")
    @PreAuthorize(READ)
    public AdmissionDto.EnrollmentDocumentChecklistResponse getEnrollmentDocuments(
            @PathVariable UUID applicationId
    ) {
        return enrollmentDocumentChecklistService.get(applicationId);
    }

    @PutMapping("/applications/{applicationId}/enrollment-documents")
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.EnrollmentDocumentChecklistResponse reviewEnrollmentDocuments(
            @PathVariable UUID applicationId,
            @Valid @RequestBody AdmissionDto.EnrollmentDocumentChecklistRequest request
    ) {
        return enrollmentDocumentChecklistService.review(applicationId, request);
    }

    @PostMapping("/applications/{applicationId}/invoice")
    @PreAuthorize(FINANCE)
    public AdmissionDto.ApplicationResponse issueInvoice(
            @PathVariable UUID applicationId,
            @Valid @RequestBody AdmissionDto.InvoiceRequest request
    ) {
        return service.issueInvoice(applicationId, request);
    }

    @PostMapping("/invoices/{invoiceId}/payment-proofs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMISSIONS)
    public AdmissionDto.PaymentProofResponse submitPaymentProof(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody AdmissionDto.PaymentProofRequest request
    ) {
        return service.submitPaymentProof(invoiceId, request);
    }

    @PostMapping("/payment-proofs/{proofId}/approve")
    @PreAuthorize(FINANCE)
    public AdmissionDto.ApplicationResponse approvePaymentProof(
            @PathVariable UUID proofId,
            @Valid @RequestBody AdmissionDto.ReviewPaymentProofRequest request
    ) {
        return paymentApprovalWorkflowService.approvePaymentProof(proofId, request);
    }

    @PostMapping("/payment-proofs/{proofId}/reject")
    @PreAuthorize(FINANCE)
    public AdmissionDto.ApplicationResponse rejectPaymentProof(
            @PathVariable UUID proofId,
            @Valid @RequestBody AdmissionDto.ReviewPaymentProofRequest request
    ) {
        return service.rejectPaymentProof(proofId, request);
    }

    @PostMapping("/invoices/{invoiceId}/confirm-payment")
    @PreAuthorize(FINANCE)
    public AdmissionDto.ApplicationResponse confirmInvoicePayment(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody AdmissionDto.ReviewPaymentProofRequest request
    ) {
        return paymentApprovalWorkflowService.confirmInvoicePayment(invoiceId, request);
    }

    @GetMapping("/dashboard")
    @PreAuthorize(READ)
    public AdmissionDto.DashboardResponse dashboard(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String shift
    ) {
        return service.dashboard(institutionId, courseId, shift);
    }
}
