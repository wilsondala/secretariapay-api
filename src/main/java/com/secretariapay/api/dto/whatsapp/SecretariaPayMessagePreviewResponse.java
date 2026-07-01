package com.secretariapay.api.dto.whatsapp;

import java.util.UUID;

public class SecretariaPayMessagePreviewResponse {

    private String type;
    private UUID institutionId;
    private String institutionName;
    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private String studentWhatsapp;
    private UUID chargeId;
    private String chargeCode;
    private UUID paymentProofId;
    private UUID receiptId;
    private String receiptCode;
    private String channel;
    private String language;
    private String message;

    public String getType() {
        return type;
    }

    public SecretariaPayMessagePreviewResponse setType(String type) {
        this.type = type;
        return this;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public SecretariaPayMessagePreviewResponse setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public SecretariaPayMessagePreviewResponse setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
        return this;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public SecretariaPayMessagePreviewResponse setStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public SecretariaPayMessagePreviewResponse setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
        return this;
    }

    public String getStudentName() {
        return studentName;
    }

    public SecretariaPayMessagePreviewResponse setStudentName(String studentName) {
        this.studentName = studentName;
        return this;
    }

    public String getStudentWhatsapp() {
        return studentWhatsapp;
    }

    public SecretariaPayMessagePreviewResponse setStudentWhatsapp(String studentWhatsapp) {
        this.studentWhatsapp = studentWhatsapp;
        return this;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public SecretariaPayMessagePreviewResponse setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public SecretariaPayMessagePreviewResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public UUID getPaymentProofId() {
        return paymentProofId;
    }

    public SecretariaPayMessagePreviewResponse setPaymentProofId(UUID paymentProofId) {
        this.paymentProofId = paymentProofId;
        return this;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public SecretariaPayMessagePreviewResponse setReceiptId(UUID receiptId) {
        this.receiptId = receiptId;
        return this;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public SecretariaPayMessagePreviewResponse setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
        return this;
    }

    public String getChannel() {
        return channel;
    }

    public SecretariaPayMessagePreviewResponse setChannel(String channel) {
        this.channel = channel;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public SecretariaPayMessagePreviewResponse setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public SecretariaPayMessagePreviewResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
