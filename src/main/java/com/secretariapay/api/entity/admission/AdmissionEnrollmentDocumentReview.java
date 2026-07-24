package com.secretariapay.api.entity.admission;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "admission_enrollment_document_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admission_enrollment_document_review_application",
                columnNames = "application_id"
        )
)
public class AdmissionEnrollmentDocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(name = "two_passport_photos", nullable = false)
    private Boolean twoPassportPhotos = false;

    @Column(name = "authenticated_certificate_copy", nullable = false)
    private Boolean authenticatedCertificateCopy = false;

    @Column(name = "identity_document_copy", nullable = false)
    private Boolean identityDocumentCopy = false;

    @Column(name = "studied_abroad", nullable = false)
    private Boolean studiedAbroad = false;

    @Column(name = "education_equivalence_copy", nullable = false)
    private Boolean educationEquivalenceCopy = false;

    @Column(name = "secondary_education_completed", nullable = false)
    private Boolean secondaryEducationCompleted = false;

    @Column(name = "age_eligible", nullable = false)
    private Boolean ageEligible = false;

    @Column(name = "originals_presented", nullable = false)
    private Boolean originalsPresented = false;

    @Column(name = "originals_verified", nullable = false)
    private Boolean originalsVerified = false;

    @Column(name = "originals_verified_by", length = 180)
    private String originalsVerifiedBy;

    @Column(name = "originals_verified_at")
    private LocalDateTime originalsVerifiedAt;

    @Column(name = "originals_verification_notes", columnDefinition = "text")
    private String originalsVerificationNotes;

    @Column(name = "originals_due_date")
    private LocalDate originalsDueDate;

    @Column(name = "originals_block_active", nullable = false)
    private Boolean originalsBlockActive = false;

    @Column(name = "originals_blocked_at")
    private LocalDateTime originalsBlockedAt;

    @Column(name = "documents_complete", nullable = false)
    private Boolean documentsComplete = false;

    @Column(name = "reviewed_by", nullable = false, length = 180)
    private String reviewedBy;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "reviewed_at", nullable = false)
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
        normalizeBooleans();
        if (reviewedAt == null) reviewedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        normalizeBooleans();
        if (reviewedAt == null) reviewedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeBooleans() {
        if (twoPassportPhotos == null) twoPassportPhotos = false;
        if (authenticatedCertificateCopy == null) authenticatedCertificateCopy = false;
        if (identityDocumentCopy == null) identityDocumentCopy = false;
        if (studiedAbroad == null) studiedAbroad = false;
        if (educationEquivalenceCopy == null) educationEquivalenceCopy = false;
        if (secondaryEducationCompleted == null) secondaryEducationCompleted = false;
        if (ageEligible == null) ageEligible = false;
        if (originalsPresented == null) originalsPresented = false;
        if (originalsVerified == null) originalsVerified = false;
        if (originalsBlockActive == null) originalsBlockActive = false;
        if (documentsComplete == null) documentsComplete = false;
    }

    public UUID getId() { return id; }
    public AdmissionApplication getApplication() { return application; }
    public AdmissionEnrollmentDocumentReview setApplication(AdmissionApplication application) { this.application = application; return this; }
    public Boolean getTwoPassportPhotos() { return twoPassportPhotos; }
    public AdmissionEnrollmentDocumentReview setTwoPassportPhotos(Boolean twoPassportPhotos) { this.twoPassportPhotos = twoPassportPhotos; return this; }
    public Boolean getAuthenticatedCertificateCopy() { return authenticatedCertificateCopy; }
    public AdmissionEnrollmentDocumentReview setAuthenticatedCertificateCopy(Boolean authenticatedCertificateCopy) { this.authenticatedCertificateCopy = authenticatedCertificateCopy; return this; }
    public Boolean getIdentityDocumentCopy() { return identityDocumentCopy; }
    public AdmissionEnrollmentDocumentReview setIdentityDocumentCopy(Boolean identityDocumentCopy) { this.identityDocumentCopy = identityDocumentCopy; return this; }
    public Boolean getStudiedAbroad() { return studiedAbroad; }
    public AdmissionEnrollmentDocumentReview setStudiedAbroad(Boolean studiedAbroad) { this.studiedAbroad = studiedAbroad; return this; }
    public Boolean getEducationEquivalenceCopy() { return educationEquivalenceCopy; }
    public AdmissionEnrollmentDocumentReview setEducationEquivalenceCopy(Boolean educationEquivalenceCopy) { this.educationEquivalenceCopy = educationEquivalenceCopy; return this; }
    public Boolean getSecondaryEducationCompleted() { return secondaryEducationCompleted; }
    public AdmissionEnrollmentDocumentReview setSecondaryEducationCompleted(Boolean secondaryEducationCompleted) { this.secondaryEducationCompleted = secondaryEducationCompleted; return this; }
    public Boolean getAgeEligible() { return ageEligible; }
    public AdmissionEnrollmentDocumentReview setAgeEligible(Boolean ageEligible) { this.ageEligible = ageEligible; return this; }
    public Boolean getOriginalsPresented() { return originalsPresented; }
    public AdmissionEnrollmentDocumentReview setOriginalsPresented(Boolean originalsPresented) { this.originalsPresented = originalsPresented; return this; }
    public Boolean getOriginalsVerified() { return originalsVerified; }
    public AdmissionEnrollmentDocumentReview setOriginalsVerified(Boolean originalsVerified) { this.originalsVerified = originalsVerified; return this; }
    public String getOriginalsVerifiedBy() { return originalsVerifiedBy; }
    public AdmissionEnrollmentDocumentReview setOriginalsVerifiedBy(String originalsVerifiedBy) { this.originalsVerifiedBy = originalsVerifiedBy; return this; }
    public LocalDateTime getOriginalsVerifiedAt() { return originalsVerifiedAt; }
    public AdmissionEnrollmentDocumentReview setOriginalsVerifiedAt(LocalDateTime originalsVerifiedAt) { this.originalsVerifiedAt = originalsVerifiedAt; return this; }
    public String getOriginalsVerificationNotes() { return originalsVerificationNotes; }
    public AdmissionEnrollmentDocumentReview setOriginalsVerificationNotes(String originalsVerificationNotes) { this.originalsVerificationNotes = originalsVerificationNotes; return this; }
    public LocalDate getOriginalsDueDate() { return originalsDueDate; }
    public AdmissionEnrollmentDocumentReview setOriginalsDueDate(LocalDate originalsDueDate) { this.originalsDueDate = originalsDueDate; return this; }
    public Boolean getOriginalsBlockActive() { return originalsBlockActive; }
    public AdmissionEnrollmentDocumentReview setOriginalsBlockActive(Boolean originalsBlockActive) { this.originalsBlockActive = originalsBlockActive; return this; }
    public LocalDateTime getOriginalsBlockedAt() { return originalsBlockedAt; }
    public AdmissionEnrollmentDocumentReview setOriginalsBlockedAt(LocalDateTime originalsBlockedAt) { this.originalsBlockedAt = originalsBlockedAt; return this; }
    public Boolean getDocumentsComplete() { return documentsComplete; }
    public AdmissionEnrollmentDocumentReview setDocumentsComplete(Boolean documentsComplete) { this.documentsComplete = documentsComplete; return this; }
    public String getReviewedBy() { return reviewedBy; }
    public AdmissionEnrollmentDocumentReview setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; return this; }
    public String getNotes() { return notes; }
    public AdmissionEnrollmentDocumentReview setNotes(String notes) { this.notes = notes; return this; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public AdmissionEnrollmentDocumentReview setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
