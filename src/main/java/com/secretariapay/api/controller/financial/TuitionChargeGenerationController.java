package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.TuitionChargeGenerationRequest;
import com.secretariapay.api.dto.financial.TuitionChargeGenerationResponse;
import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryRequest;
import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryResponse;
import com.secretariapay.api.service.financial.TuitionChargeGenerationService;
import com.secretariapay.api.service.financial.TuitionChargeGuideDeliveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/imetro/tuition-charges")
public class TuitionChargeGenerationController {

    private final TuitionChargeGenerationService service;
    private final TuitionChargeGuideDeliveryService guideDeliveryService;

    public TuitionChargeGenerationController(
            TuitionChargeGenerationService service,
            TuitionChargeGuideDeliveryService guideDeliveryService
    ) {
        this.service = service;
        this.guideDeliveryService = guideDeliveryService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public TuitionChargeGenerationResponse generate(@Valid @RequestBody TuitionChargeGenerationRequest request) {
        return service.generate(request);
    }

    @PostMapping("/send-guides")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL', 'ADMIN_INSTITUTION', 'ROLE_ADMIN_INSTITUTION', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'DCR_COORDENACAO', 'ROLE_DCR_COORDENACAO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public TuitionChargeGuideDeliveryResponse sendGuides(@Valid @RequestBody TuitionChargeGuideDeliveryRequest request) {
        return guideDeliveryService.sendGuides(request);
    }
}
