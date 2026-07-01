package com.secretariapay.api.entity.academic;

import com.secretariapay.api.entity.enums.institution.SubscriptionPlan;
import com.secretariapay.api.entity.enums.institution.SubscriptionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institution_settings")
public class InstitutionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false, unique = true)
    private Institution institution;

    @Column(name = "public_slug", nullable = false, unique = true, length = 120)
    private String publicSlug;

    @Column(name = "official_whatsapp", length = 40)
    private String officialWhatsapp;

    @Column(name = "support_email", length = 180)
    private String supportEmail;

    @Column(length = 80)
    private String timezone = "Africa/Luanda";

    @Column(length = 5)
    private String country = "AO";

    @Column(length = 10)
    private String currency = "AOA";

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 30)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.PILOT;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 30)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "academic_portal_base_url", columnDefinition = "TEXT")
    private String academicPortalBaseUrl;

    @Column(name = "allow_academic_blocking", nullable = false)
    private Boolean allowAcademicBlocking = false;

    @Column(name = "auto_unblock_after_payment", nullable = false)
    private Boolean autoUnblockAfterPayment = true;

    @Column(name = "payment_grace_days", nullable = false)
    private Integer paymentGraceDays = 5;

    @Column(name = "monthly_due_day", nullable = false)
    private Integer monthlyDueDay = 10;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (timezone == null || timezone.isBlank()) {
            timezone = "Africa/Luanda";
        }

        if (country == null || country.isBlank()) {
            country = "AO";
        }

        if (currency == null || currency.isBlank()) {
            currency = "AOA";
        }

        if (subscriptionPlan == null) {
            subscriptionPlan = SubscriptionPlan.PILOT;
        }

        if (subscriptionStatus == null) {
            subscriptionStatus = SubscriptionStatus.TRIAL;
        }

        if (allowAcademicBlocking == null) {
            allowAcademicBlocking = false;
        }

        if (autoUnblockAfterPayment == null) {
            autoUnblockAfterPayment = true;
        }

        if (paymentGraceDays == null) {
            paymentGraceDays = 5;
        }

        if (monthlyDueDay == null) {
            monthlyDueDay = 10;
        }

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (timezone == null || timezone.isBlank()) {
            timezone = "Africa/Luanda";
        }

        if (country == null || country.isBlank()) {
            country = "AO";
        }

        if (currency == null || currency.isBlank()) {
            currency = "AOA";
        }

        if (allowAcademicBlocking == null) {
            allowAcademicBlocking = false;
        }

        if (autoUnblockAfterPayment == null) {
            autoUnblockAfterPayment = true;
        }

        if (paymentGraceDays == null) {
            paymentGraceDays = 5;
        }

        if (monthlyDueDay == null) {
            monthlyDueDay = 10;
        }

        if (active == null) {
            active = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public InstitutionSettings setInstitution(Institution institution) {
        this.institution = institution;
        return this;
    }

    public String getPublicSlug() {
        return publicSlug;
    }

    public InstitutionSettings setPublicSlug(String publicSlug) {
        this.publicSlug = publicSlug;
        return this;
    }

    public String getOfficialWhatsapp() {
        return officialWhatsapp;
    }

    public InstitutionSettings setOfficialWhatsapp(String officialWhatsapp) {
        this.officialWhatsapp = officialWhatsapp;
        return this;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public InstitutionSettings setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public String getTimezone() {
        return timezone;
    }

    public InstitutionSettings setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public InstitutionSettings setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public InstitutionSettings setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public InstitutionSettings setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
        return this;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public InstitutionSettings setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
        return this;
    }

    public String getAcademicPortalBaseUrl() {
        return academicPortalBaseUrl;
    }

    public InstitutionSettings setAcademicPortalBaseUrl(String academicPortalBaseUrl) {
        this.academicPortalBaseUrl = academicPortalBaseUrl;
        return this;
    }

    public Boolean getAllowAcademicBlocking() {
        return allowAcademicBlocking;
    }

    public InstitutionSettings setAllowAcademicBlocking(Boolean allowAcademicBlocking) {
        this.allowAcademicBlocking = allowAcademicBlocking;
        return this;
    }

    public Boolean getAutoUnblockAfterPayment() {
        return autoUnblockAfterPayment;
    }

    public InstitutionSettings setAutoUnblockAfterPayment(Boolean autoUnblockAfterPayment) {
        this.autoUnblockAfterPayment = autoUnblockAfterPayment;
        return this;
    }

    public Integer getPaymentGraceDays() {
        return paymentGraceDays;
    }

    public InstitutionSettings setPaymentGraceDays(Integer paymentGraceDays) {
        this.paymentGraceDays = paymentGraceDays;
        return this;
    }

    public Integer getMonthlyDueDay() {
        return monthlyDueDay;
    }

    public InstitutionSettings setMonthlyDueDay(Integer monthlyDueDay) {
        this.monthlyDueDay = monthlyDueDay;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public InstitutionSettings setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
