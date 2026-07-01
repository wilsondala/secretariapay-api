package com.secretariapay.api.service;

import com.secretariapay.api.dto.auth.AuthResponse;
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

        UserRole role = request.getRole() == null ? UserRole.ADMIN : request.getRole();

        User user = new User()
                .setFullName(request.getFullName())
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setRole(role)
                .setStatus(UserStatus.ACTIVE);

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
                .setCreatedAt(user.getCreatedAt())
                .setUpdatedAt(user.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
