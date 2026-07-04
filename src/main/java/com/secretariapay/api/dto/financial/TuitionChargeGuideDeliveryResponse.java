package com.secretariapay.api.dto.financial;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TuitionChargeGuideDeliveryResponse {

    private UUID institutionId;
    private String referenceMonth;
    private String chargeCodePrefix;
    private int selectedCharges;
    private int processedCharges;
    private int sentWhatsapp;
    private int failedWhatsapp;
    private int sentEmail;
    private int failedEmail;
    private int sentSms;
    private int failedSms;
    private int skippedNoContact;
    private int skippedNotPending;
    private int skippedAlreadySent;
    private int skippedByFilter;
    private String status;
    private String message;
    private LocalDateTime processedAt;
    private List<TuitionChargeGuideDeliveryItem> items = new ArrayList<>();

    public UUID getInstitutionId() { return institutionId; }
    public TuitionChargeGuideDeliveryResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getReferenceMonth() { return referenceMonth; }
    public TuitionChargeGuideDeliveryResponse setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }
    public String getChargeCodePrefix() { return chargeCodePrefix; }
    public TuitionChargeGuideDeliveryResponse setChargeCodePrefix(String chargeCodePrefix) { this.chargeCodePrefix = chargeCodePrefix; return this; }
    public int getSelectedCharges() { return selectedCharges; }
    public TuitionChargeGuideDeliveryResponse setSelectedCharges(int selectedCharges) { this.selectedCharges = selectedCharges; return this; }
    public int getProcessedCharges() { return processedCharges; }
    public TuitionChargeGuideDeliveryResponse setProcessedCharges(int processedCharges) { this.processedCharges = processedCharges; return this; }
    public int getSentWhatsapp() { return sentWhatsapp; }
    public TuitionChargeGuideDeliveryResponse setSentWhatsapp(int sentWhatsapp) { this.sentWhatsapp = sentWhatsapp; return this; }
    public int getFailedWhatsapp() { return failedWhatsapp; }
    public TuitionChargeGuideDeliveryResponse setFailedWhatsapp(int failedWhatsapp) { this.failedWhatsapp = failedWhatsapp; return this; }
    public int getSentEmail() { return sentEmail; }
    public TuitionChargeGuideDeliveryResponse setSentEmail(int sentEmail) { this.sentEmail = sentEmail; return this; }
    public int getFailedEmail() { return failedEmail; }
    public TuitionChargeGuideDeliveryResponse setFailedEmail(int failedEmail) { this.failedEmail = failedEmail; return this; }
    public int getSentSms() { return sentSms; }
    public TuitionChargeGuideDeliveryResponse setSentSms(int sentSms) { this.sentSms = sentSms; return this; }
    public int getFailedSms() { return failedSms; }
    public TuitionChargeGuideDeliveryResponse setFailedSms(int failedSms) { this.failedSms = failedSms; return this; }
    public int getSkippedNoContact() { return skippedNoContact; }
    public TuitionChargeGuideDeliveryResponse setSkippedNoContact(int skippedNoContact) { this.skippedNoContact = skippedNoContact; return this; }
    public int getSkippedNotPending() { return skippedNotPending; }
    public TuitionChargeGuideDeliveryResponse setSkippedNotPending(int skippedNotPending) { this.skippedNotPending = skippedNotPending; return this; }
    public int getSkippedAlreadySent() { return skippedAlreadySent; }
    public TuitionChargeGuideDeliveryResponse setSkippedAlreadySent(int skippedAlreadySent) { this.skippedAlreadySent = skippedAlreadySent; return this; }
    public int getSkippedByFilter() { return skippedByFilter; }
    public TuitionChargeGuideDeliveryResponse setSkippedByFilter(int skippedByFilter) { this.skippedByFilter = skippedByFilter; return this; }
    public String getStatus() { return status; }
    public TuitionChargeGuideDeliveryResponse setStatus(String status) { this.status = status; return this; }
    public String getMessage() { return message; }
    public TuitionChargeGuideDeliveryResponse setMessage(String message) { this.message = message; return this; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public TuitionChargeGuideDeliveryResponse setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
    public List<TuitionChargeGuideDeliveryItem> getItems() { return items; }
    public TuitionChargeGuideDeliveryResponse setItems(List<TuitionChargeGuideDeliveryItem> items) { this.items = items; return this; }
}
