package com.secretariapay.api.service;

import com.secretariapay.api.dto.auth.AuthResponse;
import com.secretariapay.api.dto.auth.ChangePasswordRequest;
import com.secretariapay.api.dto.auth.LoginRequest;
import com.secretariapay.api.dto.auth.RegisterRequest;
import com.secretariapay.api.dto.user.UserResponse;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        UserRole role = request.getRole() == null ? UserRole.ADMIN_GLOBAL : request.getRole();

        User user = new User()
                .setFullName(request.getFullName())
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setRole(role)
                .setStatus(UserStatus.ACTIVE)
                .setMustChangePassword(false)
                .setPasswordChangedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuário ou senha inválidos."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Usuário não está ativo.");
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new NotFoundException("Usuário ou senha inválidos.");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuário autenticado não encontrado."));

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("Usuário autenticado não encontrado."));

        validatePasswordChange(request);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A palavra-passe atual está incorreta.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A nova palavra-passe deve ser diferente da palavra-passe atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()))
                .setMustChangePassword(false)
                .setPasswordChangedAt(LocalDateTime.now());

        return toUserResponse(userRepository.save(user));
    }

    private void validatePasswordChange(ChangePasswordRequest request) {
        if (request == null
                || request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("A palavra-passe atual é obrigatória.");
        }

        String newPassword = request.getNewPassword();

        if (newPassword == null || newPassword.length() < 10) {
            throw new IllegalArgumentException("A nova palavra-passe deve ter pelo menos 10 caracteres.");
        }

        if (newPassword.length() > 128) {
            throw new IllegalArgumentException("A nova palavra-passe não pode exceder 128 caracteres.");
        }

        if (!newPassword.equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("A confirmação da nova palavra-passe não corresponde.");
        }

        boolean hasUppercase = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars().anyMatch(value -> !Character.isLetterOrDigit(value));

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                    "A nova palavra-passe deve conter letra maiúscula, letra minúscula, número e símbolo."
            );
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);

        return new AuthResponse()
                .setToken(token)
                .setTokenType("Bearer")
                .setExpiresInMinutes(jwtService.getJwtExpirationMinutes())
                .setUser(toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse()
                .setId(user.getId())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setRole(user.getRole())
                .setStatus(user.getStatus())
                .setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .setPasswordChangedAt(user.getPasswordChangedAt())
                .setCreatedAt(user.getCreatedAt())
                .setUpdatedAt(user.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
