package com.vairapido.api.dto.passenger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PassengerResponse {

    private UUID id;
    private String fullName;
    private String documentNumber;
    private String email;
    private String phone;
    private String whatsapp;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public PassengerResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public PassengerResponse setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public PassengerResponse setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PassengerResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public PassengerResponse setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public PassengerResponse setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public PassengerResponse setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PassengerResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PassengerResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}