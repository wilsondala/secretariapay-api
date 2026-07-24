package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessagePreviewResponse;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/messages")
public class SecretariaPayMessageTemplateController {

    private final SecretariaPayMessageTemplateService service;

    public SecretariaPayMessageTemplateController(SecretariaPayMessageTemplateService service) {
        this.service = service;
    }

    @GetMapping("/charges/{chargeId}/before-due")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse beforeDue(
            @PathVariable UUID chargeId,
            @RequestParam(required = false, defaultValue = "5") Integer daysBefore
    ) {
        return service.beforeDue(chargeId, daysBefore);
    }

    @GetMapping("/charges/{chargeId}/due-today")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse dueToday(@PathVariable UUID chargeId) {
        return service.dueToday(chargeId);
    }

    @GetMapping("/charges/{chargeId}/overdue")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse overdue(
            @PathVariable UUID chargeId,
            @RequestParam(required = false, defaultValue = "1") Integer daysLate
    ) {
        return service.overdue(chargeId, daysLate);
    }

    @GetMapping("/payment-proofs/{paymentProofId}/received")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse proofReceived(@PathVariable UUID paymentProofId) {
        return service.proofReceived(paymentProofId);
    }

    @GetMapping("/payment-proofs/{paymentProofId}/approved")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse proofApproved(@PathVariable UUID paymentProofId) {
        return service.proofApproved(paymentProofId);
    }

    @GetMapping("/receipts/{receiptId}/issued")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse receiptIssued(@PathVariable UUID receiptId) {
        return service.receiptIssued(receiptId);
    }

    @GetMapping("/students/{studentId}/regularized")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO')")
    public SecretariaPayMessagePreviewResponse regularized(@PathVariable UUID studentId) {
        return service.regularized(studentId);
    }
}
