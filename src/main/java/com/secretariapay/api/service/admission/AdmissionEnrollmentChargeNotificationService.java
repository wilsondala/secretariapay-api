package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionOperationalNotification;
import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import com.secretariapay.api.repository.admission.AdmissionOperationalNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AdmissionEnrollmentChargeNotificationService {

    static final String EVENT_TYPE = "ENROLLMENT_CHARGE_CREATED";
    private static final String CHANNEL = "WHATSAPP";
    private static final String MISSING_RECIPIENT = "SEM_CONTACTO";
    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AdmissionOperationalNotificationRepository repository;

    public AdmissionEnrollmentChargeNotificationService(
            AdmissionOperationalNotificationRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public void enqueue(
            AdmissionApplication application,
            EnrollmentDto.EnrollmentResponse enrollment
    ) {
        if (application == null
                || application.getApplicationCode() == null
                || application.getApplicationCode().isBlank()
                || enrollment == null
                || enrollment.invoice() == null) {
            return;
        }

        String contact = firstNonBlank(application.getWhatsapp(), application.getPhone());
        String normalizedContact = normalizePhone(contact);
        boolean missingContact = normalizedContact.isBlank();
        String storedRecipient = missingContact ? MISSING_RECIPIENT : contact.trim();
        String recipientKey = missingContact ? MISSING_RECIPIENT : normalizedContact;
        String idempotencyKey = EVENT_TYPE
                + ":" + CHANNEL
                + ":" + application.getApplicationCode().trim().toUpperCase(Locale.ROOT)
                + ":" + recipientKey;

        if (repository.existsByIdempotencyKey(idempotencyKey)) return;

        AdmissionOperationalNotification notification = new AdmissionOperationalNotification()
                .setApplication(application)
                .setEventType(EVENT_TYPE)
                .setChannel(CHANNEL)
                .setRecipient(storedRecipient)
                .setMessageBody(buildMessage(application, enrollment))
                .setIdempotencyKey(idempotencyKey)
                .setStatus(missingContact
                        ? AdmissionNotificationStatus.EXHAUSTED
                        : AdmissionNotificationStatus.PENDING)
                .setAttempts(0)
                .setNextAttemptAt(LocalDateTime.now(LUANDA_ZONE));

        if (missingContact) {
            notification.setLastError(
                    "A candidatura não possui WhatsApp nem telefone para receber a guia da matrícula."
            );
        }
        repository.save(notification);
    }

    private String buildMessage(
            AdmissionApplication application,
            EnrollmentDto.EnrollmentResponse enrollment
    ) {
        EnrollmentDto.InvoiceResponse invoice = enrollment.invoice();
        return """
                Documentos recebidos — guia de matrícula disponível

                Caro(a) %s,

                Os documentos digitais obrigatórios da sua matrícula foram recebidos e o processo financeiro foi preparado.

                Código da candidatura: %s
                Pedido de matrícula: %s
                Valor: %s
                Vencimento: %s

                Efetue o pagamento dentro do prazo e envie o comprovativo pelo SecretáriaPay para validação da DCR. Depois da confirmação, a matrícula será concluída e o seu número de estudante será gerado automaticamente.

                Importante: os documentos originais deverão ser apresentados presencialmente à Secretaria Académica antes do encerramento do período de matrícula. A falta de apresentação poderá causar bloqueio temporário até à regularização.

                SecretariaPay IMETRO
                """.formatted(
                clean(application.getFullName(), "Candidato(a)"),
                application.getApplicationCode(),
                clean(enrollment.requestCode(), "A confirmar"),
                money(invoice.amount(), invoice.currency()),
                invoice.dueDate() == null ? "A confirmar" : invoice.dueDate().format(DATE_FORMAT)
        ).trim();
    }

    private String money(BigDecimal amount, String currency) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("pt", "AO"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount == null ? BigDecimal.ZERO : amount)
                + " "
                + (currency == null || currency.isBlank() || "AOA".equalsIgnoreCase(currency) ? "Kz" : currency);
    }

    private String normalizePhone(String phone) {
        String value = clean(phone, "");
        String digits = value.replaceAll("[^0-9]", "");
        return digits.startsWith("00") ? digits.substring(2) : digits;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String value = clean(preferred, null);
        return value == null ? clean(fallback, null) : value;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
