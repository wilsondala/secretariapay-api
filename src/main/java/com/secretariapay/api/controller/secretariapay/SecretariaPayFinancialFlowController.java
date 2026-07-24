package com.secretariapay.api.controller.secretariapay;

import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.dto.secretariapay.SecretariaPayFinancialFlowResponse;
import com.secretariapay.api.service.secretariapay.SecretariaPayFinancialFlowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/financial-flow")
public class SecretariaPayFinancialFlowController {

    private final SecretariaPayFinancialFlowService service;

    public SecretariaPayFinancialFlowController(SecretariaPayFinancialFlowService service) {
        this.service = service;
    }

    @PostMapping("/charges/{chargeId}/send-guide")
    @PreAuthorize("hasAnyAuthority(" +
            "'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', " +
            "'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', " +
            "'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', " +
            "'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', " +
            "'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', " +
            "'DIRECAO', 'ROLE_DIRECAO', " +
            "'FINANCEIRO', 'ROLE_FINANCEIRO', " +
            "'TESOURARIA', 'ROLE_TESOURARIA', " +
            "'DCR_COORDENACAO', 'ROLE_DCR_COORDENACAO', " +
            "'DCR_OPERADOR', 'ROLE_DCR_OPERADOR', " +
            "'SECRETARIA', 'ROLE_SECRETARIA', " +
            "'OPERADOR_ATENDIMENTO', 'ROLE_OPERADOR_ATENDIMENTO', " +
            "'TIC', 'ROLE_TIC')")
    public SecretariaPayFinancialFlowResponse sendPaymentGuide(@PathVariable UUID chargeId) {
        return service.sendPaymentGuide(chargeId);
    }

    @PostMapping("/payment-proofs/{paymentProofId}/approve-complete")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public SecretariaPayFinancialFlowResponse approveProofIssueReceiptAndNotify(
            @PathVariable UUID paymentProofId,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.approveProofIssueReceiptAndNotify(paymentProofId, request);
    }

    @PostMapping("/payment-proofs/{paymentProofId}/reject-complete")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public SecretariaPayFinancialFlowResponse rejectProofAndNotifyStudent(
            @PathVariable UUID paymentProofId,
            @Valid @RequestBody PaymentProofReviewRequest request
    ) {
        return service.rejectProofAndNotifyStudent(paymentProofId, request);
    }
}
