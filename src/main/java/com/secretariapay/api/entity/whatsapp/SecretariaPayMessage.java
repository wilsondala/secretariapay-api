package com.secretariapay.api.entity.whatsapp;

import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "secretariapay_messages")
public class SecretariaPayMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "institution_name", length = 220)
    private String institutionName;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "student_number", length = 60)
    private String studentNumber;

    @Column(name = "student_name", length = 180)
    private String studentName;

    @Column(name = "charge_id")
    private UUID chargeId;

    @Column(name = "charge_code", length = 80)
    private String chargeCode;

    @Column(name = "payment_proof_id")
    private UUID paymentProofId;

    @Column(name = "receipt_id")
    private UUID receiptId;

    @Column(name = "receipt_code", length = 80)
    private String receiptCode;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 40)
    private String channel = "WHATSAPP";

    @Column(length = 20)
    private String language = "pt-AO";

    @Column(name = "recipient_phone", length = 40)
    private String recipientPhone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SecretariaPayMessageStatus status = SecretariaPayMessageStatus.GENERATED;

    @Column(name = "provider_message_id", length = 180)
    private String providerMessageId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = SecretariaPayMessageStatus.GENERATED;
        }

        if (channel == null || channel.isBlank()) {
            channel = "WHATSAPP";
        }

        if (language == null || language.isBlank()) {
            language = "pt-AO";
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = SecretariaPayMessageStatus.GENERATED;
        }
    }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public SecretariaPayMessage setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getInstitutionName() { return institutionName; }
    public SecretariaPayMessage setInstitutionName(String institutionName) { this.institutionName = institutionName; return this; }
    public UUID getStudentId() { return studentId; }
    public SecretariaPayMessage setStudentId(UUID studentId) { this.studentId = studentId; return this; }
    public String getStudentNumber() { return studentNumber; }
    public SecretariaPayMessage setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getStudentName() { return studentName; }
    public SecretariaPayMessage setStudentName(String studentName) { this.studentName = studentName; return this; }
    public UUID getChargeId() { return chargeId; }
    public SecretariaPayMessage setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public SecretariaPayMessage setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public UUID getPaymentProofId() { return paymentProofId; }
    public SecretariaPayMessage setPaymentProofId(UUID paymentProofId) { this.paymentProofId = paymentProofId; return this; }
    public UUID getReceiptId() { return receiptId; }
    public SecretariaPayMessage setReceiptId(UUID receiptId) { this.receiptId = receiptId; return this; }
    public String getReceiptCode() { return receiptCode; }
    public SecretariaPayMessage setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; return this; }
    public String getType() { return type; }
    public SecretariaPayMessage setType(String type) { this.type = type; return this; }
    public String getChannel() { return channel; }
    public SecretariaPayMessage setChannel(String channel) { this.channel = channel; return this; }
    public String getLanguage() { return language; }
    public SecretariaPayMessage setLanguage(String language) { this.language = language; return this; }
    public String getRecipientPhone() { return recipientPhone; }
    public SecretariaPayMessage setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; return this; }
    public String getMessage() { return message; }
    public SecretariaPayMessage setMessage(String message) { this.message = message; return this; }
    public SecretariaPayMessageStatus getStatus() { return status; }
    public SecretariaPayMessage setStatus(SecretariaPayMessageStatus status) { this.status = status; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public SecretariaPayMessage setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getFailureReason() { return failureReason; }
    public SecretariaPayMessage setFailureReason(String failureReason) { this.failureReason = failureReason; return this; }
    public LocalDateTime getSentAt() { return sentAt; }
    public SecretariaPayMessage setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
    public LocalDateTime getReadAt() { return readAt; }
    public SecretariaPayMessage setReadAt(LocalDateTime readAt) { this.readAt = readAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
