package com.secretariapay.api.dto.financial;

import java.time.LocalDateTime;
import java.util.UUID;

public class MockAutomaticPaymentResponse {

    private String event;
    private Boolean automatic;
    private String paymentMethod;
    private String settlementStatus;
    private String externalTransactionId;
    private UUID chargeId;
    private String chargeCode;
    private String chargeStatus;
    private UUID paymentProofId;
    private String paymentProofStatus;
    private UUID receiptId;
    private String receiptCode;
    private String receiptPdfUrl;
    private String receiptValidationUrl;
    private UUID receiptMessageId;
    private String receiptMessageStatus;
    private String providerMessageId;
    private String recipientPhone;
    private String note;
    private LocalDateTime processedAt;

    public String getEvent() { return event; }
    public MockAutomaticPaymentResponse setEvent(String event) { this.event = event; return this; }
    public Boolean getAutomatic() { return automatic; }
    public MockAutomaticPaymentResponse setAutomatic(Boolean automatic) { this.automatic = automatic; return this; }
    public String getPaymentMethod() { return paymentMethod; }
    public MockAutomaticPaymentResponse setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public String getSettlementStatus() { return settlementStatus; }
    public MockAutomaticPaymentResponse setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; return this; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public MockAutomaticPaymentResponse setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; return this; }
    public UUID getChargeId() { return chargeId; }
    public MockAutomaticPaymentResponse setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public MockAutomaticPaymentResponse setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public String getChargeStatus() { return chargeStatus; }
    public MockAutomaticPaymentResponse setChargeStatus(String chargeStatus) { this.chargeStatus = chargeStatus; return this; }
    public UUID getPaymentProofId() { return paymentProofId; }
    public MockAutomaticPaymentResponse setPaymentProofId(UUID paymentProofId) { this.paymentProofId = paymentProofId; return this; }
    public String getPaymentProofStatus() { return paymentProofStatus; }
    public MockAutomaticPaymentResponse setPaymentProofStatus(String paymentProofStatus) { this.paymentProofStatus = paymentProofStatus; return this; }
    public UUID getReceiptId() { return receiptId; }
    public MockAutomaticPaymentResponse setReceiptId(UUID receiptId) { this.receiptId = receiptId; return this; }
    public String getReceiptCode() { return receiptCode; }
    public MockAutomaticPaymentResponse setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; return this; }
    public String getReceiptPdfUrl() { return receiptPdfUrl; }
    public MockAutomaticPaymentResponse setReceiptPdfUrl(String receiptPdfUrl) { this.receiptPdfUrl = receiptPdfUrl; return this; }
    public String getReceiptValidationUrl() { return receiptValidationUrl; }
    public MockAutomaticPaymentResponse setReceiptValidationUrl(String receiptValidationUrl) { this.receiptValidationUrl = receiptValidationUrl; return this; }
    public UUID getReceiptMessageId() { return receiptMessageId; }
    public MockAutomaticPaymentResponse setReceiptMessageId(UUID receiptMessageId) { this.receiptMessageId = receiptMessageId; return this; }
    public String getReceiptMessageStatus() { return receiptMessageStatus; }
    public MockAutomaticPaymentResponse setReceiptMessageStatus(String receiptMessageStatus) { this.receiptMessageStatus = receiptMessageStatus; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public MockAutomaticPaymentResponse setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getRecipientPhone() { return recipientPhone; }
    public MockAutomaticPaymentResponse setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; return this; }
    public String getNote() { return note; }
    public MockAutomaticPaymentResponse setNote(String note) { this.note = note; return this; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public MockAutomaticPaymentResponse setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
}
