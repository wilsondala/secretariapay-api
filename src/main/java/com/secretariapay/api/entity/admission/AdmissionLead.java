package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_leads")
public class AdmissionLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desired_course_id")
    private Course desiredCourse;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(length = 180)
    private String email;

    @Column(name = "document_number", length = 80)
    private String documentNumber;

    @Column(name = "desired_shift", length = 40)
    private String desiredShift;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String municipality;

    @Column(name = "lead_source", length = 80)
    private String leadSource;

    @Column(name = "consent_given", nullable = false)
    private Boolean consentGiven = false;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdmissionLeadStatus status = AdmissionLeadStatus.NEW;

    @Column(name = "last_contact_at")
    private LocalDateTime lastContactAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = AdmissionLeadStatus.NEW;
        if (consentGiven == null) consentGiven = false;
        if (Boolean.TRUE.equals(consentGiven) && consentAt == null) consentAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (status == null) status = AdmissionLeadStatus.NEW;
        if (consentGiven == null) consentGiven = false;
        if (Boolean.TRUE.equals(consentGiven) && consentAt == null) consentAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Institution getInstitution() { return institution; }
    public AdmissionLead setInstitution(Institution institution) { this.institution = institution; return this; }
    public Course getDesiredCourse() { return desiredCourse; }
    public AdmissionLead setDesiredCourse(Course desiredCourse) { this.desiredCourse = desiredCourse; return this; }
    public String getFullName() { return fullName; }
    public AdmissionLead setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getPhone() { return phone; }
    public AdmissionLead setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public AdmissionLead setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getEmail() { return email; }
    public AdmissionLead setEmail(String email) { this.email = email; return this; }
    public String getDocumentNumber() { return documentNumber; }
    public AdmissionLead setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; return this; }
    public String getDesiredShift() { return desiredShift; }
    public AdmissionLead setDesiredShift(String desiredShift) { this.desiredShift = desiredShift; return this; }
    public String getProvince() { return province; }
    public AdmissionLead setProvince(String province) { this.province = province; return this; }
    public String getMunicipality() { return municipality; }
    public AdmissionLead setMunicipality(String municipality) { this.municipality = municipality; return this; }
    public String getLeadSource() { return leadSource; }
    public AdmissionLead setLeadSource(String leadSource) { this.leadSource = leadSource; return this; }
    public Boolean getConsentGiven() { return consentGiven; }
    public AdmissionLead setConsentGiven(Boolean consentGiven) { this.consentGiven = consentGiven; return this; }
    public LocalDateTime getConsentAt() { return consentAt; }
    public AdmissionLeadStatus getStatus() { return status; }
    public AdmissionLead setStatus(AdmissionLeadStatus status) { this.status = status; return this; }
    public LocalDateTime getLastContactAt() { return lastContactAt; }
    public AdmissionLead setLastContactAt(LocalDateTime lastContactAt) { this.lastContactAt = lastContactAt; return this; }
    public LocalDateTime getConvertedAt() { return convertedAt; }
    public AdmissionLead setConvertedAt(LocalDateTime convertedAt) { this.convertedAt = convertedAt; return this; }
    public String getNotes() { return notes; }
    public AdmissionLead setNotes(String notes) { this.notes = notes; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
