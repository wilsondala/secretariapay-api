package com.vairapido.api.dto.whatsapp;

public class WhatsAppBulkSendRequest {

    private String confirmText;
    private Boolean dryRun = true;
    private String onlyToPhone;
    private Integer limit = 1;

    public String getConfirmText() {
        return confirmText;
    }

    public WhatsAppBulkSendRequest setConfirmText(String confirmText) {
        this.confirmText = confirmText;
        return this;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public WhatsAppBulkSendRequest setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public String getOnlyToPhone() {
        return onlyToPhone;
    }

    public WhatsAppBulkSendRequest setOnlyToPhone(String onlyToPhone) {
        this.onlyToPhone = onlyToPhone;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public WhatsAppBulkSendRequest setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
}
