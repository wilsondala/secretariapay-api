package com.vairapido.api.dto.transportcompany;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TransportCompanyRequest {

    @NotBlank(message = "O nome da empresa é obrigatório.")
    @Size(max = 160, message = "O nome deve ter no máximo 160 caracteres.")
    private String name;

    @Size(max = 160, message = "O nome comercial deve ter no máximo 160 caracteres.")
    private String tradeName;

    @NotBlank(message = "O documento da empresa é obrigatório.")
    @Size(max = 30, message = "O documento deve ter no máximo 30 caracteres.")
    private String documentNumber;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
    private String email;

    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    private String phone;

    @Size(max = 40, message = "O WhatsApp deve ter no máximo 40 caracteres.")
    private String whatsapp;

    private String logoUrl;

    public String getName() {
        return name;
    }

    public TransportCompanyRequest setName(String name) {
        this.name = name;
        return this;
    }

    public String getTradeName() {
        return tradeName;
    }

    public TransportCompanyRequest setTradeName(String tradeName) {
        this.tradeName = tradeName;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public TransportCompanyRequest setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TransportCompanyRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public TransportCompanyRequest setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public TransportCompanyRequest setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public TransportCompanyRequest setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }
}