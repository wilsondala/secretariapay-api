package com.secretariapay.api.dto.auth;

import com.secretariapay.api.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "O nome completo é obrigatório.")
    @Size(max = 160, message = "O nome deve ter no máximo 160 caracteres.")
    private String fullName;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 80, message = "A senha deve ter entre 6 e 80 caracteres.")
    private String password;

    private UserRole role;

    public String getFullName() {
        return fullName;
    }

    public RegisterRequest setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public RegisterRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RegisterRequest setPassword(String password) {
        this.password = password;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public RegisterRequest setRole(UserRole role) {
        this.role = role;
        return this;
    }
}
