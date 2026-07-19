package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.ChargeRequest;
import com.secretariapay.api.dto.financial.ChargeResponse;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.service.financial.ChargeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/charges")
public class ChargeController {

    private final ChargeService service;

    public ChargeController(ChargeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public ChargeResponse create(@Valid @RequestBody ChargeRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<ChargeResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public ChargeResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/code/{chargeCode}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public ChargeResponse findByCode(@PathVariable String chargeCode) {
        return service.findByCode(chargeCode);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<ChargeResponse> findByStudent(@PathVariable UUID studentId) {
        return service.findByStudent(studentId);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public List<ChargeResponse> findByStatus(@PathVariable ChargeStatus status) {
        return service.findByStatus(status);
    }

    @PostMapping("/mark-overdue")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public List<ChargeResponse> markOverdueCharges() {
        return service.markOverdueCharges();
    }

    @PatchMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'DCR_COORDENACAO', 'ROLE_DCR_COORDENACAO', 'DCR_OPERADOR', 'ROLE_DCR_OPERADOR')")
    public ChargeResponse confirmPayment(@PathVariable UUID id) {
        return service.confirmPayment(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public ChargeResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
