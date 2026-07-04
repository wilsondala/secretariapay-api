package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class SecretariaPayEmailNotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SecretariaPayMessageRepository messageRepository;
    private final boolean emailEnabled;
    private final boolean emailMockEnabled;
    private final String emailFrom;
    private final String emailCc;

    public SecretariaPayEmailNotificationService(
            SecretariaPayMessageRepository messageRepository,
            @Value("${secretariapay.email.enabled:false}") boolean emailEnabled,
            @Value("${secretariapay.email.mock-enabled:true}") boolean emailMockEnabled,
            @Value("${secretariapay.email.from:dcr_pay@imetroangola.com}") String emailFrom,
            @Value("${secretariapay.email.cc:df.oi_pay@imetroangola.com}") String emailCc
    ) {
        this.messageRepository = messageRepository;
        this.emailEnabled = emailEnabled;
        this.emailMockEnabled = emailMockEnabled;
        this.emailFrom = emailFrom;
        this.emailCc = emailCc;
    }

    @Transactional
    public NotificationDispatchResult sendPaymentGuideEmail(Charge charge, String guideUrl) {
        Student student = student(charge);
        String recipient = firstNotBlank(student.getEmail(), student.getGuardianEmail());

        SecretariaPayMessage message = baseMessage(charge, student, guideUrl)
                .setType("PAYMENT_GUIDE_EMAIL")
                .setChannel("EMAIL")
                .setRecipientPhone(null)
                .setMessage(buildEmailBody(charge, student, recipient, guideUrl));

        if (isBlank(recipient)) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Estudante sem e-mail cadastrado.");
            message = messageRepository.save(message);
            return result(message, "EMAIL", recipient);
        }

        if (emailEnabled || emailMockEnabled) {
            String providerPrefix = emailEnabled ? "email-provider-pending-" : "mock-email-";
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId(providerPrefix + messageIdSeed(charge, recipient))
                    .setFailureReason(emailEnabled ? "E-mail real ainda sem adaptador SMTP configurado; envio registrado para integração." : null)
                    .setSentAt(LocalDateTime.now());
        } else {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Canal de e-mail desativado.");
        }

        message = messageRepository.save(message);
        return result(message, "EMAIL", recipient);
    }

    private SecretariaPayMessage baseMessage(Charge charge, Student student, String guideUrl) {
        Institution institution = institution(student);
        return new SecretariaPayMessage()
                .setInstitutionId(institution != null ? institution.getId() : null)
                .setInstitutionName(displayInstitutionName(institution))
                .setStudentId(student != null ? student.getId() : null)
                .setStudentNumber(student != null ? student.getStudentNumber() : null)
                .setStudentName(student != null ? student.getFullName() : null)
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setLanguage("pt-AO")
                .setStatus(SecretariaPayMessageStatus.GENERATED);
    }

    private String buildEmailBody(Charge charge, Student student, String recipient, String guideUrl) {
        return """
                Para: %s
                De: %s
                Cc: %s
                Assunto: Guia de pagamento IMETRO - %s

                Olá, %s.

                A sua guia de pagamento está disponível.

                Código: %s
                Descrição: %s
                Valor: %s
                Vencimento: %s

                Link da guia:
                %s

                Após o pagamento, envie o comprovativo para validação da DCR.
                O recibo final só será emitido após confirmação manual.

                SecretáriaPay Académico / IMETRO
                """.formatted(
                safe(recipient),
                safe(emailFrom),
                safe(emailCc),
                safe(charge.getDescription()),
                firstName(student != null ? student.getFullName() : null),
                safe(charge.getChargeCode()),
                safe(charge.getDescription()),
                money(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate() != null ? charge.getDueDate().format(DATE_FORMATTER) : "-",
                guideUrl
        ).trim();
    }

    private NotificationDispatchResult result(SecretariaPayMessage message, String channel, String recipient) {
        return new NotificationDispatchResult()
                .setMessageId(message.getId())
                .setChannel(channel)
                .setStatus(message.getStatus() != null ? message.getStatus().name() : null)
                .setRecipient(recipient)
                .setProviderMessageId(message.getProviderMessageId())
                .setFailureReason(message.getFailureReason());
    }

    private Student student(Charge charge) {
        return charge == null ? null : charge.getStudent();
    }

    private Institution institution(Student student) {
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        return course != null ? course.getInstitution() : null;
    }

    private String displayInstitutionName(Institution institution) {
        if (institution == null) return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
        if (!isBlank(institution.getLegalName())) return institution.getLegalName();
        if (!isBlank(institution.getName())) return institution.getName();
        return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
    }

    private String firstName(String fullName) {
        if (isBlank(fullName)) return "estudante";
        return fullName.trim().split("\\s+")[0];
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = isBlank(currency) ? "AOA" : currency;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return "AOA".equalsIgnoreCase(safeCurrency) ? formatter.format(safeAmount) + " Kz" : formatter.format(safeAmount) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private String firstNotBlank(String first, String second) {
        return !isBlank(first) ? first.trim() : (!isBlank(second) ? second.trim() : null);
    }

    private String messageIdSeed(Charge charge, String recipient) {
        return (charge != null ? charge.getId() : "no-charge") + "-" + Math.abs(safe(recipient).hashCode());
    }

    private String safe(String value) { return value == null ? "" : value; }
    private boolean isBlank(String value) { return value == null || value.trim().isBlank(); }
}
