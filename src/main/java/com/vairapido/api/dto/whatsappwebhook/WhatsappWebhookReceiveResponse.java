package com.vairapido.api.dto.whatsappwebhook;

import com.vairapido.api.entity.enums.WhatsappConversationStep;
import com.vairapido.api.entity.enums.WhatsappSessionType;

import java.time.LocalDateTime;
import java.util.UUID;

public class WhatsappWebhookReceiveResponse {

    private Boolean processed;
    private String reason;

    private String phoneNumber;
    private String messageText;
    private WhatsappSessionType sessionType;

    private UUID sessionId;
    private WhatsappConversationStep currentStep;

    private Boolean commandProcessed;
    private Boolean commandAllowed;
    private String commandName;
    private String replyMessage;

    private Boolean outboundEnabled;
    private Boolean outboundAttempted;
    private Boolean outboundSent;
    private String outboundPhoneNumber;
    private String outboundProviderMessageId;
    private String outboundErrorMessage;

    private LocalDateTime receivedAt;

    public Boolean getProcessed() {
        return processed;
    }

    public WhatsappWebhookReceiveResponse setProcessed(Boolean processed) {
        this.processed = processed;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public WhatsappWebhookReceiveResponse setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public WhatsappWebhookReceiveResponse setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getMessageText() {
        return messageText;
    }

    public WhatsappWebhookReceiveResponse setMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }

    public WhatsappSessionType getSessionType() {
        return sessionType;
    }

    public WhatsappWebhookReceiveResponse setSessionType(WhatsappSessionType sessionType) {
        this.sessionType = sessionType;
        return this;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public WhatsappWebhookReceiveResponse setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public WhatsappConversationStep getCurrentStep() {
        return currentStep;
    }

    public WhatsappWebhookReceiveResponse setCurrentStep(WhatsappConversationStep currentStep) {
        this.currentStep = currentStep;
        return this;
    }

    public Boolean getCommandProcessed() {
        return commandProcessed;
    }

    public WhatsappWebhookReceiveResponse setCommandProcessed(Boolean commandProcessed) {
        this.commandProcessed = commandProcessed;
        return this;
    }

    public Boolean getCommandAllowed() {
        return commandAllowed;
    }

    public WhatsappWebhookReceiveResponse setCommandAllowed(Boolean commandAllowed) {
        this.commandAllowed = commandAllowed;
        return this;
    }

    public String getCommandName() {
        return commandName;
    }

    public WhatsappWebhookReceiveResponse setCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public WhatsappWebhookReceiveResponse setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
        return this;
    }

    public Boolean getOutboundEnabled() {
        return outboundEnabled;
    }

    public WhatsappWebhookReceiveResponse setOutboundEnabled(Boolean outboundEnabled) {
        this.outboundEnabled = outboundEnabled;
        return this;
    }

    public Boolean getOutboundAttempted() {
        return outboundAttempted;
    }

    public WhatsappWebhookReceiveResponse setOutboundAttempted(Boolean outboundAttempted) {
        this.outboundAttempted = outboundAttempted;
        return this;
    }

    public Boolean getOutboundSent() {
        return outboundSent;
    }

    public WhatsappWebhookReceiveResponse setOutboundSent(Boolean outboundSent) {
        this.outboundSent = outboundSent;
        return this;
    }

    public String getOutboundPhoneNumber() {
        return outboundPhoneNumber;
    }

    public WhatsappWebhookReceiveResponse setOutboundPhoneNumber(String outboundPhoneNumber) {
        this.outboundPhoneNumber = outboundPhoneNumber;
        return this;
    }

    public String getOutboundProviderMessageId() {
        return outboundProviderMessageId;
    }

    public WhatsappWebhookReceiveResponse setOutboundProviderMessageId(String outboundProviderMessageId) {
        this.outboundProviderMessageId = outboundProviderMessageId;
        return this;
    }

    public String getOutboundErrorMessage() {
        return outboundErrorMessage;
    }

    public WhatsappWebhookReceiveResponse setOutboundErrorMessage(String outboundErrorMessage) {
        this.outboundErrorMessage = outboundErrorMessage;
        return this;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public WhatsappWebhookReceiveResponse setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
        return this;
    }
}