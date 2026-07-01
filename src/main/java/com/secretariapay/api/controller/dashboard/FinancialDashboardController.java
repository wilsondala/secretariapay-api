package com.secretariapay.api.controller.dashboard;

import com.secretariapay.api.dto.dashboard.FinancialDashboardResponse;
import com.secretariapay.api.service.dashboard.FinancialDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/financial")
public class FinancialDashboardController {

    private final FinancialDashboardService service;

    public FinancialDashboardController(FinancialDashboardService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public FinancialDashboardResponse getSummary() {
        return service.getSummary();
    }
}
