package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.service.GuidePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/public/demo/receipts")
public class PublicDemoReceiptController {

    private final GuidePdfService guidePdfService;

    public PublicDemoReceiptController(GuidePdfService guidePdfService) {
        this.guidePdfService = guidePdfService;
    }

    @GetMapping("/{receiptCode}/pdf")
    public ResponseEntity<byte[]> downloadDemoReceipt(
            @PathVariable String receiptCode,
            @RequestParam(name = "student", required = false) String student,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "method", required = false) String method
    ) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(firstNonBlank(student, "Estudante Teste WhatsApp Angola"));
        request.setStudentNumber("IMETRO-2026-TESTE-002");
        request.setGuideCode(firstNonBlank(receiptCode, "BORD-DEMO"));
        request.setGuideUrl("https://secretariapay-api.paixaoangola.com/api/v1/public/demo/receipts/" + firstNonBlank(receiptCode, "BORD-DEMO") + "/pdf");
        request.setAmount(new BigDecimal("165915.00"));
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 10));
        request.setMessage("Bordereau/comprovativo emitido automaticamente pelo SecretáriaPay após confirmação de pagamento. Referência: "
                + firstNonBlank(month, "Maio/2026 + Junho/2026 + Julho/2026")
                + ". Forma de pagamento: " + firstNonBlank(method, "AppyPay / InfinitePay teste real") + ".");

        byte[] pdf = guidePdfService.generateGuidePdf(request);
        String filename = "bordereau-" + firstNonBlank(receiptCode, "BORD-DEMO") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(pdf);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) return value.trim();
        }
        return "";
    }
}
