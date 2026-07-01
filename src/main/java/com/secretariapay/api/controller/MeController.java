package com.secretariapay.api.controller;

import com.secretariapay.api.dto.dashboard.DashboardSummaryResponse;
import com.secretariapay.api.dto.me.MeResponse;
import com.secretariapay.api.dto.me.MyCompanyResponse;
import com.secretariapay.api.service.MeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final MeService service;

    public MeController(MeService service) {
        this.service = service;
    }

    @GetMapping
    public MeResponse me() {
        return service.getMe();
    }

    @GetMapping("/company")
    public MyCompanyResponse myCompany() {
        return service.getMyCompany();
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse myDashboardSummary() {
        return service.getMyDashboardSummary();
    }
}
