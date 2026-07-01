package com.secretariapay.api.dto.me;

import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class MeResponse {

    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private Boolean active;

    private UUID transportCompanyId;
    private String transportCompanyName;
    private String transportCompanyTradeName;

    private Boolean admin;
    private Boolean operator;
    private Boolean companyAdmin;
    private Boolean canAccessGlobalDashboard;
    private Boolean canAccessCompanyDashboard;
    private Boolean canBoardTickets;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public MeResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public MeResponse setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public MeResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public MeResponse setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserStatus getStatus() {
        return status;
    }

    public MeResponse setStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public MeResponse setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public UUID getTransportCompanyId() {
        return transportCompanyId;
    }

    public MeResponse setTransportCompanyId(UUID transportCompanyId) {
        this.transportCompanyId = transportCompanyId;
        return this;
    }

    public String getTransportCompanyName() {
        return transportCompanyName;
    }

    public MeResponse setTransportCompanyName(String transportCompanyName) {
        this.transportCompanyName = transportCompanyName;
        return this;
    }

    public String getTransportCompanyTradeName() {
        return transportCompanyTradeName;
    }

    public MeResponse setTransportCompanyTradeName(String transportCompanyTradeName) {
        this.transportCompanyTradeName = transportCompanyTradeName;
        return this;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public MeResponse setAdmin(Boolean admin) {
        this.admin = admin;
        return this;
    }

    public Boolean getOperator() {
        return operator;
    }

    public MeResponse setOperator(Boolean operator) {
        this.operator = operator;
        return this;
    }

    public Boolean getCompanyAdmin() {
        return companyAdmin;
    }

    public MeResponse setCompanyAdmin(Boolean companyAdmin) {
        this.companyAdmin = companyAdmin;
        return this;
    }

    public Boolean getCanAccessGlobalDashboard() {
        return canAccessGlobalDashboard;
    }

    public MeResponse setCanAccessGlobalDashboard(Boolean canAccessGlobalDashboard) {
        this.canAccessGlobalDashboard = canAccessGlobalDashboard;
        return this;
    }

    public Boolean getCanAccessCompanyDashboard() {
        return canAccessCompanyDashboard;
    }

    public MeResponse setCanAccessCompanyDashboard(Boolean canAccessCompanyDashboard) {
        this.canAccessCompanyDashboard = canAccessCompanyDashboard;
        return this;
    }

    public Boolean getCanBoardTickets() {
        return canBoardTickets;
    }

    public MeResponse setCanBoardTickets(Boolean canBoardTickets) {
        this.canBoardTickets = canBoardTickets;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public MeResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public MeResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
