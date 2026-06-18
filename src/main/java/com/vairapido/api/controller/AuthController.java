package com.vairapido.api.controller;

import com.vairapido.api.dto.auth.AuthResponse;
import com.vairapido.api.dto.auth.LoginRequest;
import com.vairapido.api.dto.auth.RegisterRequest;
import com.vairapido.api.dto.user.UserResponse;
import com.vairapido.api.service.AuthService;
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