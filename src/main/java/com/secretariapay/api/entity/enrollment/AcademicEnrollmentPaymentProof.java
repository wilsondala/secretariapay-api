package com.secretariapay.api.entity.enrollment;

import com.secretariapay.api.entity.enums.enrollment.EnrollmentPaymentProofStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_enrollment_payment_proofs")
public class AcademicEnrollmentPaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private AcademicEnrollmentInvoice invoice;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "file_name", length = 220)
    private String fileName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentPaymentProofStatus status = EnrollmentPaymentProofStatus.PENDING_REVIEW;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = EnrollmentPaymentProofStatus.PENDING_REVIEW;
        if (submittedAt == null) submittedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (status == null) status = EnrollmentPaymentProofStatus.PENDING_REVIEW;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AcademicEnrollmentInvoice getInvoice() { return invoice; }
    public AcademicEnrollmentPaymentProof setInvoice(AcademicEnrollmentInvoice invoice) { this.invoice = invoice; return this; }
    public String getFileUrl() { return fileUrl; }
    public AcademicEnrollmentPaymentProof setFileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
    public String getFileName() { return fileName; }
    public AcademicEnrollmentPaymentProof setFileName(String fileName) { this.fileName = fileName; return this; }
    public String getMimeType() { return mimeType; }
    public AcademicEnrollmentPaymentProof setMimeType(String mimeType) { this.mimeType = mimeType; return this; }
    public EnrollmentPaymentProofStatus getStatus() { return status; }
    public AcademicEnrollmentPaymentProof setStatus(EnrollmentPaymentProofStatus status) { this.status = status; return this; }
    public String getReviewedBy() { return reviewedBy; }
    public AcademicEnrollmentPaymentProof setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; return this; }
    public String getReviewNote() { return reviewNote; }
    public AcademicEnrollmentPaymentProof setReviewNote(String reviewNote) { this.reviewNote = reviewNote; return this; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public AcademicEnrollmentPaymentProof setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
