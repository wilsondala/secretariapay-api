package com.secretariapay.api.entity;

import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRole role = UserRole.OPERADOR_ATENDIMENTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 40)
    private String whatsapp;

    @Column(nullable = false)
    private Boolean whatsappVerified = false;

    private LocalDateTime lastWhatsappLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (role == null) {
            role = UserRole.OPERADOR_ATENDIMENTO;
        }

        if (status == null) {
            status = UserStatus.ACTIVE;
        }

        if (whatsappVerified == null) {
            whatsappVerified = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (whatsappVerified == null) {
            whatsappVerified = false;
        }

        if (role == null) {
            role = UserRole.OPERADOR_ATENDIMENTO;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }

        return List.of(
                new SimpleGrantedAuthority(role.name()),
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    public boolean isGlobalAdmin() {
        return UserRole.ADMIN_GLOBAL.equals(role);
    }

    public boolean belongsToInstitution(UUID institutionId) {
        return institution != null
                && institution.getId() != null
                && institution.getId().equals(institutionId);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserStatus.ACTIVE.equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserStatus.ACTIVE.equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserStatus.ACTIVE.equals(status);
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ACTIVE.equals(status);
    }

    public UUID getId() {
        return id;
    }

    public User setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public User setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public User setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public User setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserStatus getStatus() {
        return status;
    }

    public User setStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public User setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public Boolean getWhatsappVerified() {
        return whatsappVerified;
    }

    public User setWhatsappVerified(Boolean whatsappVerified) {
        this.whatsappVerified = whatsappVerified;
        return this;
    }

    public LocalDateTime getLastWhatsappLoginAt() {
        return lastWhatsappLoginAt;
    }

    public User setLastWhatsappLoginAt(LocalDateTime lastWhatsappLoginAt) {
        this.lastWhatsappLoginAt = lastWhatsappLoginAt;
        return this;
    }

    public Institution getInstitution() {
        return institution;
    }

    public User setInstitution(Institution institution) {
        this.institution = institution;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
