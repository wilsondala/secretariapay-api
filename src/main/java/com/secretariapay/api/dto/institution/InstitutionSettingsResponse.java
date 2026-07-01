package com.secretariapay.api.dto.institution;

import com.secretariapay.api.entity.enums.institution.SubscriptionPlan;
import com.secretariapay.api.entity.enums.institution.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class InstitutionSettingsResponse {

    private UUID id;
    private UUID institutionId;
    private String institutionName;
    private String publicSlug;
    private String officialWhatsapp;
    private String supportEmail;
    private String timezone;
    private String country;
    private String currency;
    private SubscriptionPlan subscriptionPlan;
    private SubscriptionStatus subscriptionStatus;
    private String academicPortalBaseUrl;
    private Boolean allowAcademicBlocking;
    private Boolean autoUnblockAfterPayment;
    private Integer paymentGraceDays;
    private Integer monthlyDueDay;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public InstitutionSettingsResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public InstitutionSettingsResponse setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public InstitutionSettingsResponse setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
        return this;
    }

    public String getPublicSlug() {
        return publicSlug;
    }

    public InstitutionSettingsResponse setPublicSlug(String publicSlug) {
        this.publicSlug = publicSlug;
        return this;
    }

    public String getOfficialWhatsapp() {
        return officialWhatsapp;
    }

    public InstitutionSettingsResponse setOfficialWhatsapp(String officialWhatsapp) {
        this.officialWhatsapp = officialWhatsapp;
        return this;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public InstitutionSettingsResponse setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public String getTimezone() {
        return timezone;
    }

    public InstitutionSettingsResponse setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public InstitutionSettingsResponse setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public InstitutionSettingsResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public InstitutionSettingsResponse setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
        return this;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public InstitutionSettingsResponse setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
        return this;
    }

    public String getAcademicPortalBaseUrl() {
        return academicPortalBaseUrl;
    }

    public InstitutionSettingsResponse setAcademicPortalBaseUrl(String academicPortalBaseUrl) {
        this.academicPortalBaseUrl = academicPortalBaseUrl;
        return this;
    }

    public Boolean getAllowAcademicBlocking() {
        return allowAcademicBlocking;
    }

    public InstitutionSettingsResponse setAllowAcademicBlocking(Boolean allowAcademicBlocking) {
        this.allowAcademicBlocking = allowAcademicBlocking;
        return this;
    }

    public Boolean getAutoUnblockAfterPayment() {
        return autoUnblockAfterPayment;
    }

    public InstitutionSettingsResponse setAutoUnblockAfterPayment(Boolean autoUnblockAfterPayment) {
        this.autoUnblockAfterPayment = autoUnblockAfterPayment;
        return this;
    }

    public Integer getPaymentGraceDays() {
        return paymentGraceDays;
    }

    public InstitutionSettingsResponse setPaymentGraceDays(Integer paymentGraceDays) {
        this.paymentGraceDays = paymentGraceDays;
        return this;
    }

    public Integer getMonthlyDueDay() {
        return monthlyDueDay;
    }

    public InstitutionSettingsResponse setMonthlyDueDay(Integer monthlyDueDay) {
        this.monthlyDueDay = monthlyDueDay;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public InstitutionSettingsResponse setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public InstitutionSettingsResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public InstitutionSettingsResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
