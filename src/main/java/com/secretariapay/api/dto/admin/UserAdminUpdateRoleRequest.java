package com.secretariapay.api.dto.admin;

import com.secretariapay.api.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class UserAdminUpdateRoleRequest {

    @NotNull(message = "Perfil do usuário é obrigatório.")
    private UserRole role;

    public UserRole getRole() {
        return role;
    }

    public UserAdminUpdateRoleRequest setRole(UserRole role) {
        this.role = role;
        return this;
    }
}
