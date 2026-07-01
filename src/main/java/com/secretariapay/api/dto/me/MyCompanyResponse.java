package com.secretariapay.api.dto.me;

import com.secretariapay.api.entity.enums.CompanyStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class MyCompanyResponse {

    private UUID id;
    private String name;
    private String tradeName;
    private String documentNumber;
    private String email;
    private String phone;
    private String whatsapp;
    private String logoUrl;
    private CompanyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public MyCompanyResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public MyCompanyResponse setName(String name) {
        this.name = name;
        return this;
    }

    public String getTradeName() {
        return tradeName;
    }

    public MyCompanyResponse setTradeName(String tradeName) {
        this.tradeName = tradeName;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public MyCompanyResponse setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public MyCompanyResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public MyCompanyResponse setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public MyCompanyResponse setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public MyCompanyResponse setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public MyCompanyResponse setStatus(CompanyStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public MyCompanyResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public MyCompanyResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
