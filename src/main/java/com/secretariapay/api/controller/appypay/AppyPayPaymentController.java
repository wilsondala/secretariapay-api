package com.secretariapay.api.controller.appypay;

import com.secretariapay.api.dto.appypay.AppyPayChargeRequest;
import com.secretariapay.api.dto.appypay.AppyPayChargeResponse;
import com.secretariapay.api.dto.appypay.AppyPayMockReferenceRequest;
import com.secretariapay.api.dto.appypay.AppyPayProviderResponse;
import com.secretariapay.api.service.appypay.AppyPayPaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/financial/appypay")
public class AppyPayPaymentController {

    private final AppyPayPaymentService service;

    public AppyPayPaymentController(AppyPayPaymentService service) {
        this.service = service;
    }

    @PostMapping("/reference")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public AppyPayChargeResponse createReferenceCharge(@Valid @RequestBody AppyPayChargeRequest request) {
        return service.createReferenceCharge(request);
    }

    @PostMapping("/gpo")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public AppyPayChargeResponse createGpoCharge(@Valid @RequestBody AppyPayChargeRequest request) {
        return service.createGpoCharge(request);
    }

    @PostMapping("/sandbox/reference-processing")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public AppyPayProviderResponse processSandboxReference(@Valid @RequestBody AppyPayMockReferenceRequest request) {
        return service.processReferenceInSandbox(request.getEntity(), request.getReferenceNumber());
    }
}
