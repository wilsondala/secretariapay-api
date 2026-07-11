package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.service.financial.ReceiptPdfService;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private static final String READ_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN','ROLE_ADMIN'," +
            "'COMPANY_ADMIN','ROLE_COMPANY_ADMIN'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR'," +
            "'OPERADOR_ATENDIMENTO','ROLE_OPERADOR_ATENDIMENTO'," +
            "'AUDITORIA','ROLE_AUDITORIA'," +
            "'TIC','ROLE_TIC')";

    private static final String WRITE_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN','ROLE_ADMIN'," +
            "'COMPANY_ADMIN','ROLE_COMPANY_ADMIN'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR')";

    private static final String CANCEL_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN','ROLE_ADMIN'," +
            "'COMPANY_ADMIN','ROLE_COMPANY_ADMIN'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL'," +
            "'ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'DIRECAO','ROLE_DIRECAO'," +
            "'TESOURARIA','ROLE_TESOURARIA'," +
            "'DCR_COORDENACAO','ROLE_DCR_COORDENACAO')";

    private final ReceiptService service;
    private final ReceiptPdfService receiptPdfService;

    public ReceiptController(ReceiptService service, ReceiptPdfService receiptPdfService) {
        this.service = service;
        this.receiptPdfService = receiptPdfService;
    }

    @PostMapping("/charge/{chargeId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE_AUTHORITIES)
    public ReceiptResponse issueForCharge(@PathVariable UUID chargeId) {
        return service.issueForCharge(chargeId);
    }

    @GetMapping
    @PreAuthorize(READ_AUTHORITIES)
    public List<ReceiptResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_AUTHORITIES)
    public ReceiptResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize(READ_AUTHORITIES)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        ReceiptResponse receipt = service.findById(id);
        byte[] pdf = receiptPdfService.generateReceiptPdf(id);
        String filename = "recibo-" + receipt.getReceiptCode() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(pdf);
    }

    @GetMapping("/code/{receiptCode}")
    @PreAuthorize(READ_AUTHORITIES)
    public ReceiptResponse findByCode(@PathVariable String receiptCode) {
        return service.findByCode(receiptCode);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(CANCEL_AUTHORITIES)
    public ReceiptResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
