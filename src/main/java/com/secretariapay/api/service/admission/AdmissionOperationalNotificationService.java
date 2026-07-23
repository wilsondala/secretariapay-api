package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionOperationalNotification;
import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import com.secretariapay.api.repository.admission.AdmissionOperationalNotificationRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class AdmissionOperationalNotificationService {

    static final String APPLICATION_SUBMITTED_EVENT = "APPLICATION_SUBMITTED";
    static final String ENROLLMENT_DOCUMENTS_REQUESTED_EVENT = "ENROLLMENT_DOCUMENTS_REQUESTED";
    private static final String WHATSAPP_CHANNEL = "WHATSAPP";
    private static final String MISSING_RECIPIENT = "SEM_CONTACTO";
    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");

    private final AdmissionOperationalNotificationRepository repository;
    private final WhatsAppCloudApiClient whatsAppClient;
    private final boolean enabled;
    private final String recipientWhatsapp;
    private final String internalPanelUrl;
    private final int maxAttempts;

    public AdmissionOperationalNotificationService(
            AdmissionOperationalNotificationRepository repository,
            WhatsAppCloudApiClient whatsAppClient,
            @Value("${secretariapay.admissions.notifications.enabled:false}") boolean enabled,
            @Value("${secretariapay.admissions.notifications.recipient-whatsapp:+244 991 640 259}") String recipientWhatsapp,
            @Value("${secretariapay.admissions.notifications.internal-panel-url:http://localhost:5173/admissions}") String internalPanelUrl,
            @Value("${secretariapay.admissions.notifications.max-attempts:5}") int maxAttempts
    ) {
        this.repository = repository;
        this.whatsAppClient = whatsAppClient;
        this.enabled = enabled;
        this.recipientWhatsapp = clean(recipientWhatsapp, "+244 991 640 259");
        this.internalPanelUrl = stripTrailingSlash(clean(internalPanelUrl, "http://localhost:5173/admissions"));
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Transactional
    public void enqueueApplicationSubmitted(AdmissionApplication application) {
        if (!hasApplicationCode(application)) return;
        enqueue(
                application,
                APPLICATION_SUBMITTED_EVENT,
                recipientWhatsapp,
                buildApplicationSubmittedMessage(application)
        );
    }

    @Transactional
    public void enqueueEnrollmentDocumentsRequested(AdmissionApplication application) {
        if (!hasApplicationCode(application)) return;
        String candidateContact = firstNonBlank(application.getWhatsapp(), application.getPhone());
        enqueue(
                application,
                ENROLLMENT_DOCUMENTS_REQUESTED_EVENT,
                candidateContact,
                buildEnrollmentDocumentsMessage(application)
        );
    }

    private void enqueue(
            AdmissionApplication application,
            String eventType,
            String recipient,
            String messageBody
    ) {
        String normalizedRecipient = normalizePhone(recipient);
        boolean missingContact = normalizedRecipient.isBlank();
        String recipientKey = missingContact ? MISSING_RECIPIENT : normalizedRecipient;
        String storedRecipient = missingContact ? MISSING_RECIPIENT : recipient.trim();
        String idempotencyKey = eventType
                + ":" + WHATSAPP_CHANNEL
                + ":" + application.getApplicationCode().trim().toUpperCase(Locale.ROOT)
                + ":" + recipientKey;

        if (repository.existsByIdempotencyKey(idempotencyKey)) return;

        AdmissionOperationalNotification notification = new AdmissionOperationalNotification()
                .setApplication(application)
                .setEventType(eventType)
                .setChannel(WHATSAPP_CHANNEL)
                .setRecipient(storedRecipient)
                .setMessageBody(messageBody)
                .setIdempotencyKey(idempotencyKey)
                .setStatus(missingContact
                        ? AdmissionNotificationStatus.EXHAUSTED
                        : AdmissionNotificationStatus.PENDING)
                .setAttempts(0)
                .setNextAttemptAt(LocalDateTime.now(LUANDA_ZONE));

        if (missingContact) {
            notification.setLastError(
                    "A candidatura não possui WhatsApp nem telefone para solicitar os documentos da matrícula."
            );
        }

        repository.save(notification);
    }

    @Scheduled(
            cron = "${secretariapay.admissions.notifications.dispatch-cron:*/15 * * * * *}",
            zone = "Africa/Luanda"
    )
    @Transactional
    public void dispatchPending() {
        if (!enabled) return;

        List<AdmissionOperationalNotification> pending = repository
                .findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(
                                AdmissionNotificationStatus.PENDING,
                                AdmissionNotificationStatus.FAILED
                        ),
                        LocalDateTime.now(LUANDA_ZONE)
                );

        for (AdmissionOperationalNotification notification : pending) {
            dispatch(notification);
        }
    }

    private void dispatch(AdmissionOperationalNotification notification) {
        int attempts = notification.getAttempts() == null ? 0 : notification.getAttempts();
        if (attempts >= maxAttempts) {
            notification.setStatus(AdmissionNotificationStatus.EXHAUSTED)
                    .setLastError(firstNonBlank(
                            notification.getLastError(),
                            "Número máximo de tentativas atingido."
                    ));
            repository.save(notification);
            return;
        }

        int currentAttempt = attempts + 1;
        notification.setAttempts(currentAttempt);

        try {
            WhatsAppCloudSendResult result = whatsAppClient.sendText(
                    notification.getRecipient(),
                    notification.getMessageBody()
            );

            if (result != null && result.isSuccess()) {
                notification.setStatus(AdmissionNotificationStatus.SENT)
                        .setProviderMessageId(result.getProviderMessageId())
                        .setLastError(null)
                        .setSentAt(LocalDateTime.now(LUANDA_ZONE));
            } else {
                markFailed(
                        notification,
                        result == null ? "O provedor não devolveu resultado." : result.getErrorMessage(),
                        currentAttempt
                );
            }
        } catch (Exception exception) {
            markFailed(notification, exception.getMessage(), currentAttempt);
        }

        repository.save(notification);
    }

    private void markFailed(
            AdmissionOperationalNotification notification,
            String error,
            int currentAttempt
    ) {
        boolean exhausted = currentAttempt >= maxAttempts;
        notification.setStatus(exhausted
                        ? AdmissionNotificationStatus.EXHAUSTED
                        : AdmissionNotificationStatus.FAILED)
                .setLastError(clean(error, "Falha não especificada no envio pelo WhatsApp."))
                .setNextAttemptAt(LocalDateTime.now(LUANDA_ZONE)
                        .plusMinutes(Math.min(30L, currentAttempt * 5L)));
    }

    private String buildApplicationSubmittedMessage(AdmissionApplication application) {
        String course = application.getDesiredCourse() == null
                ? "Não informado"
                : clean(application.getDesiredCourse().getName(), "Não informado");
        String shift = shiftLabel(application.getDesiredShift());
        String contact = firstNonBlank(application.getWhatsapp(), application.getPhone());
        String panelLink = internalPanelUrl + "?applicationCode=" + application.getApplicationCode();

        return """
                Nova candidatura recebida — IMETRO

                Código: %s
                Candidato: %s
                Documento: %s
                Curso: %s
                Turno: %s
                WhatsApp: %s
                Estado: Inscrição submetida
                Ano académico: %s

                Acompanhe a candidatura no painel SecretáriaPay:
                %s
                """.formatted(
                application.getApplicationCode(),
                clean(application.getFullName(), "Não informado"),
                maskDocument(application.getDocumentNumber()),
                course,
                shift,
                clean(contact, "Não informado"),
                clean(application.getAcademicYear(), "Não informado"),
                panelLink
        ).trim();
    }

    private String buildEnrollmentDocumentsMessage(AdmissionApplication application) {
        String course = application.getDesiredCourse() == null
                ? "Não informado"
                : clean(application.getDesiredCourse().getName(), "Não informado");

        return """
                Inscrição confirmada — Próxima etapa: matrícula

                Caro(a) %s,

                A DCR confirmou o pagamento da sua inscrição. O seu processo documental da matrícula foi liberado.

                Código da candidatura: %s
                Curso: %s
                Turno: %s
                Ano académico: %s

                1. Envie pelo robô SecretáriaPay as cópias digitais dos documentos obrigatórios:
                • 2 fotografias do tipo passe
                • Fotocópia autenticada do certificado de habilitações
                • Fotocópia do Bilhete de Identidade
                • Equivalência do Ministério da Educação, somente para candidatos que estudaram no estrangeiro

                2. Depois do envio digital, apresente-se presencialmente na Secretaria Académica com os documentos originais:
                • Certificado original de habilitações
                • Bilhete de Identidade original
                • Equivalência original, quando aplicável

                A Secretaria confrontará as cópias enviadas com os originais para confirmar a autenticidade. A cobrança da matrícula de 23.500,00 Kz somente será liberada depois dessa conferência presencial.

                Requisitos de elegibilidade:
                • Ensino médio concluído
                • Idade mínima de 18 anos

                SecretariaPay IMETRO
                """.formatted(
                clean(application.getFullName(), "Candidato(a)"),
                application.getApplicationCode(),
                course,
                shiftLabel(application.getDesiredShift()),
                clean(application.getAcademicYear(), "Não informado")
        ).trim();
    }

    static String maskDocument(String document) {
        String value = clean(document, null);
        if (value == null) return "Não informado";
        if (value.length() <= 4) return "****";
        int visible = Math.min(4, value.length());
        return "*".repeat(Math.max(4, value.length() - visible))
                + value.substring(value.length() - visible);
    }

    private boolean hasApplicationCode(AdmissionApplication application) {
        return application != null
                && application.getApplicationCode() != null
                && !application.getApplicationCode().isBlank();
    }

    private String shiftLabel(String shift) {
        String normalized = clean(shift, "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MANHA", "MANHÃ", "MORNING" -> "Manhã";
            case "TARDE", "AFTERNOON" -> "Tarde";
            case "NOITE", "NIGHT", "EVENING" -> "Noite";
            default -> clean(shift, "Não informado");
        };
    }

    private String normalizePhone(String phone) {
        String value = clean(phone, "");
        String digits = value.replaceAll("[^0-9]", "");
        return digits.startsWith("00") ? digits.substring(2) : digits;
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) return value.substring(0, value.length() - 1);
        return value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String value = clean(preferred, null);
        return value == null ? clean(fallback, null) : value;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
