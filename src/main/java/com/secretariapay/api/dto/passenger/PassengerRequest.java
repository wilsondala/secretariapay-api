package com.secretariapay.api.dto.passenger;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PassengerRequest {

    @NotBlank(message = "O nome completo do passageiro é obrigatório.")
    @Size(max = 160, message = "O nome completo deve ter no máximo 160 caracteres.")
    private String fullName;

    @NotBlank(message = "O documento do passageiro é obrigatório.")
    @Size(max = 30, message = "O documento deve ter no máximo 30 caracteres.")
    private String documentNumber;

    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
    private String email;

    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    private String phone;

    @Size(max = 40, message = "O WhatsApp deve ter no máximo 40 caracteres.")
    private String whatsapp;

    @Past(message = "A data de nascimento deve estar no passado.")
    private LocalDate birthDate;

    public String getFullName() {
        return fullName;
    }

    public PassengerRequest setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public PassengerRequest setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PassengerRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public PassengerRequest setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public PassengerRequest setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public PassengerRequest setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }
}
