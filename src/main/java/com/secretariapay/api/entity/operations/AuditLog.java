package com.secretariapay.api.entity.operations;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id", length = 120)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public AuditLog setActor(String actor) { this.actor = actor; return this; }
    public String getAction() { return action; }
    public AuditLog setAction(String action) { this.action = action; return this; }
    public String getEntityType() { return entityType; }
    public AuditLog setEntityType(String entityType) { this.entityType = entityType; return this; }
    public String getEntityId() { return entityId; }
    public AuditLog setEntityId(String entityId) { this.entityId = entityId; return this; }
    public String getDetails() { return details; }
    public AuditLog setDetails(String details) { this.details = details; return this; }
    public String getIpAddress() { return ipAddress; }
    public AuditLog setIpAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
    public String getUserAgent() { return userAgent; }
    public AuditLog setUserAgent(String userAgent) { this.userAgent = userAgent; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
