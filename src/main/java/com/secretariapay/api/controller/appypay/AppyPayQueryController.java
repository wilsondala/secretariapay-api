package com.secretariapay.api.controller.appypay;

import com.secretariapay.api.dto.appypay.AppyPayProviderResponse;
import com.secretariapay.api.service.appypay.AppyPayClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/appypay")
public class AppyPayQueryController {

    private final AppyPayClient appyPayClient;

    public AppyPayQueryController(AppyPayClient appyPayClient) {
        this.appyPayClient = appyPayClient;
    }

    @GetMapping("/charges/{merchantTransactionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public AppyPayProviderResponse findChargeDetails(@PathVariable String merchantTransactionId) {
        return appyPayClient.findCharge(merchantTransactionId);
    }
}
