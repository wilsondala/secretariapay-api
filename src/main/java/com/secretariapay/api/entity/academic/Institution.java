package com.secretariapay.api.entity.academic;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "legal_name", length = 220)
    private String legalName;

    @Column(length = 40)
    private String nif;

    @Column(length = 180)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (active == null) {
            active = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Institution setName(String name) {
        this.name = name;
        return this;
    }

    public String getLegalName() {
        return legalName;
    }

    public Institution setLegalName(String legalName) {
        this.legalName = legalName;
        return this;
    }

    public String getNif() {
        return nif;
    }

    public Institution setNif(String nif) {
        this.nif = nif;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Institution setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public Institution setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public Institution setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Institution setAddress(String address) {
        this.address = address;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public Institution setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}