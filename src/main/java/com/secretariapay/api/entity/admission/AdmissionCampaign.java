package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.academic.Institution;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_campaigns")
public class AdmissionCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "campaign_code", nullable = false, unique = true, length = 80)
    private String campaignCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "registration_start", nullable = false)
    private LocalDate registrationStart;

    @Column(name = "registration_end", nullable = false)
    private LocalDate registrationEnd;

    @Column(name = "registration_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal registrationFee;

    @Column(name = "enrollment_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal enrollmentFee;

    @Column(name = "reenrollment_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal reenrollmentFee;

    @Column(nullable = false, length = 10)
    private String currency = "AOA";

    @Column(name = "public_form_enabled", nullable = false)
    private Boolean publicFormEnabled = true;

    @Column(name = "whatsapp_enabled", nullable = false)
    private Boolean whatsappEnabled = true;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (publicFormEnabled == null) publicFormEnabled = true;
        if (whatsappEnabled == null) whatsappEnabled = true;
        if (active == null) active = true;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (publicFormEnabled == null) publicFormEnabled = true;
        if (whatsappEnabled == null) whatsappEnabled = true;
        if (active == null) active = true;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Institution getInstitution() { return institution; }
    public AdmissionCampaign setInstitution(Institution institution) { this.institution = institution; return this; }
    public String getCampaignCode() { return campaignCode; }
    public AdmissionCampaign setCampaignCode(String campaignCode) { this.campaignCode = campaignCode; return this; }
    public String getName() { return name; }
    public AdmissionCampaign setName(String name) { this.name = name; return this; }
    public String getAcademicYear() { return academicYear; }
    public AdmissionCampaign setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public LocalDate getRegistrationStart() { return registrationStart; }
    public AdmissionCampaign setRegistrationStart(LocalDate registrationStart) { this.registrationStart = registrationStart; return this; }
    public LocalDate getRegistrationEnd() { return registrationEnd; }
    public AdmissionCampaign setRegistrationEnd(LocalDate registrationEnd) { this.registrationEnd = registrationEnd; return this; }
    public BigDecimal getRegistrationFee() { return registrationFee; }
    public AdmissionCampaign setRegistrationFee(BigDecimal registrationFee) { this.registrationFee = registrationFee; return this; }
    public BigDecimal getEnrollmentFee() { return enrollmentFee; }
    public AdmissionCampaign setEnrollmentFee(BigDecimal enrollmentFee) { this.enrollmentFee = enrollmentFee; return this; }
    public BigDecimal getReenrollmentFee() { return reenrollmentFee; }
    public AdmissionCampaign setReenrollmentFee(BigDecimal reenrollmentFee) { this.reenrollmentFee = reenrollmentFee; return this; }
    public String getCurrency() { return currency; }
    public AdmissionCampaign setCurrency(String currency) { this.currency = currency; return this; }
    public Boolean getPublicFormEnabled() { return publicFormEnabled; }
    public AdmissionCampaign setPublicFormEnabled(Boolean publicFormEnabled) { this.publicFormEnabled = publicFormEnabled; return this; }
    public Boolean getWhatsappEnabled() { return whatsappEnabled; }
    public AdmissionCampaign setWhatsappEnabled(Boolean whatsappEnabled) { this.whatsappEnabled = whatsappEnabled; return this; }
    public Boolean getActive() { return active; }
    public AdmissionCampaign setActive(Boolean active) { this.active = active; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
