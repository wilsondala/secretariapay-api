package com.vairapido.api.dto.auth;

import com.vairapido.api.dto.user.UserResponse;

public class AuthResponse {

    private String token;
    private String tokenType;
    private Long expiresInMinutes;
    private UserResponse user;

    public String getToken() {
        return token;
    }

    public AuthResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public AuthResponse setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    public Long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public AuthResponse setExpiresInMinutes(Long expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
        return this;
    }

    public UserResponse getUser() {
        return user;
    }

    public AuthResponse setUser(UserResponse user) {
        this.user = user;
        return this;
    }
}