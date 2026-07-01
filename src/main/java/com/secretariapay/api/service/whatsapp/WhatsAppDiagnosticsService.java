package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppDiagnosticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppDiagnosticsService {

    private final boolean enabled;
    private final String phoneNumberId;
    private final String accessToken;
    private final String graphApiVersion;
    private final String graphApiBaseUrl;

    public WhatsAppDiagnosticsService(
            @Value("${secretariapay.whatsapp.enabled:false}") boolean enabled,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken,
            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}") String graphApiVersion,
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl
    ) {
        this.enabled = enabled;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.graphApiVersion = graphApiVersion;
        this.graphApiBaseUrl = graphApiBaseUrl;
    }

    public WhatsAppDiagnosticsResponse getDiagnostics() {
        boolean phoneConfigured = hasText(phoneNumberId);
        boolean tokenConfigured = hasText(accessToken);
        boolean readyForRealSend = enabled && phoneConfigured && tokenConfigured;

        return new WhatsAppDiagnosticsResponse()
                .setEnabled(enabled)
                .setPhoneNumberIdConfigured(phoneConfigured)
                .setAccessTokenConfigured(tokenConfigured)
                .setGraphApiVersion(graphApiVersion)
                .setGraphApiBaseUrl(graphApiBaseUrl)
                .setMode(resolveMode(readyForRealSend))
                .setSafetyMessage(resolveSafetyMessage(readyForRealSend, phoneConfigured, tokenConfigured));
    }

    private String resolveMode(boolean readyForRealSend) {
        if (readyForRealSend) {
            return "REAL_SEND_READY";
        }

        if (enabled) {
            return "REAL_SEND_BLOCKED_BY_MISSING_CONFIG";
        }

        return "MOCK_SAFE";
    }

    private String resolveSafetyMessage(boolean readyForRealSend, boolean phoneConfigured, boolean tokenConfigured) {
        if (readyForRealSend) {
            return "WhatsApp Cloud API habilitado e credenciais básicas configuradas. Validar em ambiente controlado antes de enviar em massa.";
        }

        if (enabled && (!phoneConfigured || !tokenConfigured)) {
            return "WhatsApp real foi habilitado, mas phone-number-id ou access-token não foram configurados. O envio real deve ser bloqueado.";
        }

        return "Modo seguro/mock ativo. Nenhuma mensagem real será enviada pela Meta.";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
