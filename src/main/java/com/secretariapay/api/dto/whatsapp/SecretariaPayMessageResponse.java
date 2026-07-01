package com.secretariapay.api.dto.whatsapp;

import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class SecretariaPayMessageResponse {

    private UUID id;
    private UUID institutionId;
    private String institutionName;
    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID chargeId;
    private String chargeCode;
    private UUID paymentProofId;
    private UUID receiptId;
    private String receiptCode;
    private String type;
    private String channel;
    private String language;
    private String recipientPhone;
    private String message;
    private SecretariaPayMessageStatus status;
    private String providerMessageId;
    private String failureReason;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public SecretariaPayMessageResponse setId(UUID id) { this.id = id; return this; }
    public UUID getInstitutionId() { return institutionId; }
    public SecretariaPayMessageResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getInstitutionName() { return institutionName; }
    public SecretariaPayMessageResponse setInstitutionName(String institutionName) { this.institutionName = institutionName; return this; }
    public UUID getStudentId() { return studentId; }
    public SecretariaPayMessageResponse setStudentId(UUID studentId) { this.studentId = studentId; return this; }
    public String getStudentNumber() { return studentNumber; }
    public SecretariaPayMessageResponse setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getStudentName() { return studentName; }
    public SecretariaPayMessageResponse setStudentName(String studentName) { this.studentName = studentName; return this; }
    public UUID getChargeId() { return chargeId; }
    public SecretariaPayMessageResponse setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public SecretariaPayMessageResponse setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public UUID getPaymentProofId() { return paymentProofId; }
    public SecretariaPayMessageResponse setPaymentProofId(UUID paymentProofId) { this.paymentProofId = paymentProofId; return this; }
    public UUID getReceiptId() { return receiptId; }
    public SecretariaPayMessageResponse setReceiptId(UUID receiptId) { this.receiptId = receiptId; return this; }
    public String getReceiptCode() { return receiptCode; }
    public SecretariaPayMessageResponse setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; return this; }
    public String getType() { return type; }
    public SecretariaPayMessageResponse setType(String type) { this.type = type; return this; }
    public String getChannel() { return channel; }
    public SecretariaPayMessageResponse setChannel(String channel) { this.channel = channel; return this; }
    public String getLanguage() { return language; }
    public SecretariaPayMessageResponse setLanguage(String language) { this.language = language; return this; }
    public String getRecipientPhone() { return recipientPhone; }
    public SecretariaPayMessageResponse setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; return this; }
    public String getMessage() { return message; }
    public SecretariaPayMessageResponse setMessage(String message) { this.message = message; return this; }
    public SecretariaPayMessageStatus getStatus() { return status; }
    public SecretariaPayMessageResponse setStatus(SecretariaPayMessageStatus status) { this.status = status; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public SecretariaPayMessageResponse setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getFailureReason() { return failureReason; }
    public SecretariaPayMessageResponse setFailureReason(String failureReason) { this.failureReason = failureReason; return this; }
    public LocalDateTime getSentAt() { return sentAt; }
    public SecretariaPayMessageResponse setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
    public LocalDateTime getReadAt() { return readAt; }
    public SecretariaPayMessageResponse setReadAt(LocalDateTime readAt) { this.readAt = readAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public SecretariaPayMessageResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public SecretariaPayMessageResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
