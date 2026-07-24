package com.secretariapay.api.controller.config;

import com.secretariapay.api.dto.config.DcrChargeEvaluationRequest;
import com.secretariapay.api.dto.config.DcrChargeEvaluationResponse;
import com.secretariapay.api.dto.config.InstitutionDcrPolicyResponse;
import com.secretariapay.api.service.config.InstitutionDcrPolicyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institution-dcr-policies")
public class InstitutionDcrPolicyController {

    private final InstitutionDcrPolicyService service;

    public InstitutionDcrPolicyController(InstitutionDcrPolicyService service) {
        this.service = service;
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'DCR_COORDENACAO', 'ROLE_DCR_COORDENACAO', 'DCR_OPERADOR', 'ROLE_DCR_OPERADOR', 'TIC', 'ROLE_TIC', 'AUDITORIA', 'ROLE_AUDITORIA')")
    public InstitutionDcrPolicyResponse findByInstitution(@PathVariable UUID institutionId) {
        return service.findActiveByInstitution(institutionId);
    }

    @PostMapping("/institution/{institutionId}/evaluate")
    @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'ADMIN_IMETRO', 'ROLE_ADMIN_IMETRO', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA', 'DCR_COORDENACAO', 'ROLE_DCR_COORDENACAO', 'DCR_OPERADOR', 'ROLE_DCR_OPERADOR', 'TIC', 'ROLE_TIC', 'AUDITORIA', 'ROLE_AUDITORIA')")
    public DcrChargeEvaluationResponse evaluate(@PathVariable UUID institutionId, @RequestBody DcrChargeEvaluationRequest request) {
        return service.evaluate(institutionId, request);
    }
}
