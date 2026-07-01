package com.secretariapay.api.dto.institution;

import com.secretariapay.api.entity.enums.institution.SubscriptionPlan;
import com.secretariapay.api.entity.enums.institution.SubscriptionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InstitutionSettingsRequest {

    @NotBlank(message = "O slug público é obrigatório.")
    @Size(max = 120, message = "O slug público deve ter no máximo 120 caracteres.")
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

    @Min(value = 0, message = "Os dias de tolerância não podem ser negativos.")
    @Max(value = 90, message = "Os dias de tolerância não podem passar de 90.")
    private Integer paymentGraceDays;

    @Min(value = 1, message = "O dia de vencimento deve ser entre 1 e 31.")
    @Max(value = 31, message = "O dia de vencimento deve ser entre 1 e 31.")
    private Integer monthlyDueDay;

    private Boolean active;

    public String getPublicSlug() {
        return publicSlug;
    }

    public InstitutionSettingsRequest setPublicSlug(String publicSlug) {
        this.publicSlug = publicSlug;
        return this;
    }

    public String getOfficialWhatsapp() {
        return officialWhatsapp;
    }

    public InstitutionSettingsRequest setOfficialWhatsapp(String officialWhatsapp) {
        this.officialWhatsapp = officialWhatsapp;
        return this;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public InstitutionSettingsRequest setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public String getTimezone() {
        return timezone;
    }

    public InstitutionSettingsRequest setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public InstitutionSettingsRequest setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public InstitutionSettingsRequest setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public InstitutionSettingsRequest setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
        return this;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public InstitutionSettingsRequest setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
        return this;
    }

    public String getAcademicPortalBaseUrl() {
        return academicPortalBaseUrl;
    }

    public InstitutionSettingsRequest setAcademicPortalBaseUrl(String academicPortalBaseUrl) {
        this.academicPortalBaseUrl = academicPortalBaseUrl;
        return this;
    }

    public Boolean getAllowAcademicBlocking() {
        return allowAcademicBlocking;
    }

    public InstitutionSettingsRequest setAllowAcademicBlocking(Boolean allowAcademicBlocking) {
        this.allowAcademicBlocking = allowAcademicBlocking;
        return this;
    }

    public Boolean getAutoUnblockAfterPayment() {
        return autoUnblockAfterPayment;
    }

    public InstitutionSettingsRequest setAutoUnblockAfterPayment(Boolean autoUnblockAfterPayment) {
        this.autoUnblockAfterPayment = autoUnblockAfterPayment;
        return this;
    }

    public Integer getPaymentGraceDays() {
        return paymentGraceDays;
    }

    public InstitutionSettingsRequest setPaymentGraceDays(Integer paymentGraceDays) {
        this.paymentGraceDays = paymentGraceDays;
        return this;
    }

    public Integer getMonthlyDueDay() {
        return monthlyDueDay;
    }

    public InstitutionSettingsRequest setMonthlyDueDay(Integer monthlyDueDay) {
        this.monthlyDueDay = monthlyDueDay;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public InstitutionSettingsRequest setActive(Boolean active) {
        this.active = active;
        return this;
    }
}
