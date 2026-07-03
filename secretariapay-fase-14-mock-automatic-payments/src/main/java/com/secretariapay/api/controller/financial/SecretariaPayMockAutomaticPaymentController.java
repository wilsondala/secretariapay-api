package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.MockAutomaticPaymentRequest;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentResponse;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/mock-payments")
public class SecretariaPayMockAutomaticPaymentController {

    private final SecretariaPayMockAutomaticPaymentService service;

    public SecretariaPayMockAutomaticPaymentController(SecretariaPayMockAutomaticPaymentService service) {
        this.service = service;
    }

    @PostMapping("/charges/{chargeId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmGeneric(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, null, request);
    }

    @PostMapping("/charges/code/{chargeCode}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmGenericByCode(
            @PathVariable String chargeCode,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeCode(chargeCode, null, request);
    }

    @PostMapping("/multicaixa-express/charges/{chargeId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmMulticaixaExpress(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "MULTICAIXA_EXPRESS", request);
    }

    @PostMapping("/iban-same-bank/charges/{chargeId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmIbanSameBank(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "IBAN_MESMO_BANCO", request);
    }

    @PostMapping("/iban-other-bank/charges/{chargeId}/settle")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse settleIbanOtherBank(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "IBAN_OUTRO_BANCO", request);
    }

    @PostMapping("/deposit/charges/{chargeId}/settle-after-24h")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse settleDepositAfter24h(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "DEPOSITO_BANCARIO", request);
    }

    @PostMapping("/unitel-money/charges/{chargeId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmUnitelMoney(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "UNITEL_MONEY", request);
    }

    @PostMapping("/afrimoney/charges/{chargeId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public MockAutomaticPaymentResponse confirmAfrimoney(
            @PathVariable UUID chargeId,
            @RequestBody(required = false) MockAutomaticPaymentRequest request
    ) {
        return service.confirmByChargeId(chargeId, "AFRIMONEY", request);
    }
}
