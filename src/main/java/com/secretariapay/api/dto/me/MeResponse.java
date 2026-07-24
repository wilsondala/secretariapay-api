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
    private UUID institutionId;
    private String institutionName;
    private Boolean globalAdmin;
    private Boolean institutionAdmin;
    private Boolean canAccessFinancialDashboard;
    private Boolean canManageUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public MeResponse setId(UUID id) { this.id = id; return this; }
    public String getFullName() { return fullName; }
    public MeResponse setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getEmail() { return email; }
    public MeResponse setEmail(String email) { this.email = email; return this; }
    public UserRole getRole() { return role; }
    public MeResponse setRole(UserRole role) { this.role = role; return this; }
    public UserStatus getStatus() { return status; }
    public MeResponse setStatus(UserStatus status) { this.status = status; return this; }
    public Boolean getActive() { return active; }
    public MeResponse setActive(Boolean active) { this.active = active; return this; }
    public UUID getInstitutionId() { return institutionId; }
    public MeResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getInstitutionName() { return institutionName; }
    public MeResponse setInstitutionName(String institutionName) { this.institutionName = institutionName; return this; }
    public Boolean getGlobalAdmin() { return globalAdmin; }
    public MeResponse setGlobalAdmin(Boolean globalAdmin) { this.globalAdmin = globalAdmin; return this; }
    public Boolean getInstitutionAdmin() { return institutionAdmin; }
    public MeResponse setInstitutionAdmin(Boolean institutionAdmin) { this.institutionAdmin = institutionAdmin; return this; }
    public Boolean getCanAccessFinancialDashboard() { return canAccessFinancialDashboard; }
    public MeResponse setCanAccessFinancialDashboard(Boolean canAccessFinancialDashboard) { this.canAccessFinancialDashboard = canAccessFinancialDashboard; return this; }
    public Boolean getCanManageUsers() { return canManageUsers; }
    public MeResponse setCanManageUsers(Boolean canManageUsers) { this.canManageUsers = canManageUsers; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public MeResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public MeResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
