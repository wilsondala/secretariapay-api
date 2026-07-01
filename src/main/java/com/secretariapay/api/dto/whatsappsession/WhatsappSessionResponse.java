package com.secretariapay.api.dto.whatsappsession;

import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;

import java.time.LocalDateTime;
import java.util.UUID;

public class WhatsappSessionResponse {

    private UUID id;
    private String phoneNumber;
    private WhatsappSessionType sessionType;
    private WhatsappSessionStatus status;
    private WhatsappConversationStep currentStep;

    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userRole;

    private UUID passengerId;
    private String passengerFullName;
    private String passengerDocumentNumber;

    private String lastMessageText;
    private String metadata;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public WhatsappSessionResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public WhatsappSessionResponse setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public WhatsappSessionType getSessionType() {
        return sessionType;
    }

    public WhatsappSessionResponse setSessionType(WhatsappSessionType sessionType) {
        this.sessionType = sessionType;
        return this;
    }

    public WhatsappSessionStatus getStatus() {
        return status;
    }

    public WhatsappSessionResponse setStatus(WhatsappSessionStatus status) {
        this.status = status;
        return this;
    }

    public WhatsappConversationStep getCurrentStep() {
        return currentStep;
    }

    public WhatsappSessionResponse setCurrentStep(WhatsappConversationStep currentStep) {
        this.currentStep = currentStep;
        return this;
    }

    public UUID getUserId() {
        return userId;
    }

    public WhatsappSessionResponse setUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public WhatsappSessionResponse setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public WhatsappSessionResponse setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserRole() {
        return userRole;
    }

    public WhatsappSessionResponse setUserRole(String userRole) {
        this.userRole = userRole;
        return this;
    }

    public UUID getPassengerId() {
        return passengerId;
    }

    public WhatsappSessionResponse setPassengerId(UUID passengerId) {
        this.passengerId = passengerId;
        return this;
    }

    public String getPassengerFullName() {
        return passengerFullName;
    }

    public WhatsappSessionResponse setPassengerFullName(String passengerFullName) {
        this.passengerFullName = passengerFullName;
        return this;
    }

    public String getPassengerDocumentNumber() {
        return passengerDocumentNumber;
    }

    public WhatsappSessionResponse setPassengerDocumentNumber(String passengerDocumentNumber) {
        this.passengerDocumentNumber = passengerDocumentNumber;
        return this;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public WhatsappSessionResponse setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public WhatsappSessionResponse setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public WhatsappSessionResponse setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WhatsappSessionResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public WhatsappSessionResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
