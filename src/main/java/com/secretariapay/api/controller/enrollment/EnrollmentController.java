package com.secretariapay.api.controller.enrollment;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import com.secretariapay.api.service.enrollment.EnrollmentPaymentCompletionWorkflowService;
import com.secretariapay.api.service.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private static final String ADMINS = "'ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'";
    private static final String READ = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC','AUDITORIA','ROLE_AUDITORIA')";
    private static final String ACADEMIC = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','SECRETARIA','ROLE_SECRETARIA')";
    private static final String FINANCE = "hasAnyAuthority(" + ADMINS + ",'DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')";

    private final EnrollmentService service;
    private final EnrollmentPaymentCompletionWorkflowService paymentCompletionWorkflowService;

    public EnrollmentController(
            EnrollmentService service,
            EnrollmentPaymentCompletionWorkflowService paymentCompletionWorkflowService
    ) {
        this.service = service;
        this.paymentCompletionWorkflowService = paymentCompletionWorkflowService;
    }

    @PostMapping("/from-admission/{applicationId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ACADEMIC)
    public EnrollmentDto.EnrollmentResponse createEnrollmentFromAdmission(
            @PathVariable UUID applicationId,
            @Valid @RequestBody EnrollmentDto.EnrollmentFromAdmissionRequest request
    ) {
        return service.createEnrollmentFromAdmission(applicationId, request);
    }

    @PostMapping("/reenrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ACADEMIC)
    public EnrollmentDto.EnrollmentResponse createReenrollment(
            @Valid @RequestBody EnrollmentDto.ReenrollmentRequest request
    ) {
        return service.createReenrollment(request);
    }

    @GetMapping
    @PreAuthorize(READ)
    public List<EnrollmentDto.EnrollmentResponse> list(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) EnrollmentRequestType requestType,
            @RequestParam(required = false) EnrollmentRequestStatus status
    ) {
        return service.list(institutionId, requestType, status);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize(READ)
    public EnrollmentDto.EnrollmentResponse get(@PathVariable UUID requestId) {
        return service.get(requestId);
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize(READ)
    public EnrollmentDto.DashboardResponse dashboard(@RequestParam UUID institutionId) {
        return service.dashboard(institutionId);
    }

    @PostMapping("/invoices/{invoiceId}/payment-proofs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ACADEMIC)
    public EnrollmentDto.PaymentProofResponse submitPaymentProof(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody EnrollmentDto.PaymentProofRequest request
    ) {
        return service.submitPaymentProof(invoiceId, request);
    }

    @PostMapping("/payment-proofs/{proofId}/approve")
    @PreAuthorize(FINANCE)
    public EnrollmentDto.EnrollmentResponse approvePaymentProof(
            @PathVariable UUID proofId,
            @Valid @RequestBody EnrollmentDto.ReviewPaymentRequest request
    ) {
        return paymentCompletionWorkflowService.approvePaymentProof(proofId, request);
    }

    @PostMapping("/payment-proofs/{proofId}/reject")
    @PreAuthorize(FINANCE)
    public EnrollmentDto.EnrollmentResponse rejectPaymentProof(
            @PathVariable UUID proofId,
            @Valid @RequestBody EnrollmentDto.ReviewPaymentRequest request
    ) {
        return service.rejectPaymentProof(proofId, request);
    }

    @PostMapping("/invoices/{invoiceId}/confirm-payment")
    @PreAuthorize(FINANCE)
    public EnrollmentDto.EnrollmentResponse confirmPayment(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody EnrollmentDto.ReviewPaymentRequest request
    ) {
        return paymentCompletionWorkflowService.confirmPayment(invoiceId, request);
    }
}
