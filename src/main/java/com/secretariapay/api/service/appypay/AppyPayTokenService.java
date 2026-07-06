package com.secretariapay.api.service.appypay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class AppyPayTokenService {

    private final RestClient restClient;
    private final String authUrl;
    private final String grantType;
    private final String clientId;
    private final String clientPassword;
    private final String resource;
    private volatile String token;
    private volatile Instant expiresAt = Instant.EPOCH;

    public AppyPayTokenService(
            RestClient.Builder restClientBuilder,
            @Value("${secretariapay.appypay.auth-url:}") String authUrl,
            @Value("${secretariapay.appypay.grant-type:client_credentials}") String grantType,
            @Value("${secretariapay.appypay.client-id:}") String clientId,
            @Value("${secretariapay.appypay.client-password:}") String clientPassword,
            @Value("${secretariapay.appypay.resource:}") String resource
    ) {
        this.restClient = restClientBuilder.build();
        this.authUrl = authUrl == null ? "" : authUrl.trim();
        this.grantType = grantType == null || grantType.isBlank() ? "client_credentials" : grantType.trim();
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientPassword = clientPassword == null ? "" : clientPassword.trim();
        this.resource = resource == null ? "" : resource.trim();
    }

    public synchronized String getToken() {
        if (token != null && !token.isBlank() && expiresAt.isAfter(Instant.now().plusSeconds(60))) return token;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);
        form.add("client_id", clientId);
        form.add("client_secret", clientPassword);
        form.add("resource", resource);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(authUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(Map.class);

        Object tokenValue = response == null ? null : response.get("access_token");
        if (tokenValue == null || tokenValue.toString().isBlank()) throw new IllegalArgumentException("AppyPay não retornou token.");

        long expiresIn = 3600L;
        Object expires = response.get("expires_in");
        if (expires != null) {
            try { expiresIn = Long.parseLong(expires.toString()); } catch (NumberFormatException ignored) { expiresIn = 3600L; }
        }

        token = tokenValue.toString();
        expiresAt = Instant.now().plusSeconds(Math.max(300L, expiresIn - 300L));
        return token;
    }
}
