package com.secretariapay.api.dto.academic;

import java.time.LocalDateTime;
import java.util.UUID;

public class InstitutionResponse {
    private UUID id;
    private String name;
    private String legalName;
    private String nif;
    private String email;
    private String phone;
    private String whatsapp;
    private String address;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public InstitutionResponse setId(UUID id) { this.id = id; return this; }
    public String getName() { return name; }
    public InstitutionResponse setName(String name) { this.name = name; return this; }
    public String getLegalName() { return legalName; }
    public InstitutionResponse setLegalName(String legalName) { this.legalName = legalName; return this; }
    public String getNif() { return nif; }
    public InstitutionResponse setNif(String nif) { this.nif = nif; return this; }
    public String getEmail() { return email; }
    public InstitutionResponse setEmail(String email) { this.email = email; return this; }
    public String getPhone() { return phone; }
    public InstitutionResponse setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public InstitutionResponse setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getAddress() { return address; }
    public InstitutionResponse setAddress(String address) { this.address = address; return this; }
    public Boolean getActive() { return active; }
    public InstitutionResponse setActive(Boolean active) { this.active = active; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public InstitutionResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public InstitutionResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
