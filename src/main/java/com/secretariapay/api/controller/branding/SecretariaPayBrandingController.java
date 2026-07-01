package com.secretariapay.api.controller.branding;

import com.secretariapay.api.dto.branding.SecretariaPayBrandingResponse;
import com.secretariapay.api.service.branding.SecretariaPayBrandingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/branding")
public class SecretariaPayBrandingController {

    private final SecretariaPayBrandingService brandingService;

    public SecretariaPayBrandingController(SecretariaPayBrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/secretariapay")
    public SecretariaPayBrandingResponse getSecretariaPayBranding(HttpServletRequest request) {
        return brandingService.getBranding(request);
    }
}
