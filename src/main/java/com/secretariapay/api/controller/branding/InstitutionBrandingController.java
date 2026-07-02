package com.secretariapay.api.controller.branding;

import com.secretariapay.api.dto.branding.InstitutionBrandingResponse;
import com.secretariapay.api.service.branding.InstitutionBrandingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/public/branding/institutions")
@RestController
public class InstitutionBrandingController {

    private final InstitutionBrandingService service;

    public InstitutionBrandingController(InstitutionBrandingService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public InstitutionBrandingResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }
}
