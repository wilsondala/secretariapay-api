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
import java.util.Locale;

@Service
public class SecretariaPaySmsNotificationService {

    private final SecretariaPayMessageRepository messageRepository;
    private final boolean smsEnabled;
    private final boolean smsMockEnabled;
    private final String smsProvider;

    public SecretariaPaySmsNotificationService(
            SecretariaPayMessageRepository messageRepository,
            @Value("${secretariapay.sms.enabled:false}") boolean smsEnabled,
            @Value("${secretariapay.sms.mock-enabled:true}") boolean smsMockEnabled,
            @Value("${secretariapay.sms.provider:MOCK}") String smsProvider
    ) {
        this.messageRepository = messageRepository;
        this.smsEnabled = smsEnabled;
        this.smsMockEnabled = smsMockEnabled;
        this.smsProvider = smsProvider;
    }

    @Transactional
    public NotificationDispatchResult sendPaymentGuideSms(Charge charge, String guideUrl) {
        Student student = student(charge);
        String recipient = firstNotBlank(student != null ? student.getPhone() : null, student != null ? student.getGuardianPhone() : null);

        SecretariaPayMessage message = baseMessage(charge, student)
                .setType("PAYMENT_GUIDE_SMS")
                .setChannel("SMS")
                .setRecipientPhone(recipient)
                .setMessage(buildSmsText(charge, student, guideUrl));

        if (isBlank(recipient)) {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Estudante sem telefone para SMS.");
            message = messageRepository.save(message);
            return result(message, recipient);
        }

        if (smsEnabled || smsMockEnabled) {
            String providerPrefix = smsEnabled ? "sms-provider-pending-" : "mock-sms-";
            message.setStatus(SecretariaPayMessageStatus.SENT)
                    .setProviderMessageId(providerPrefix + safe(smsProvider).toLowerCase(Locale.ROOT) + "-" + messageIdSeed(charge, recipient))
                    .setFailureReason(smsEnabled ? "SMS real ainda sem adaptador de provedor configurado; envio registrado para integração." : null)
                    .setSentAt(LocalDateTime.now());
        } else {
            message.setStatus(SecretariaPayMessageStatus.FAILED)
                    .setFailureReason("Canal de SMS desativado.");
        }

        message = messageRepository.save(message);
        return result(message, recipient);
    }

    private SecretariaPayMessage baseMessage(Charge charge, Student student) {
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

    private String buildSmsText(Charge charge, Student student, String guideUrl) {
        return "IMETRO/DCR: Ola, %s. A sua guia %s esta disponivel: %s Valor: %s. Recibo apos confirmacao da DCR."
                .formatted(
                        firstName(student != null ? student.getFullName() : null),
                        charge != null ? safe(charge.getReferenceMonth()) : "",
                        guideUrl,
                        charge != null ? money(charge.getTotalAmount(), charge.getCurrency()) : "-"
                );
    }

    private NotificationDispatchResult result(SecretariaPayMessage message, String recipient) {
        return new NotificationDispatchResult()
                .setMessageId(message.getId())
                .setChannel("SMS")
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
