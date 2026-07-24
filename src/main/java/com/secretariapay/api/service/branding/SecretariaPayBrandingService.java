package com.secretariapay.api.service.branding;

import com.secretariapay.api.dto.branding.SecretariaPayBrandingResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class SecretariaPayBrandingService {

    public SecretariaPayBrandingResponse getBranding(HttpServletRequest request) {
        String baseUrl = resolveBaseUrl(request);

        return new SecretariaPayBrandingResponse()
                .setName("SecretáriaPay")
                .setProduct("SecretáriaPay Académico")
                .setDescription("Plataforma institucional de automação de propinas, cobranças, comprovativos, recibos digitais e atendimento académico via WhatsApp.")
                .setLogoUrl(baseUrl + "/branding/secretariapay-logo.png")
                .setPrimaryColor("#0B3B82")
                .setSecondaryColor("#16A34A")
                .setAccentColor("#D4AF37")
                .setCompany("SecretáriaPay Académico — IMETRO")
                .setPlatform("Academic Financial Automation")
                .setCountryFocus("Angola");
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");

        if (forwardedProto != null && !forwardedProto.isBlank()
                && forwardedHost != null && !forwardedHost.isBlank()) {
            return forwardedProto + "://" + forwardedHost;
        }

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        if (defaultPort) {
            return scheme + "://" + serverName;
        }

        return scheme + "://" + serverName + ":" + serverPort;
    }
}
