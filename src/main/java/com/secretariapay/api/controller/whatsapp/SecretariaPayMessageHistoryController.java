package com.secretariapay.api.controller.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageStatusRequest;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretariapay/message-history")
public class SecretariaPayMessageHistoryController {

    private final SecretariaPayMessageHistoryService service;

    public SecretariaPayMessageHistoryController(SecretariaPayMessageHistoryService service) {
        this.service = service;
    }

    @PostMapping("/charges/{chargeId}/before-due")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateBeforeDue(
            @PathVariable UUID chargeId,
            @RequestParam(required = false) Integer daysBefore
    ) {
        return service.generateBeforeDue(chargeId, daysBefore);
    }

    @PostMapping("/charges/{chargeId}/due-today")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateDueToday(@PathVariable UUID chargeId) {
        return service.generateDueToday(chargeId);
    }

    @PostMapping("/charges/{chargeId}/overdue")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateOverdue(
            @PathVariable UUID chargeId,
            @RequestParam(required = false) Integer daysLate
    ) {
        return service.generateOverdue(chargeId, daysLate);
    }

    @PostMapping("/payment-proofs/{paymentProofId}/received")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateProofReceived(@PathVariable UUID paymentProofId) {
        return service.generateProofReceived(paymentProofId);
    }

    @PostMapping("/payment-proofs/{paymentProofId}/approved")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateProofApproved(@PathVariable UUID paymentProofId) {
        return service.generateProofApproved(paymentProofId);
    }

    @PostMapping("/receipts/{receiptId}/issued")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateReceiptIssued(@PathVariable UUID receiptId) {
        return service.generateReceiptIssued(receiptId);
    }

    @PostMapping("/students/{studentId}/regularized")
    @ResponseStatus(HttpStatus.CREATED)
    public SecretariaPayMessageResponse generateRegularized(@PathVariable UUID studentId) {
        return service.generateRegularized(studentId);
    }

    @GetMapping("/institutions/{institutionId}")
    public List<SecretariaPayMessageResponse> findByInstitution(@PathVariable UUID institutionId) {
        return service.findByInstitution(institutionId);
    }

    @GetMapping("/students/{studentId}")
    public List<SecretariaPayMessageResponse> findByStudent(@PathVariable UUID studentId) {
        return service.findByStudent(studentId);
    }

    @GetMapping("/charges/{chargeId}")
    public List<SecretariaPayMessageResponse> findByCharge(@PathVariable UUID chargeId) {
        return service.findByCharge(chargeId);
    }

    @PatchMapping("/{messageId}/queue")
    public SecretariaPayMessageResponse markQueued(@PathVariable UUID messageId) {
        return service.markQueued(messageId);
    }

    @PatchMapping("/{messageId}/sent")
    public SecretariaPayMessageResponse markSent(
            @PathVariable UUID messageId,
            @RequestBody(required = false) SecretariaPayMessageStatusRequest request
    ) {
        return service.markSent(messageId, request);
    }

    @PatchMapping("/{messageId}/failed")
    public SecretariaPayMessageResponse markFailed(
            @PathVariable UUID messageId,
            @RequestBody(required = false) SecretariaPayMessageStatusRequest request
    ) {
        return service.markFailed(messageId, request);
    }

    @PatchMapping("/{messageId}/read")
    public SecretariaPayMessageResponse markRead(@PathVariable UUID messageId) {
        return service.markRead(messageId);
    }
}
