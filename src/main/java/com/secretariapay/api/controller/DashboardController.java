package com.secretariapay.api.controller;

import com.secretariapay.api.dto.dashboard.DashboardSummaryResponse;
import com.secretariapay.api.service.DashboardService;
import com.secretariapay.api.service.MeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final MeService meService;

    public DashboardController(
            DashboardService service,
            MeService meService
    ) {
        this.service = service;
        this.meService = meService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public DashboardSummaryResponse summary() {
        return service.getSummary();
    }

    @GetMapping("/company/{companyId}/summary")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public DashboardSummaryResponse companySummary(
            @PathVariable UUID companyId
    ) {
        return service.getCompanySummary(companyId);
    }

    @GetMapping("/me/summary")
    public DashboardSummaryResponse myDashboardSummary() {
        return meService.getMyDashboardSummary();
    }
}
