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
    private final StudentFinancialStatementWhatsappService financialStatementFlow;
    private final Map<String, Boolean> awaitingMainMenuSelection = new ConcurrentHashMap<>();

    public AcademicServicesWhatsappFlowAspect(
            AcademicServicesWhatsappFlowService academicServicesFlow,
            StudentFinancialStatementWhatsappService financialStatementFlow
    ) {
        this.academicServicesFlow = academicServicesFlow;
        this.financialStatementFlow = financialStatementFlow;
    }

    @Around("execution(java.util.Optional com.secretariapay.api.service.whatsapp.SecretariaPayWhatsappFinancialDemoConversationService.handle(..))")
    public Object routeAcademicServices(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String phone = args.length > 0 ? sanitizePhone(String.valueOf(args[0])) : "";
        String messageType = args.length > 1 ? String.valueOf(args[1]) : "text";
        String message = args.length > 2 ? String.valueOf(args[2]) : "";
        String normalized = normalize(message);

        Optional<String> statementReply = financialStatementFlow.handleIfActive(phone, messageType, message);
        if (statementReply.isPresent()) {
            awaitingMainMenuSelection.remove(phone);
            return statementReply;
        }

        Optional<String> activeFlowReply = academicServicesFlow.handleIfActive(phone, messageType, message);
        if (activeFlowReply.isPresent()) {
            awaitingMainMenuSelection.remove(phone);
            return activeFlowReply;
        }

        if (isFinancialSummaryIntent(normalized)) {
            awaitingMainMenuSelection.remove(phone);
            return Optional.of(financialStatementFlow.startSummary(phone));
        }
        if (isFinancialDocumentsIntent(normalized)) {
            awaitingMainMenuSelection.remove(phone);
            return Optional.of(financialStatementFlow.startDocuments(phone));
        }
        if (academicServicesFlow.isServiceIntent(message) || isOfficialAcademicServiceIntent(normalized)) {
            awaitingMainMenuSelection.remove(phone);
            return Optional.of(academicServicesFlow.start(phone));
        }

        if (Boolean.TRUE.equals(awaitingMainMenuSelection.get(phone))) {
            awaitingMainMenuSelection.remove(phone);
            if ("2".equals(normalized)) return Optional.of(financialStatementFlow.startSummary(phone));
            if ("3".equals(normalized)) return Optional.of(financialStatementFlow.startDocuments(phone));
            if ("4".equals(normalized)) return Optional.of(academicServicesFlow.start(phone));
            if ("5".equals(normalized)) {
                return Optional.of("Certo. A sua solicitação será encaminhada para a DCR.\n\nInforme o motivo do atendimento.");
            }
        }

        Object result = joinPoint.proceed();
        if (result instanceof Optional<?> optional && optional.isPresent() && optional.get() instanceof String reply) {
            if (isLegacyMainMenu(reply)) {
                awaitingMainMenuSelection.put(phone, true);
                return Optional.of(buildInstitutionalMenu());
            }
            awaitingMainMenuSelection.remove(phone);
        }
        return result;
    }

    private String buildInstitutionalMenu() {
        return """
                Secretaria Pay (IMETRO) 👋

                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Como posso ajudar?

                [1] Propinas
                [2] Situação financeira
                [3] Recibos e documentos
                [4] Serviços académicos
                [5] Falar com a DCR
                """.trim();
    }

    private boolean isFinancialSummaryIntent(String value) {
        return containsAny(value, "situacao financeira", "estado financeiro", "saldo", "minhas dividas", "minhas pendencias");
    }

    private boolean isFinancialDocumentsIntent(String value) {
        return containsAny(value,
                "recibos e documentos",
                "bordero de propinas",
                "borderô de propinas",
                "historico de servicos",
                "histórico de serviços",
                "meus recibos");
    }

    private boolean isOfficialAcademicServiceIntent(String normalized) {
        return containsAny(normalized,
                "servicos academicos", "servico academico", "pagar matricula", "matricula",
                "confirmacao de matricula", "confirmar matricula", "inscricao", "pagar recurso",
                "recurso", "exame de recurso", "exame especial", "declaracao com nota",
                "declaracao sem nota", "pagar declaracao", "declaracao", "certificado", "diploma");
    }

    private boolean isLegacyMainMenu(String reply) {
        String normalized = normalize(reply);
        return normalized.contains("como posso ajudar")
                && normalized.contains("[1] propinas")
                && normalized.contains("falar com a dcr");
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

    private String sanitizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
