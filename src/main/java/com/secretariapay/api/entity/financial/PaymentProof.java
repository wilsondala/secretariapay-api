package com.secretariapay.api.entity.financial;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_proofs")
public class PaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", length = 180)
    private String fileName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "submitted_by_phone", length = 40)
    private String submittedByPhone;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentProofStatus status = PaymentProofStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        if (status == null) {
            status = PaymentProofStatus.PENDING_REVIEW;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = PaymentProofStatus.PENDING_REVIEW;
        }
    }

    public UUID getId() {
        return id;
    }

    public Charge getCharge() {
        return charge;
    }

    public PaymentProof setCharge(Charge charge) {
        this.charge = charge;
        return this;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public PaymentProof setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public PaymentProof setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PaymentProof setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getSubmittedByPhone() {
        return submittedByPhone;
    }

    public PaymentProof setSubmittedByPhone(String submittedByPhone) {
        this.submittedByPhone = submittedByPhone;
        return this;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public PaymentProof setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public PaymentProofStatus getStatus() {
        return status;
    }

    public PaymentProof setStatus(PaymentProofStatus status) {
        this.status = status;
        return this;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public PaymentProof setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
        return this;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public PaymentProof setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
        return this;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public PaymentProof setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}