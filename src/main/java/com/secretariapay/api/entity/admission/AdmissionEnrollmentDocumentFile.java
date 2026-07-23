package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionEnrollmentDocumentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "admission_enrollment_document_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admission_enrollment_document_file_stored_name",
                columnNames = "stored_name"
        )
)
public class AdmissionEnrollmentDocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 60)
    private AdmissionEnrollmentDocumentType documentType;

    @Column(name = "stored_name", nullable = false, length = 180)
    private String storedName;

    @Column(name = "original_file_name", nullable = false, length = 180)
    private String originalFileName;

    @Column(name = "mime_type", nullable = false, length = 80)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "uploaded_by", nullable = false, length = 180)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (uploadedAt == null) uploadedAt = now;
        if (createdAt == null) createdAt = now;
    }

    public UUID getId() { return id; }
    public AdmissionApplication getApplication() { return application; }
    public AdmissionEnrollmentDocumentFile setApplication(AdmissionApplication application) { this.application = application; return this; }
    public AdmissionEnrollmentDocumentType getDocumentType() { return documentType; }
    public AdmissionEnrollmentDocumentFile setDocumentType(AdmissionEnrollmentDocumentType documentType) { this.documentType = documentType; return this; }
    public String getStoredName() { return storedName; }
    public AdmissionEnrollmentDocumentFile setStoredName(String storedName) { this.storedName = storedName; return this; }
    public String getOriginalFileName() { return originalFileName; }
    public AdmissionEnrollmentDocumentFile setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }
    public String getMimeType() { return mimeType; }
    public AdmissionEnrollmentDocumentFile setMimeType(String mimeType) { this.mimeType = mimeType; return this; }
    public long getFileSize() { return fileSize; }
    public AdmissionEnrollmentDocumentFile setFileSize(long fileSize) { this.fileSize = fileSize; return this; }
    public String getUploadedBy() { return uploadedBy; }
    public AdmissionEnrollmentDocumentFile setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; return this; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public AdmissionEnrollmentDocumentFile setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
