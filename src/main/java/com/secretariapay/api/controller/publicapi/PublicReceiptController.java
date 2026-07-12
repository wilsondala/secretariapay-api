package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.service.financial.OfficialDocumentFilenameService;
import com.secretariapay.api.service.financial.ReceiptPdfService;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/public/receipts")
public class PublicReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptPdfService receiptPdfService;
    private final OfficialDocumentFilenameService filenameService;

    public PublicReceiptController(
            ReceiptService receiptService,
            ReceiptPdfService receiptPdfService,
            OfficialDocumentFilenameService filenameService
    ) {
        this.receiptService = receiptService;
        this.receiptPdfService = receiptPdfService;
        this.filenameService = filenameService;
    }

    @GetMapping("/validate/{receiptCode}")
    public ReceiptResponse validateByReceiptCode(@PathVariable String receiptCode) {
        return receiptService.findByCode(receiptCode);
    }

    @GetMapping("/validate/{receiptCode}/authentic")
    public ReceiptResponse validateAuthenticity(@PathVariable String receiptCode, @RequestParam String hash) {
        return receiptService.validate(receiptCode, hash);
    }

    @GetMapping("/{receiptCode}/pdf")
    public ResponseEntity<byte[]> downloadPdfByReceiptCode(@PathVariable String receiptCode) {
        ReceiptResponse receipt = receiptService.findByCode(receiptCode);
        byte[] pdf = receiptPdfService.generateReceiptPdf(receipt.getId());
        String filename = filenameService.receiptFilename(receipt.getStudentNumber(), receipt.getReceiptCode());

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Document-Type", "COMPROVATIVO_DE_PAGAMENTOS")
                .header("X-Document-Code", receipt.getReceiptCode())
                .body(pdf);
    }

    @GetMapping("/validate/{receiptCode}/pdf")
    public ResponseEntity<byte[]> downloadPdfFromValidationRoute(@PathVariable String receiptCode) {
        return downloadPdfByReceiptCode(receiptCode);
    }
}
