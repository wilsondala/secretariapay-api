package com.secretariapay.api.service.whatsapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SecretariaPayFinancialProductionGuardAspect {

    private final boolean infinitePayTestEnabled;

    public SecretariaPayFinancialProductionGuardAspect(
            @Value("${secretariapay.payments.infinitepay-test-enabled:false}") boolean infinitePayTestEnabled
    ) {
        this.infinitePayTestEnabled = infinitePayTestEnabled;
    }

    @Around("execution(java.util.Optional com.secretariapay.api.service.whatsapp.SecretariaPayWhatsappFinancialDemoConversationService.handle(..))")
    public Object protectMainFinancialFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        if (shouldBlockTestPayment(joinPoint.getArgs())) return Optional.of(buildInstitutionalPaymentNotice());
        return sanitizeResult(joinPoint.proceed());
    }

    @Around("execution(java.util.Optional com.secretariapay.api.service.whatsapp.AcademicServicesWhatsappFlowService.handleIfActive(..))")
    public Object protectAcademicServicesFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        if (shouldBlockTestPayment(joinPoint.getArgs())) return Optional.of(buildInstitutionalPaymentNotice());
        return sanitizeResult(joinPoint.proceed());
    }

    private boolean shouldBlockTestPayment(Object[] args) {
        if (infinitePayTestEnabled || args == null || args.length < 3) return false;
        String message = normalize(String.valueOf(args[2]));
        return "8".equals(message)
                || containsAny(message, "infinitepay", "infinite pay", "pix brasil", "teste brasil", "teste real brasil");
    }

    private Object sanitizeResult(Object result) {
        if (!(result instanceof Optional<?> optional) || optional.isEmpty() || !(optional.get() instanceof String text)) return result;
        return Optional.of(sanitizeFinancialText(text));
    }

    String sanitizeFinancialText(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;

        String sanitized = Arrays.stream(value.split("\\R", -1))
                .filter(line -> !containsAny(normalize(line), "infinitepay", "infinite pay", "teste real brasil", "pix brasil"))
                .collect(Collectors.joining("\n"));

        return sanitized
                .replaceAll("(?iu)AppyPay\\s+Sandbox", "AppyPay")
                .replaceAll("(?iu)fluxo\\s+financeiro\\s+demo", "fluxo financeiro institucional")
                .replaceAll("(?iu)\\[3]\\s+Borderô\\s+financeiro", "[3] Recibos e documentos")
                .replaceAll("(?iu)DCR\\s*/\\s*IMETRO", "Instituto Superior Politécnico Metropolitano de Angola — IMETRO")
                .replaceAll("(?m)^[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String buildInstitutionalPaymentNotice() {
        return """
                Esta forma de pagamento não está disponível no ambiente institucional do IMETRO.

                Utilize uma das opções autorizadas:

                [1] Multicaixa Express — AppyPay
                [2] Pagamento por Referência — AppyPay
                [3] Transferência entre contas BAI
                [4] Depósito ou transferência de outro banco
                [5] Voltar ao menu principal
                """.trim();
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank() || terms == null) return false;
        for (String term : terms) if (value.contains(normalize(term))) return true;
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
