package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "admission_operational_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_admission_operational_notification_idempotency",
                columnNames = "idempotency_key"
        )
)
public class AdmissionOperationalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(nullable = false, length = 60)
    private String recipient;

    @Column(name = "message_body", nullable = false, columnDefinition = "text")
    private String messageBody;

    @Column(name = "idempotency_key", nullable = false, length = 220)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdmissionNotificationStatus status = AdmissionNotificationStatus.PENDING;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "provider_message_id", length = 220)
    private String providerMessageId;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = AdmissionNotificationStatus.PENDING;
        if (attempts == null) attempts = 0;
        if (nextAttemptAt == null) nextAttemptAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (status == null) status = AdmissionNotificationStatus.PENDING;
        if (attempts == null) attempts = 0;
        if (nextAttemptAt == null) nextAttemptAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AdmissionApplication getApplication() { return application; }
    public AdmissionOperationalNotification setApplication(AdmissionApplication application) { this.application = application; return this; }
    public String getEventType() { return eventType; }
    public AdmissionOperationalNotification setEventType(String eventType) { this.eventType = eventType; return this; }
    public String getChannel() { return channel; }
    public AdmissionOperationalNotification setChannel(String channel) { this.channel = channel; return this; }
    public String getRecipient() { return recipient; }
    public AdmissionOperationalNotification setRecipient(String recipient) { this.recipient = recipient; return this; }
    public String getMessageBody() { return messageBody; }
    public AdmissionOperationalNotification setMessageBody(String messageBody) { this.messageBody = messageBody; return this; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public AdmissionOperationalNotification setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
    public AdmissionNotificationStatus getStatus() { return status; }
    public AdmissionOperationalNotification setStatus(AdmissionNotificationStatus status) { this.status = status; return this; }
    public Integer getAttempts() { return attempts; }
    public AdmissionOperationalNotification setAttempts(Integer attempts) { this.attempts = attempts; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public AdmissionOperationalNotification setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getLastError() { return lastError; }
    public AdmissionOperationalNotification setLastError(String lastError) { this.lastError = lastError; return this; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public AdmissionOperationalNotification setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; return this; }
    public LocalDateTime getSentAt() { return sentAt; }
    public AdmissionOperationalNotification setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
