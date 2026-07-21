package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@ConditionalOnProperty(
        name = "secretariapay.whatsapp.validation-mock-enabled",
        havingValue = "true"
)
public class WhatsAppValidationMockAspect {

    private final String graphApiBaseUrl;

    public WhatsAppValidationMockAspect(
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl
    ) {
        this.graphApiBaseUrl = stripTrailingSlash(graphApiBaseUrl);
    }

    @Around("execution(* com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient.sendText(..))")
    public Object mockTextSend(ProceedingJoinPoint joinPoint) {
        if (!isLocalEndpoint()) {
            return WhatsAppCloudSendResult.failed(
                    "Modo de validação do WhatsApp recusado: configure graph-api-base-url como localhost ou 127.0.0.1.",
                    null
            );
        }

        Object[] arguments = joinPoint == null ? null : joinPoint.getArgs();
        String recipientPhone = arguments != null && arguments.length > 0 && arguments[0] != null
                ? arguments[0].toString()
                : "";
        String normalizedPhone = recipientPhone.replaceAll("[^0-9]", "");
        if (normalizedPhone.startsWith("00")) {
            normalizedPhone = normalizedPhone.substring(2);
        }
        if (normalizedPhone.isBlank()) {
            return WhatsAppCloudSendResult.failed(
                    "WhatsApp validation mock: número do destinatário inválido.",
                    null
            );
        }

        return WhatsAppCloudSendResult.sent(
                "validation-mock-" + UUID.randomUUID(),
                200
        );
    }

    private boolean isLocalEndpoint() {
        String normalized = graphApiBaseUrl == null ? "" : graphApiBaseUrl.trim().toLowerCase();
        return normalized.startsWith("http://localhost")
                || normalized.startsWith("https://localhost")
                || normalized.startsWith("http://127.0.0.1")
                || normalized.startsWith("https://127.0.0.1");
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "";
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
