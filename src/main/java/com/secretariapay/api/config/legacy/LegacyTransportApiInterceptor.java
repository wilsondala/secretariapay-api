package com.secretariapay.api.config.legacy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
public class LegacyTransportApiInterceptor implements HandlerInterceptor {

    private final LegacyTransportApiProperties properties;

    public LegacyTransportApiInterceptor(LegacyTransportApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (properties.isEnabled()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_GONE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(buildDisabledResponse(request));
        return false;
    }

    private String buildDisabledResponse(HttpServletRequest request) {
        String path = request == null ? null : request.getRequestURI();
        String timestamp = OffsetDateTime.now().toString();

        return "{"
                + "\"status\":410,"
                + "\"error\":\"LEGACY_TRANSPORT_API_DISABLED\","
                + "\"message\":\"API legada de transporte/passagens desativada. Use os módulos SecretáriaPay académico, financeiro, recibos, comprovativos e message-dispatch.\","
                + "\"path\":\"" + escapeJson(path) + "\","
                + "\"timestamp\":\"" + escapeJson(timestamp) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
