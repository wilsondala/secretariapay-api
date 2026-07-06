package com.secretariapay.api.dto.financial;

import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentProofResponse {

    private UUID id;
    private UUID chargeId;
    private String chargeCode;
    private String studentName;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private String submittedByPhone;
    private LocalDateTime submittedAt;
    private PaymentProofStatus status;
    private UUID reviewedByUserId;
    private String reviewedByName;
    private String reviewNote;
    private String receiptCode;
    private String receiptPdfUrl;
    private String receiptValidationUrl;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public PaymentProofResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public PaymentProofResponse setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public PaymentProofResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getStudentName() {
        return studentName;
    }

    public PaymentProofResponse setStudentName(String studentName) {
        this.studentName = studentName;
        return this;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public PaymentProofResponse setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public PaymentProofResponse setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PaymentProofResponse setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getSubmittedByPhone() {
        return submittedByPhone;
    }

    public PaymentProofResponse setSubmittedByPhone(String submittedByPhone) {
        this.submittedByPhone = submittedByPhone;
        return this;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public PaymentProofResponse setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public PaymentProofStatus getStatus() {
        return status;
    }

    public PaymentProofResponse setStatus(PaymentProofStatus status) {
        this.status = status;
        return this;
    }

    public UUID getReviewedByUserId() {
        return reviewedByUserId;
    }

    public PaymentProofResponse setReviewedByUserId(UUID reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
        return this;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public PaymentProofResponse setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
        return this;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public PaymentProofResponse setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
        return this;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public PaymentProofResponse setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
        return this;
    }

    public String getReceiptPdfUrl() {
        return receiptPdfUrl;
    }

    public PaymentProofResponse setReceiptPdfUrl(String receiptPdfUrl) {
        this.receiptPdfUrl = receiptPdfUrl;
        return this;
    }

    public String getReceiptValidationUrl() {
        return receiptValidationUrl;
    }

    public PaymentProofResponse setReceiptValidationUrl(String receiptValidationUrl) {
        this.receiptValidationUrl = receiptValidationUrl;
        return this;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public PaymentProofResponse setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PaymentProofResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PaymentProofResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
