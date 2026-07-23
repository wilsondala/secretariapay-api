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
    private static final String WHATSAPP_CHANNEL = "WHATSAPP";
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
        if (application == null || application.getApplicationCode() == null) return;

        String idempotencyKey = APPLICATION_SUBMITTED_EVENT
                + ":" + WHATSAPP_CHANNEL
                + ":" + application.getApplicationCode().trim().toUpperCase(Locale.ROOT)
                + ":" + normalizePhone(recipientWhatsapp);

        if (repository.existsByIdempotencyKey(idempotencyKey)) return;

        repository.save(new AdmissionOperationalNotification()
                .setApplication(application)
                .setEventType(APPLICATION_SUBMITTED_EVENT)
                .setChannel(WHATSAPP_CHANNEL)
                .setRecipient(recipientWhatsapp)
                .setMessageBody(buildApplicationSubmittedMessage(application))
                .setIdempotencyKey(idempotencyKey)
                .setStatus(AdmissionNotificationStatus.PENDING)
                .setAttempts(0)
                .setNextAttemptAt(LocalDateTime.now(LUANDA_ZONE)));
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

    static String maskDocument(String document) {
        String value = clean(document, null);
        if (value == null) return "Não informado";
        if (value.length() <= 4) return "****";
        int visible = Math.min(4, value.length());
        return "*".repeat(Math.max(4, value.length() - visible))
                + value.substring(value.length() - visible);
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
