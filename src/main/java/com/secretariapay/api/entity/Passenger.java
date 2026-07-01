package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.PassengerDocumentType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private PassengerDocumentType documentType = PassengerDocumentType.CPF;

    @Column(name = "document_number", nullable = false, unique = true, length = 30)
    private String documentNumber;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (documentType == null) {
            documentType = PassengerDocumentType.CPF;
        }

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        if (documentType == null) {
            documentType = PassengerDocumentType.CPF;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Passenger setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public PassengerDocumentType getDocumentType() {
        return documentType;
    }

    public Passenger setDocumentType(PassengerDocumentType documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public Passenger setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Passenger setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public Passenger setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public Passenger setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Passenger setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
