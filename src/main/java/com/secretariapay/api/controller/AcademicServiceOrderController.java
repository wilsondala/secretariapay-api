package com.secretariapay.api.controller;

import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.service.academic.AcademicServiceOrderService;
import com.secretariapay.api.service.financial.ChargeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-service-orders")
public class AcademicServiceOrderController {

    private static final String ADMINS = "'ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'";
    private static final String READ_AUTHORITIES = "hasAnyAuthority(" + ADMINS + ",'DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','SECRETARIA','ROLE_SECRETARIA','AUDITORIA','ROLE_AUDITORIA')";
    private static final String DCR_AUTHORITIES = "hasAnyAuthority(" + ADMINS + ",'DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR')";
    private static final String SECRETARIA_AUTHORITIES = "hasAnyAuthority(" + ADMINS + ",'SECRETARIA','ROLE_SECRETARIA')";
    private static final String DIRECAO_AUTHORITIES = "hasAnyAuthority(" + ADMINS + ",'DIRECAO','ROLE_DIRECAO')";

    private final AcademicServiceOrderService service;
    private final ChargeService chargeService;

    public AcademicServiceOrderController(
            AcademicServiceOrderService service,
            ChargeService chargeService
    ) {
        this.service = service;
        this.chargeService = chargeService;
    }

    @GetMapping
    @PreAuthorize(READ_AUTHORITIES)
    public List<AcademicServiceOrderDto.Response> list(
            @RequestParam(required = false) AcademicServiceOrderStatus status,
            @RequestParam(required = false) UUID studentId
    ) {
        return service.list(status, studentId);
    }

    @GetMapping("/archive")
    @PreAuthorize(READ_AUTHORITIES)
    public List<AcademicServiceOrderDto.Response> archive() {
        return service.archive();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_AUTHORITIES)
    public AcademicServiceOrderDto.Response findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(DCR_AUTHORITIES)
    public AcademicServiceOrderDto.Response create(@RequestBody AcademicServiceOrderDto.CreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/request-payment")
    @PreAuthorize(DCR_AUTHORITIES)
    public AcademicServiceOrderDto.Response requestPayment(
            @PathVariable UUID id,
            @RequestBody(required = false) AcademicServiceOrderDto.RequestPaymentRequest request
    ) {
        return service.requestPayment(id, request);
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize(DCR_AUTHORITIES)
    public AcademicServiceOrderDto.Response confirmPayment(@PathVariable UUID id) {
        AcademicServiceOrderDto.Response order = service.findById(id);
        if (order.chargeId() == null) {
            throw new IllegalStateException("O pedido ainda não possui cobrança associada.");
        }
        chargeService.confirmPayment(order.chargeId());
        return service.findById(id);
    }

    @PostMapping("/{id}/generate-document")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response generateDocument(@PathVariable UUID id) {
        return service.generateDocument(id);
    }

    @PostMapping("/{id}/ready-for-print")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response markReadyForPrint(@PathVariable UUID id) {
        return service.markReadyForPrint(id);
    }

    @PostMapping("/{id}/print")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response markPrinted(@PathVariable UUID id) {
        return service.markPrinted(id);
    }

    @PostMapping("/{id}/submit-signature")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response submitForSignature(@PathVariable UUID id) {
        return service.submitForSignature(id);
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize(DIRECAO_AUTHORITIES)
    public AcademicServiceOrderDto.Response sign(@PathVariable UUID id) {
        return service.sign(id);
    }

    @PostMapping("/{id}/ready-for-pickup")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response markReadyForPickup(
            @PathVariable UUID id,
            @RequestBody(required = false) AcademicServiceOrderDto.ActionRequest request
    ) {
        return service.markReadyForPickup(id, request);
    }

    @PostMapping("/{id}/send-pickup-whatsapp")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response sendPickupWhatsapp(@PathVariable UUID id) {
        return service.sendPickupWhatsapp(id);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderDto.Response deliver(
            @PathVariable UUID id,
            @RequestBody AcademicServiceOrderDto.ActionRequest request
    ) {
        return service.deliver(id, request);
    }
}
