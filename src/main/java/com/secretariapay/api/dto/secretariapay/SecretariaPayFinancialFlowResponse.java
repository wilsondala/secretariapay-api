package com.secretariapay.api.dto.secretariapay;

import java.time.LocalDateTime;
import java.util.UUID;

public class SecretariaPayFinancialFlowResponse {

    private String flow;
    private String status;
    private String message;
    private UUID chargeId;
    private String chargeCode;
    private UUID paymentProofId;
    private UUID receiptId;
    private String receiptCode;
    private UUID guideMessageId;
    private String guideDispatchStatus;
    private String guideProviderMessageId;
    private UUID receiptMessageId;
    private String receiptDispatchStatus;
    private String receiptProviderMessageId;
    private UUID studentNotificationMessageId;
    private String studentNotificationDispatchStatus;
    private String studentNotificationProviderMessageId;
    private String pdfUrl;
    private String validationUrl;
    private String failureReason;
    private LocalDateTime processedAt;

    public String getFlow() { return flow; }
    public SecretariaPayFinancialFlowResponse setFlow(String flow) { this.flow = flow; return this; }
    public String getStatus() { return status; }
    public SecretariaPayFinancialFlowResponse setStatus(String status) { this.status = status; return this; }
    public String getMessage() { return message; }
    public SecretariaPayFinancialFlowResponse setMessage(String message) { this.message = message; return this; }
    public UUID getChargeId() { return chargeId; }
    public SecretariaPayFinancialFlowResponse setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public SecretariaPayFinancialFlowResponse setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public UUID getPaymentProofId() { return paymentProofId; }
    public SecretariaPayFinancialFlowResponse setPaymentProofId(UUID paymentProofId) { this.paymentProofId = paymentProofId; return this; }
    public UUID getReceiptId() { return receiptId; }
    public SecretariaPayFinancialFlowResponse setReceiptId(UUID receiptId) { this.receiptId = receiptId; return this; }
    public String getReceiptCode() { return receiptCode; }
    public SecretariaPayFinancialFlowResponse setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; return this; }
    public UUID getGuideMessageId() { return guideMessageId; }
    public SecretariaPayFinancialFlowResponse setGuideMessageId(UUID guideMessageId) { this.guideMessageId = guideMessageId; return this; }
    public String getGuideDispatchStatus() { return guideDispatchStatus; }
    public SecretariaPayFinancialFlowResponse setGuideDispatchStatus(String guideDispatchStatus) { this.guideDispatchStatus = guideDispatchStatus; return this; }
    public String getGuideProviderMessageId() { return guideProviderMessageId; }
    public SecretariaPayFinancialFlowResponse setGuideProviderMessageId(String guideProviderMessageId) { this.guideProviderMessageId = guideProviderMessageId; return this; }
    public UUID getReceiptMessageId() { return receiptMessageId; }
    public SecretariaPayFinancialFlowResponse setReceiptMessageId(UUID receiptMessageId) { this.receiptMessageId = receiptMessageId; return this; }
    public String getReceiptDispatchStatus() { return receiptDispatchStatus; }
    public SecretariaPayFinancialFlowResponse setReceiptDispatchStatus(String receiptDispatchStatus) { this.receiptDispatchStatus = receiptDispatchStatus; return this; }
    public String getReceiptProviderMessageId() { return receiptProviderMessageId; }
    public SecretariaPayFinancialFlowResponse setReceiptProviderMessageId(String receiptProviderMessageId) { this.receiptProviderMessageId = receiptProviderMessageId; return this; }
    public UUID getStudentNotificationMessageId() { return studentNotificationMessageId; }
    public SecretariaPayFinancialFlowResponse setStudentNotificationMessageId(UUID studentNotificationMessageId) { this.studentNotificationMessageId = studentNotificationMessageId; return this; }
    public String getStudentNotificationDispatchStatus() { return studentNotificationDispatchStatus; }
    public SecretariaPayFinancialFlowResponse setStudentNotificationDispatchStatus(String studentNotificationDispatchStatus) { this.studentNotificationDispatchStatus = studentNotificationDispatchStatus; return this; }
    public String getStudentNotificationProviderMessageId() { return studentNotificationProviderMessageId; }
    public SecretariaPayFinancialFlowResponse setStudentNotificationProviderMessageId(String studentNotificationProviderMessageId) { this.studentNotificationProviderMessageId = studentNotificationProviderMessageId; return this; }
    public String getPdfUrl() { return pdfUrl; }
    public SecretariaPayFinancialFlowResponse setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
    public String getValidationUrl() { return validationUrl; }
    public SecretariaPayFinancialFlowResponse setValidationUrl(String validationUrl) { this.validationUrl = validationUrl; return this; }
    public String getFailureReason() { return failureReason; }
    public SecretariaPayFinancialFlowResponse setFailureReason(String failureReason) { this.failureReason = failureReason; return this; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public SecretariaPayFinancialFlowResponse setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
}
