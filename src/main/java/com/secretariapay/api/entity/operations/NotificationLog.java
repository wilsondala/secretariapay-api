package com.secretariapay.api.entity.operations;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notification_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_once_per_day", columnNames = {"charge_id", "notification_type", "channel", "business_date"})
)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id")
    private Charge charge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "provider_message_id", length = 180)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Charge getCharge() { return charge; }
    public NotificationLog setCharge(Charge charge) { this.charge = charge; return this; }
    public Student getStudent() { return student; }
    public NotificationLog setStudent(Student student) { this.student = student; return this; }
    public String getNotificationType() { return notificationType; }
    public NotificationLog setNotificationType(String notificationType) { this.notificationType = notificationType; return this; }
    public String getChannel() { return channel; }
    public NotificationLog setChannel(String channel) { this.channel = channel; return this; }
    public String getStatus() { return status; }
    public NotificationLog setStatus(String status) { this.status = status; return this; }
    public LocalDate getBusinessDate() { return businessDate; }
    public NotificationLog setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; return this; }
    public String getMessage() { return message; }
    public NotificationLog setMessage(String message) { this.message = message; return this; }
    public String getProviderMessageId() { return providerMessageId; }
    public NotificationLog setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; return this; }
    public String getErrorMessage() { return errorMessage; }
    public NotificationLog setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
    public LocalDateTime getSentAt() { return sentAt; }
    public NotificationLog setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
