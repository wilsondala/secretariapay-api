package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final ReceiptService service;

    public ReceiptController(ReceiptService service) {
        this.service = service;
    }

    @PostMapping("/charge/{chargeId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public ReceiptResponse issueForCharge(@PathVariable UUID chargeId) {
        return service.issueForCharge(chargeId);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<ReceiptResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public ReceiptResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/code/{receiptCode}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public ReceiptResponse findByCode(@PathVariable String receiptCode) {
        return service.findByCode(receiptCode);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'TESOURARIA', 'ROLE_TESOURARIA')")
    public ReceiptResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
