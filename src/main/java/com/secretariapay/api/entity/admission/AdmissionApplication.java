package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_applications")
public class AdmissionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_code", nullable = false, unique = true, length = 80)
    private String applicationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private AdmissionCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private AdmissionLead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desired_course_id", nullable = false)
    private Course desiredCourse;

    @Column(name = "desired_shift", nullable = false, length = 40)
    private String desiredShift;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", nullable = false, length = 30)
    private AdmissionSourceChannel sourceChannel = AdmissionSourceChannel.INTERNAL;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "document_type", length = 30)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(length = 180)
    private String email;

    @Column(name = "previous_school", length = 180)
    private String previousSchool;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String municipality;

    @Column(name = "documents_complete", nullable = false)
    private Boolean documentsComplete = false;

    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted = false;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdmissionApplicationStatus status = AdmissionApplicationStatus.DRAFT;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

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
        if (status == null) status = AdmissionApplicationStatus.DRAFT;
        if (sourceChannel == null) sourceChannel = AdmissionSourceChannel.INTERNAL;
        if (documentsComplete == null) documentsComplete = false;
        if (termsAccepted == null) termsAccepted = false;
        if (Boolean.TRUE.equals(termsAccepted) && termsAcceptedAt == null) termsAcceptedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (status == null) status = AdmissionApplicationStatus.DRAFT;
        if (sourceChannel == null) sourceChannel = AdmissionSourceChannel.INTERNAL;
        if (documentsComplete == null) documentsComplete = false;
        if (termsAccepted == null) termsAccepted = false;
        if (Boolean.TRUE.equals(termsAccepted) && termsAcceptedAt == null) termsAcceptedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getApplicationCode() { return applicationCode; }
    public AdmissionApplication setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; return this; }
    public Institution getInstitution() { return institution; }
    public AdmissionApplication setInstitution(Institution institution) { this.institution = institution; return this; }
    public AdmissionCampaign getCampaign() { return campaign; }
    public AdmissionApplication setCampaign(AdmissionCampaign campaign) { this.campaign = campaign; return this; }
    public AdmissionLead getLead() { return lead; }
    public AdmissionApplication setLead(AdmissionLead lead) { this.lead = lead; return this; }
    public Course getDesiredCourse() { return desiredCourse; }
    public AdmissionApplication setDesiredCourse(Course desiredCourse) { this.desiredCourse = desiredCourse; return this; }
    public String getDesiredShift() { return desiredShift; }
    public AdmissionApplication setDesiredShift(String desiredShift) { this.desiredShift = desiredShift; return this; }
    public String getAcademicYear() { return academicYear; }
    public AdmissionApplication setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public AdmissionSourceChannel getSourceChannel() { return sourceChannel; }
    public AdmissionApplication setSourceChannel(AdmissionSourceChannel sourceChannel) { this.sourceChannel = sourceChannel; return this; }
    public String getFullName() { return fullName; }
    public AdmissionApplication setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getDocumentType() { return documentType; }
    public AdmissionApplication setDocumentType(String documentType) { this.documentType = documentType; return this; }
    public String getDocumentNumber() { return documentNumber; }
    public AdmissionApplication setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; return this; }
    public LocalDate getBirthDate() { return birthDate; }
    public AdmissionApplication setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; return this; }
    public String getPhone() { return phone; }
    public AdmissionApplication setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public AdmissionApplication setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getEmail() { return email; }
    public AdmissionApplication setEmail(String email) { this.email = email; return this; }
    public String getPreviousSchool() { return previousSchool; }
    public AdmissionApplication setPreviousSchool(String previousSchool) { this.previousSchool = previousSchool; return this; }
    public String getProvince() { return province; }
    public AdmissionApplication setProvince(String province) { this.province = province; return this; }
    public String getMunicipality() { return municipality; }
    public AdmissionApplication setMunicipality(String municipality) { this.municipality = municipality; return this; }
    public Boolean getDocumentsComplete() { return documentsComplete; }
    public AdmissionApplication setDocumentsComplete(Boolean documentsComplete) { this.documentsComplete = documentsComplete; return this; }
    public Boolean getTermsAccepted() { return termsAccepted; }
    public AdmissionApplication setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; return this; }
    public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public AdmissionApplicationStatus getStatus() { return status; }
    public AdmissionApplication setStatus(AdmissionApplicationStatus status) { this.status = status; return this; }
    public String getNotes() { return notes; }
    public AdmissionApplication setNotes(String notes) { this.notes = notes; return this; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public AdmissionApplication setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public AdmissionApplication setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
