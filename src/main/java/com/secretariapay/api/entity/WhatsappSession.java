package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_sessions")
public class WhatsappSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WhatsappSessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WhatsappSessionStatus status = WhatsappSessionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private WhatsappConversationStep currentStep = WhatsappConversationStep.START;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @Column(columnDefinition = "TEXT")
    private String lastMessageText;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (expiresAt == null) {
            expiresAt = now.plusHours(24);
        }

        if (status == null) {
            status = WhatsappSessionStatus.ACTIVE;
        }

        if (currentStep == null) {
            currentStep = WhatsappConversationStep.START;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public WhatsappSession setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public WhatsappSession setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public WhatsappSessionType getSessionType() {
        return sessionType;
    }

    public WhatsappSession setSessionType(WhatsappSessionType sessionType) {
        this.sessionType = sessionType;
        return this;
    }

    public WhatsappSessionStatus getStatus() {
        return status;
    }

    public WhatsappSession setStatus(WhatsappSessionStatus status) {
        this.status = status;
        return this;
    }

    public WhatsappConversationStep getCurrentStep() {
        return currentStep;
    }

    public WhatsappSession setCurrentStep(WhatsappConversationStep currentStep) {
        this.currentStep = currentStep;
        return this;
    }

    public User getUser() {
        return user;
    }

    public WhatsappSession setUser(User user) {
        this.user = user;
        return this;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public WhatsappSession setPassenger(Passenger passenger) {
        this.passenger = passenger;
        return this;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public WhatsappSession setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public WhatsappSession setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public WhatsappSession setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WhatsappSession setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public WhatsappSession setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
