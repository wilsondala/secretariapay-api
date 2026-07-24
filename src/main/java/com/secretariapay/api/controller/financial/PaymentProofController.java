package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.PaymentProofRequest;
import com.secretariapay.api.dto.financial.PaymentProofResponse;
import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.service.financial.PaymentProofAttachmentService;
import com.secretariapay.api.service.financial.PaymentProofService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-proofs")
public class PaymentProofController {

    private static final String READ_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR'," +
            "'AUDITORIA','ROLE_AUDITORIA')";

    private static final String REVIEW_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR')";

    private static final String CHARGE_READ_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR'," +
            "'SECRETARIA','ROLE_SECRETARIA'," +
            "'AUDITORIA','ROLE_AUDITORIA')";

    private final PaymentProofService service;
    private final PaymentProofAttachmentService attachmentService;

    public PaymentProofController(PaymentProofService service, PaymentProofAttachmentService attachmentService) {
        this.service = service;
        this.attachmentService = attachmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentProofResponse create(@Valid @RequestBody PaymentProofRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize(READ_AUTHORITIES)
    public List<PaymentProofResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_AUTHORITIES)
    public PaymentProofResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/attachment")
    @PreAuthorize(READ_AUTHORITIES)
    public ResponseEntity<byte[]> openAttachment(@PathVariable UUID id) {
        return attachmentService.openAttachment(id);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize(READ_AUTHORITIES)
    public List<PaymentProofResponse> findByStatus(@PathVariable PaymentProofStatus status) {
        return service.findByStatus(status);
    }

    @GetMapping("/charge/{chargeId}")
    @PreAuthorize(CHARGE_READ_AUTHORITIES)
    public List<PaymentProofResponse> findByCharge(@PathVariable UUID chargeId) {
        return service.findByCharge(chargeId);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize(REVIEW_AUTHORITIES)
    public PaymentProofResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.approve(id, request);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize(REVIEW_AUTHORITIES)
    public PaymentProofResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.reject(id, request);
    }
}
