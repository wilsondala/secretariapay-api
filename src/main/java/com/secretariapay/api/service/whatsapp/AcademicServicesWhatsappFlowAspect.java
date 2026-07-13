package com.secretariapay.api.service.whatsapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AcademicServicesWhatsappFlowAspect {

    private final AcademicServicesWhatsappFlowService academicServicesFlow;
    private final Map<String, Boolean> awaitingMainMenuSelection = new ConcurrentHashMap<>();

    public AcademicServicesWhatsappFlowAspect(AcademicServicesWhatsappFlowService academicServicesFlow) {
        this.academicServicesFlow = academicServicesFlow;
    }

    @Around("execution(java.util.Optional com.secretariapay.api.service.whatsapp.SecretariaPayWhatsappFinancialDemoConversationService.handle(..))")
    public Object routeAcademicServices(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String phone = args.length > 0 ? sanitizePhone(String.valueOf(args[0])) : "";
        String messageType = args.length > 1 ? String.valueOf(args[1]) : "text";
        String message = args.length > 2 ? String.valueOf(args[2]) : "";
        String normalized = normalize(message);

        Optional<String> activeFlowReply = academicServicesFlow.handleIfActive(phone, messageType, message);
        if (activeFlowReply.isPresent()) {
            awaitingMainMenuSelection.remove(phone);
            return activeFlowReply;
        }

        if (academicServicesFlow.isServiceIntent(message)) {
            awaitingMainMenuSelection.remove(phone);
            return Optional.of(academicServicesFlow.start(phone));
        }

        if (Boolean.TRUE.equals(awaitingMainMenuSelection.get(phone))) {
            awaitingMainMenuSelection.remove(phone);
            if ("4".equals(normalized)) {
                return Optional.of(academicServicesFlow.start(phone));
            }
            if ("5".equals(normalized)) {
                return Optional.of("Certo. Vou encaminhar o seu atendimento para a DCR.\n\nInforme o motivo da solicitação.");
            }
        }

        Object result = joinPoint.proceed();
        if (result instanceof Optional<?> optional && optional.isPresent() && optional.get() instanceof String reply) {
            if (isLegacyMainMenu(reply)) {
                awaitingMainMenuSelection.put(phone, true);
                return Optional.of(academicServicesFlow.buildMainMenu());
            }
            awaitingMainMenuSelection.remove(phone);
        }
        return result;
    }

    private boolean isLegacyMainMenu(String reply) {
        String normalized = normalize(reply);
        return normalized.contains("como posso ajudar")
                && normalized.contains("[1] propinas")
                && normalized.contains("falar com a dcr");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String sanitizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
