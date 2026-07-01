package com.secretariapay.api.dto.admin;

import jakarta.validation.constraints.NotNull;

public class UserAdminUpdateStatusRequest {

    @NotNull(message = "Status ativo/inativo é obrigatório.")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public UserAdminUpdateStatusRequest setActive(Boolean active) {
        this.active = active;
        return this;
    }
}
