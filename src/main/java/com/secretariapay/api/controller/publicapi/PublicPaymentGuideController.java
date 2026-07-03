package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.service.financial.PaymentGuidePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/payment-guides")
public class PublicPaymentGuideController {

    private final PaymentGuidePdfService paymentGuidePdfService;

    public PublicPaymentGuideController(PaymentGuidePdfService paymentGuidePdfService) {
        this.paymentGuidePdfService = paymentGuidePdfService;
    }

    @GetMapping("/{chargeCode}/pdf")
    public ResponseEntity<byte[]> downloadPaymentGuideByChargeCode(@PathVariable String chargeCode) {
        byte[] pdf = paymentGuidePdfService.generateByChargeCode(chargeCode);
        String filename = "guia-pagamento-" + chargeCode + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(pdf);
    }
}
