package com.vairapido.api.dto.admin;

import com.vairapido.api.entity.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserAdminResponse {

    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private Boolean active;

    private String whatsapp;
    private Boolean whatsappVerified;
    private LocalDateTime lastWhatsappLoginAt;

    private UUID transportCompanyId;
    private String transportCompanyName;
    private String transportCompanyTradeName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public UserAdminResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserAdminResponse setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserAdminResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public UserAdminResponse setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public UserAdminResponse setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public UserAdminResponse setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public Boolean getWhatsappVerified() {
        return whatsappVerified;
    }

    public UserAdminResponse setWhatsappVerified(Boolean whatsappVerified) {
        this.whatsappVerified = whatsappVerified;
        return this;
    }

    public LocalDateTime getLastWhatsappLoginAt() {
        return lastWhatsappLoginAt;
    }

    public UserAdminResponse setLastWhatsappLoginAt(LocalDateTime lastWhatsappLoginAt) {
        this.lastWhatsappLoginAt = lastWhatsappLoginAt;
        return this;
    }

    public UUID getTransportCompanyId() {
        return transportCompanyId;
    }

    public UserAdminResponse setTransportCompanyId(UUID transportCompanyId) {
        this.transportCompanyId = transportCompanyId;
        return this;
    }

    public String getTransportCompanyName() {
        return transportCompanyName;
    }

    public UserAdminResponse setTransportCompanyName(String transportCompanyName) {
        this.transportCompanyName = transportCompanyName;
        return this;
    }

    public String getTransportCompanyTradeName() {
        return transportCompanyTradeName;
    }

    public UserAdminResponse setTransportCompanyTradeName(String transportCompanyTradeName) {
        this.transportCompanyTradeName = transportCompanyTradeName;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserAdminResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UserAdminResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}