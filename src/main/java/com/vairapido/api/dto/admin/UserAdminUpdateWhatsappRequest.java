package com.vairapido.api.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class UserAdminUpdateWhatsappRequest {

    @NotBlank(message = "Número do WhatsApp é obrigatório.")
    private String whatsapp;

    private Boolean whatsappVerified = true;

    public String getWhatsapp() {
        return whatsapp;
    }

    public UserAdminUpdateWhatsappRequest setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public Boolean getWhatsappVerified() {
        return whatsappVerified;
    }

    public UserAdminUpdateWhatsappRequest setWhatsappVerified(Boolean whatsappVerified) {
        this.whatsappVerified = whatsappVerified;
        return this;
    }
}