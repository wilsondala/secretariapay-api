package com.secretariapay.api.dto.admin;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminUserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String whatsapp;
    private Boolean whatsappVerified;
    private UUID institutionId;
    private String institutionName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastWhatsappLoginAt;

    public static AdminUserResponse from(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.id = user.getId();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.status = user.getStatus();
        response.whatsapp = user.getWhatsapp();
        response.whatsappVerified = user.getWhatsappVerified();
        response.createdAt = user.getCreatedAt();
        response.updatedAt = user.getUpdatedAt();
        response.lastWhatsappLoginAt = user.getLastWhatsappLoginAt();
        if (user.getInstitution() != null) {
            response.institutionId = user.getInstitution().getId();
            response.institutionName = user.getInstitution().getName();
        }
        return response;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getWhatsapp() { return whatsapp; }
    public Boolean getWhatsappVerified() { return whatsappVerified; }
    public UUID getInstitutionId() { return institutionId; }
    public String getInstitutionName() { return institutionName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastWhatsappLoginAt() { return lastWhatsappLoginAt; }
}
