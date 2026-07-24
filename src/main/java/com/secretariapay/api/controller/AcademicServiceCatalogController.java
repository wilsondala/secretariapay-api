package com.secretariapay.api.controller;

import com.secretariapay.api.dto.financial.AcademicServiceCatalogDto;
import com.secretariapay.api.service.financial.AcademicServiceCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-services")
public class AcademicServiceCatalogController {

    private final AcademicServiceCatalogService service;

    public AcademicServiceCatalogController(AcademicServiceCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','SECRETARIA','ROLE_SECRETARIA','OPERADOR_ATENDIMENTO','ROLE_OPERADOR_ATENDIMENTO','TIC','ROLE_TIC')")
    public List<AcademicServiceCatalogDto.Response> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) String category
    ) {
        return service.list(activeOnly, category);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AcademicServiceCatalogDto.Response findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','TIC','ROLE_TIC')")
    public AcademicServiceCatalogDto.Response create(@RequestBody AcademicServiceCatalogDto.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','TIC','ROLE_TIC')")
    public AcademicServiceCatalogDto.Response update(
            @PathVariable UUID id,
            @RequestBody AcademicServiceCatalogDto.Request request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','TIC','ROLE_TIC')")
    public AcademicServiceCatalogDto.Response setActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        return service.setActive(id, active);
    }
}
