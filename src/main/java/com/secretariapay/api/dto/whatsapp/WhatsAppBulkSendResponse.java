package com.secretariapay.api.dto.whatsapp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WhatsAppBulkSendResponse {

    private Boolean dryRun;
    private Integer totalPending;
    private Integer eligible;
    private Integer sent;
    private Integer failed;
    private Integer skipped;
    private Integer limitApplied;
    private String onlyToPhone;
    private String message;
    private LocalDateTime processedAt;
    private List<WhatsAppMessageResponse> messages = new ArrayList<>();

    public Boolean getDryRun() {
        return dryRun;
    }

    public WhatsAppBulkSendResponse setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public Integer getTotalPending() {
        return totalPending;
    }

    public WhatsAppBulkSendResponse setTotalPending(Integer totalPending) {
        this.totalPending = totalPending;
        return this;
    }

    public Integer getEligible() {
        return eligible;
    }

    public WhatsAppBulkSendResponse setEligible(Integer eligible) {
        this.eligible = eligible;
        return this;
    }

    public Integer getSent() {
        return sent;
    }

    public WhatsAppBulkSendResponse setSent(Integer sent) {
        this.sent = sent;
        return this;
    }

    public Integer getFailed() {
        return failed;
    }

    public WhatsAppBulkSendResponse setFailed(Integer failed) {
        this.failed = failed;
        return this;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public WhatsAppBulkSendResponse setSkipped(Integer skipped) {
        this.skipped = skipped;
        return this;
    }

    public Integer getLimitApplied() {
        return limitApplied;
    }

    public WhatsAppBulkSendResponse setLimitApplied(Integer limitApplied) {
        this.limitApplied = limitApplied;
        return this;
    }

    public String getOnlyToPhone() {
        return onlyToPhone;
    }

    public WhatsAppBulkSendResponse setOnlyToPhone(String onlyToPhone) {
        this.onlyToPhone = onlyToPhone;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public WhatsAppBulkSendResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public WhatsAppBulkSendResponse setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        return this;
    }

    public List<WhatsAppMessageResponse> getMessages() {
        return messages;
    }

    public WhatsAppBulkSendResponse setMessages(List<WhatsAppMessageResponse> messages) {
        this.messages = messages;
        return this;
    }
}

