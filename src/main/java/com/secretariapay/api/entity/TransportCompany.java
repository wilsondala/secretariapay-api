package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.CompanyStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transport_companies")
public class TransportCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "trade_name", length = 160)
    private String tradeName;

    @Column(name = "document_number", nullable = false, unique = true, length = 30)
    private String documentNumber;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = CompanyStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public TransportCompany setName(String name) {
        this.name = name;
        return this;
    }

    public String getTradeName() {
        return tradeName;
    }

    public TransportCompany setTradeName(String tradeName) {
        this.tradeName = tradeName;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public TransportCompany setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TransportCompany setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public TransportCompany setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public TransportCompany setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public TransportCompany setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public TransportCompany setStatus(CompanyStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
