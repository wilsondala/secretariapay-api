package com.secretariapay.api.service.notification;

import java.util.UUID;

public class NotificationDispatchResult {

    private UUID messageId;
    private String channel;
    private String status;
    private String recipient;
    private String providerMessageId;
    private String failureReason;

    public UUID getMessageId() { return messageId; }
    public NotificationDispatchResult setMessageId(UUID messageId) { this.messageId = messageId; return this; }
    public String getChannel() { return channel; }
    public NotificationDispatchResult setChannel(String channel) { this.channel = channel; return this; }
    public String getStatus() { return status; }
    public NotificationDispatchResult setStatus(String status) { this.status = status; return this; }
    public String getRecipient() { return recipient; }
    public NotificationDispatchResult setRecipient(String recipient) { this.recipient = recipient; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public NotificationDispatchResult setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getFailureReason() { return failureReason; }
    public NotificationDispatchResult setFailureReason(String failureReason) { this.failureReason = failureReason; return this; }
}
