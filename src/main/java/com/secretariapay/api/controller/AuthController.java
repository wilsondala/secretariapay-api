package com.secretariapay.api.controller;

import com.secretariapay.api.dto.auth.AuthResponse;
import com.secretariapay.api.dto.auth.LoginRequest;
import com.secretariapay.api.dto.auth.RegisterRequest;
import com.secretariapay.api.dto.user.UserResponse;
import com.secretariapay.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return service.me(authentication.getName());
    }
}
