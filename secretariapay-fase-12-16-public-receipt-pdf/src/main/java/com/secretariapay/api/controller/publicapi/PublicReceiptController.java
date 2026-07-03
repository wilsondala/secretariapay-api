package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.service.financial.ReceiptPdfService;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/receipts")
public class PublicReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptPdfService receiptPdfService;

    public PublicReceiptController(
            ReceiptService receiptService,
            ReceiptPdfService receiptPdfService
    ) {
        this.receiptService = receiptService;
        this.receiptPdfService = receiptPdfService;
    }

    @GetMapping("/validate/{receiptCode}")
    public ReceiptResponse validateByReceiptCode(@PathVariable String receiptCode) {
        return receiptService.findByCode(receiptCode);
    }

    @GetMapping("/{receiptCode}/pdf")
    public ResponseEntity<byte[]> downloadPdfByReceiptCode(@PathVariable String receiptCode) {
        ReceiptResponse receipt = receiptService.findByCode(receiptCode);
        byte[] pdf = receiptPdfService.generateReceiptPdf(receipt.getId());

        String filename = "recibo-" + receipt.getReceiptCode() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString()
                )
                .body(pdf);
    }

    @GetMapping("/validate/{receiptCode}/pdf")
    public ResponseEntity<byte[]> downloadPdfFromValidationRoute(@PathVariable String receiptCode) {
        return downloadPdfByReceiptCode(receiptCode);
    }
}
