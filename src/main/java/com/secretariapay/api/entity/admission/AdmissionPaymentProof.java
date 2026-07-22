package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionPaymentProofStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_payment_proofs")
public class AdmissionPaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private AdmissionInvoice invoice;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name", length = 180)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdmissionPaymentProofStatus status = AdmissionPaymentProofStatus.PENDING_REVIEW;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = AdmissionPaymentProofStatus.PENDING_REVIEW;
        if (submittedAt == null) submittedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public AdmissionInvoice getInvoice() { return invoice; }
    public AdmissionPaymentProof setInvoice(AdmissionInvoice invoice) { this.invoice = invoice; return this; }
    public String getFileUrl() { return fileUrl; }
    public AdmissionPaymentProof setFileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
    public String getFileName() { return fileName; }
    public AdmissionPaymentProof setFileName(String fileName) { this.fileName = fileName; return this; }
    public String getMimeType() { return mimeType; }
    public AdmissionPaymentProof setMimeType(String mimeType) { this.mimeType = mimeType; return this; }
    public AdmissionPaymentProofStatus getStatus() { return status; }
    public AdmissionPaymentProof setStatus(AdmissionPaymentProofStatus status) { this.status = status; return this; }
    public String getReviewedBy() { return reviewedBy; }
    public AdmissionPaymentProof setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; return this; }
    public String getReviewNote() { return reviewNote; }
    public AdmissionPaymentProof setReviewNote(String reviewNote) { this.reviewNote = reviewNote; return this; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public AdmissionPaymentProof setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
