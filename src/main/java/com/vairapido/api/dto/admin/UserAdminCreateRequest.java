package com.vairapido.api.dto.admin;

import com.vairapido.api.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserAdminCreateRequest {

    @NotBlank(message = "Nome completo é obrigatório.")
    private String fullName;

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.")
    private String password;

    @NotNull(message = "Perfil do usuário é obrigatório.")
    private UserRole role;

    private UUID transportCompanyId;

    private Boolean active = true;

    public String getFullName() {
        return fullName;
    }

    public UserAdminCreateRequest setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserAdminCreateRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserAdminCreateRequest setPassword(String password) {
        this.password = password;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public UserAdminCreateRequest setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UUID getTransportCompanyId() {
        return transportCompanyId;
    }

    public UserAdminCreateRequest setTransportCompanyId(UUID transportCompanyId) {
        this.transportCompanyId = transportCompanyId;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public UserAdminCreateRequest setActive(Boolean active) {
        this.active = active;
        return this;
    }
}