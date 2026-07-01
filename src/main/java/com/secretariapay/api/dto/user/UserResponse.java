package com.secretariapay.api.dto.user;

import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public UserResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public UserResponse setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public UserResponse setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserResponse setStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UserResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
