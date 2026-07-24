package com.secretariapay.api.controller.institution;

import com.secretariapay.api.dto.institution.InstitutionSettingsRequest;
import com.secretariapay.api.dto.institution.InstitutionSettingsResponse;
import com.secretariapay.api.service.institution.InstitutionSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institution-settings")
public class InstitutionSettingsController {

    private final InstitutionSettingsService service;

    public InstitutionSettingsController(InstitutionSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO')")
    public List<InstitutionSettingsResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION')")
    public InstitutionSettingsResponse findByInstitutionId(@PathVariable UUID institutionId) {
        return service.findByInstitutionId(institutionId);
    }

    @GetMapping("/slug/{publicSlug}")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO')")
    public InstitutionSettingsResponse findByPublicSlug(@PathVariable String publicSlug) {
        return service.findByPublicSlug(publicSlug);
    }

    @PutMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION')")
    public InstitutionSettingsResponse createOrUpdate(
            @PathVariable UUID institutionId,
            @Valid @RequestBody InstitutionSettingsRequest request
    ) {
        return service.createOrUpdate(institutionId, request);
    }
}
