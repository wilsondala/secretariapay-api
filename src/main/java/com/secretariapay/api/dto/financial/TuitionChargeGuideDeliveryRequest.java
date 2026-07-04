package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class TuitionChargeGuideDeliveryRequest {

    @NotNull(message = "A instituição é obrigatória.")
    private UUID institutionId;

    private String referenceMonth;

    private String status = "PENDING";

    private String chargeCodePrefix = "IMT-PROPINA-";

    private List<UUID> chargeIds;

    private Boolean onlyPending = true;

    private Boolean sendWhatsapp = true;

    private Boolean sendEmail = true;

    private Boolean sendSms = true;

    private Boolean forceResend = false;

    private Integer maxItems = 50;

    public UUID getInstitutionId() {
        return institutionId;
    }

    public TuitionChargeGuideDeliveryRequest setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public TuitionChargeGuideDeliveryRequest setReferenceMonth(String referenceMonth) {
        this.referenceMonth = referenceMonth;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public TuitionChargeGuideDeliveryRequest setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getChargeCodePrefix() {
        return chargeCodePrefix;
    }

    public TuitionChargeGuideDeliveryRequest setChargeCodePrefix(String chargeCodePrefix) {
        this.chargeCodePrefix = chargeCodePrefix;
        return this;
    }

    public List<UUID> getChargeIds() {
        return chargeIds;
    }

    public TuitionChargeGuideDeliveryRequest setChargeIds(List<UUID> chargeIds) {
        this.chargeIds = chargeIds;
        return this;
    }

    public Boolean getOnlyPending() {
        return onlyPending;
    }

    public TuitionChargeGuideDeliveryRequest setOnlyPending(Boolean onlyPending) {
        this.onlyPending = onlyPending;
        return this;
    }

    public Boolean getSendWhatsapp() {
        return sendWhatsapp;
    }

    public TuitionChargeGuideDeliveryRequest setSendWhatsapp(Boolean sendWhatsapp) {
        this.sendWhatsapp = sendWhatsapp;
        return this;
    }

    public Boolean getSendEmail() {
        return sendEmail;
    }

    public TuitionChargeGuideDeliveryRequest setSendEmail(Boolean sendEmail) {
        this.sendEmail = sendEmail;
        return this;
    }

    public Boolean getSendSms() {
        return sendSms;
    }

    public TuitionChargeGuideDeliveryRequest setSendSms(Boolean sendSms) {
        this.sendSms = sendSms;
        return this;
    }

    public Boolean getForceResend() {
        return forceResend;
    }

    public TuitionChargeGuideDeliveryRequest setForceResend(Boolean forceResend) {
        this.forceResend = forceResend;
        return this;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public TuitionChargeGuideDeliveryRequest setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }
}
