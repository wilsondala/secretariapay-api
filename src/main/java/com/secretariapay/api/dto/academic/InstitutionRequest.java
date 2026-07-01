package com.secretariapay.api.dto.academic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InstitutionRequest {

    @NotBlank(message = "Nome da instituição é obrigatório.")
    @Size(max = 180)
    private String name;

    @Size(max = 220)
    private String legalName;

    @Size(max = 40)
    private String nif;

    @Email(message = "E-mail inválido.")
    @Size(max = 180)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 40)
    private String whatsapp;

    private String address;

    private Boolean active;

    public String getName() { return name; }
    public InstitutionRequest setName(String name) { this.name = name; return this; }
    public String getLegalName() { return legalName; }
    public InstitutionRequest setLegalName(String legalName) { this.legalName = legalName; return this; }
    public String getNif() { return nif; }
    public InstitutionRequest setNif(String nif) { this.nif = nif; return this; }
    public String getEmail() { return email; }
    public InstitutionRequest setEmail(String email) { this.email = email; return this; }
    public String getPhone() { return phone; }
    public InstitutionRequest setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public InstitutionRequest setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getAddress() { return address; }
    public InstitutionRequest setAddress(String address) { this.address = address; return this; }
    public Boolean getActive() { return active; }
    public InstitutionRequest setActive(Boolean active) { this.active = active; return this; }
}
