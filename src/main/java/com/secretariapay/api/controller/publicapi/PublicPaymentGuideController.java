package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.service.financial.OfficialDocumentFilenameService;
import com.secretariapay.api.service.financial.PaymentGuidePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/public/payment-guides")
public class PublicPaymentGuideController {

    private final PaymentGuidePdfService paymentGuidePdfService;
    private final OfficialDocumentFilenameService filenameService;

    public PublicPaymentGuideController(
            PaymentGuidePdfService paymentGuidePdfService,
            OfficialDocumentFilenameService filenameService
    ) {
        this.paymentGuidePdfService = paymentGuidePdfService;
        this.filenameService = filenameService;
    }

    @GetMapping("/{chargeCode}/pdf")
    public ResponseEntity<byte[]> downloadPaymentGuideByChargeCode(@PathVariable String chargeCode) {
        byte[] pdf = paymentGuidePdfService.generateByChargeCode(chargeCode);
        String filename = filenameService.paymentGuideFilenameByChargeCode(chargeCode);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Document-Type", "GUIA_DE_PAGAMENTO_ACADEMICO")
                .header("X-Document-Code", chargeCode)
                .body(pdf);
    }
}
