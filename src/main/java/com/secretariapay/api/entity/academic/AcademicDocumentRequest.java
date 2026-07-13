package com.secretariapay.api.entity.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_document_requests")
public class AcademicDocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_code", nullable = false, unique = true, length = 80)
    private String documentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "service_code", nullable = false, length = 80)
    private String serviceCode;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(nullable = false, length = 40)
    private String status = "DRAFT";

    @Column(length = 240)
    private String purpose;

    @Column(name = "declaration_text", nullable = false, columnDefinition = "text")
    private String declarationText;

    @Column(name = "signatory_name", nullable = false, length = 180)
    private String signatoryName = "Zakeu António Zengo";

    @Column(name = "signatory_role", nullable = false, length = 180)
    private String signatoryRole = "Presidente da Instituição";

    @Column(name = "signature_method", length = 80)
    private String signatureMethod;

    @Column(name = "document_hash", length = 128)
    private String documentHash;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Column(name = "demo_mode", nullable = false)
    private boolean demoMode = true;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "signed_by", length = 180)
    private String signedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) status = "DRAFT";
        if (signatoryName == null || signatoryName.isBlank()) signatoryName = "Zakeu António Zengo";
        if (signatoryRole == null || signatoryRole.isBlank()) signatoryRole = "Presidente da Instituição";
        if (versionNumber < 1) versionNumber = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getDocumentCode() { return documentCode; }
    public AcademicDocumentRequest setDocumentCode(String documentCode) { this.documentCode = documentCode; return this; }
    public Student getStudent() { return student; }
    public AcademicDocumentRequest setStudent(Student student) { this.student = student; return this; }
    public String getServiceCode() { return serviceCode; }
    public AcademicDocumentRequest setServiceCode(String serviceCode) { this.serviceCode = serviceCode; return this; }
    public String getDocumentType() { return documentType; }
    public AcademicDocumentRequest setDocumentType(String documentType) { this.documentType = documentType; return this; }
    public String getStatus() { return status; }
    public AcademicDocumentRequest setStatus(String status) { this.status = status; return this; }
    public String getPurpose() { return purpose; }
    public AcademicDocumentRequest setPurpose(String purpose) { this.purpose = purpose; return this; }
    public String getDeclarationText() { return declarationText; }
    public AcademicDocumentRequest setDeclarationText(String declarationText) { this.declarationText = declarationText; return this; }
    public String getSignatoryName() { return signatoryName; }
    public AcademicDocumentRequest setSignatoryName(String signatoryName) { this.signatoryName = signatoryName; return this; }
    public String getSignatoryRole() { return signatoryRole; }
    public AcademicDocumentRequest setSignatoryRole(String signatoryRole) { this.signatoryRole = signatoryRole; return this; }
    public String getSignatureMethod() { return signatureMethod; }
    public AcademicDocumentRequest setSignatureMethod(String signatureMethod) { this.signatureMethod = signatureMethod; return this; }
    public String getDocumentHash() { return documentHash; }
    public AcademicDocumentRequest setDocumentHash(String documentHash) { this.documentHash = documentHash; return this; }
    public int getVersionNumber() { return versionNumber; }
    public AcademicDocumentRequest setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; return this; }
    public boolean isDemoMode() { return demoMode; }
    public AcademicDocumentRequest setDemoMode(boolean demoMode) { this.demoMode = demoMode; return this; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public AcademicDocumentRequest setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; return this; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public AcademicDocumentRequest setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; return this; }
    public LocalDateTime getSentAt() { return sentAt; }
    public AcademicDocumentRequest setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
    public String getCreatedBy() { return createdBy; }
    public AcademicDocumentRequest setCreatedBy(String createdBy) { this.createdBy = createdBy; return this; }
    public String getSignedBy() { return signedBy; }
    public AcademicDocumentRequest setSignedBy(String signedBy) { this.signedBy = signedBy; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
