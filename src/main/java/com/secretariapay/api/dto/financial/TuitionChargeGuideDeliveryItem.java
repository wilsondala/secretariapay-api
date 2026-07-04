package com.secretariapay.api.dto.financial;

import java.util.UUID;

public class TuitionChargeGuideDeliveryItem {

    private UUID chargeId;
    private String chargeCode;
    private String referenceMonth;
    private String chargeStatus;
    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private String whatsapp;
    private String phone;
    private String email;
    private String guideUrl;
    private String whatsappStatus;
    private UUID whatsappMessageId;
    private String whatsappFailureReason;
    private String emailStatus;
    private UUID emailMessageId;
    private String emailFailureReason;
    private String smsStatus;
    private UUID smsMessageId;
    private String smsFailureReason;
    private String finalStatus;
    private String action;

    public UUID getChargeId() { return chargeId; }
    public TuitionChargeGuideDeliveryItem setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public TuitionChargeGuideDeliveryItem setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public String getReferenceMonth() { return referenceMonth; }
    public TuitionChargeGuideDeliveryItem setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }
    public String getChargeStatus() { return chargeStatus; }
    public TuitionChargeGuideDeliveryItem setChargeStatus(String chargeStatus) { this.chargeStatus = chargeStatus; return this; }
    public UUID getStudentId() { return studentId; }
    public TuitionChargeGuideDeliveryItem setStudentId(UUID studentId) { this.studentId = studentId; return this; }
    public String getStudentNumber() { return studentNumber; }
    public TuitionChargeGuideDeliveryItem setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getStudentName() { return studentName; }
    public TuitionChargeGuideDeliveryItem setStudentName(String studentName) { this.studentName = studentName; return this; }
    public String getWhatsapp() { return whatsapp; }
    public TuitionChargeGuideDeliveryItem setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getPhone() { return phone; }
    public TuitionChargeGuideDeliveryItem setPhone(String phone) { this.phone = phone; return this; }
    public String getEmail() { return email; }
    public TuitionChargeGuideDeliveryItem setEmail(String email) { this.email = email; return this; }
    public String getGuideUrl() { return guideUrl; }
    public TuitionChargeGuideDeliveryItem setGuideUrl(String guideUrl) { this.guideUrl = guideUrl; return this; }
    public String getWhatsappStatus() { return whatsappStatus; }
    public TuitionChargeGuideDeliveryItem setWhatsappStatus(String whatsappStatus) { this.whatsappStatus = whatsappStatus; return this; }
    public UUID getWhatsappMessageId() { return whatsappMessageId; }
    public TuitionChargeGuideDeliveryItem setWhatsappMessageId(UUID whatsappMessageId) { this.whatsappMessageId = whatsappMessageId; return this; }
    public String getWhatsappFailureReason() { return whatsappFailureReason; }
    public TuitionChargeGuideDeliveryItem setWhatsappFailureReason(String whatsappFailureReason) { this.whatsappFailureReason = whatsappFailureReason; return this; }
    public String getEmailStatus() { return emailStatus; }
    public TuitionChargeGuideDeliveryItem setEmailStatus(String emailStatus) { this.emailStatus = emailStatus; return this; }
    public UUID getEmailMessageId() { return emailMessageId; }
    public TuitionChargeGuideDeliveryItem setEmailMessageId(UUID emailMessageId) { this.emailMessageId = emailMessageId; return this; }
    public String getEmailFailureReason() { return emailFailureReason; }
    public TuitionChargeGuideDeliveryItem setEmailFailureReason(String emailFailureReason) { this.emailFailureReason = emailFailureReason; return this; }
    public String getSmsStatus() { return smsStatus; }
    public TuitionChargeGuideDeliveryItem setSmsStatus(String smsStatus) { this.smsStatus = smsStatus; return this; }
    public UUID getSmsMessageId() { return smsMessageId; }
    public TuitionChargeGuideDeliveryItem setSmsMessageId(UUID smsMessageId) { this.smsMessageId = smsMessageId; return this; }
    public String getSmsFailureReason() { return smsFailureReason; }
    public TuitionChargeGuideDeliveryItem setSmsFailureReason(String smsFailureReason) { this.smsFailureReason = smsFailureReason; return this; }
    public String getFinalStatus() { return finalStatus; }
    public TuitionChargeGuideDeliveryItem setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; return this; }
    public String getAction() { return action; }
    public TuitionChargeGuideDeliveryItem setAction(String action) { this.action = action; return this; }
}
