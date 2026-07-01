package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/receipts")
public class PublicReceiptController {

    private final ReceiptService receiptService;

    public PublicReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/validate/{receiptCode}")
    public ReceiptResponse validateByReceiptCode(@PathVariable String receiptCode) {
        return receiptService.findByCode(receiptCode);
    }
}
