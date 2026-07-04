package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecretariaPayPaymentGuideMessageService {

    private static final String PUBLIC_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChargeRepository chargeRepository;
    private final SecretariaPayMessageRepository messageRepository;

    public SecretariaPayPaymentGuideMessageService(
            ChargeRepository chargeRepository,
            SecretariaPayMessageRepository messageRepository
    ) {
        this.chargeRepository = chargeRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public SecretariaPayMessageResponse generatePaymentGuideMessage(UUID chargeId) {
        return generatePaymentGuideMessage(chargeId, false);
    }

    @Transactional
    public SecretariaPayMessageResponse generatePaymentGuideMessage(UUID chargeId, boolean forceResend) {
        if (!forceResend) {
            Optional<SecretariaPayMessage> alreadySent = findAlreadySentPaymentGuide(chargeId);
            if (alreadySent.isPresent()) {
                return toResponse(alreadySent.get());
            }
        }

        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        Student student = student(charge);
        Institution institution = institution(student);
        String guideUrl = paymentGuideUrl(charge);

        String messageText = """
                Olá, %s.

                Segue a sua guia de pagamento.

                Cobrança: %s
                Código: %s
                Valor: %s
                Vencimento: %s

                O PDF da guia segue em anexo com os dados bancários e instruções de pagamento.

                Após pagar, envie o comprovativo por aqui no WhatsApp.

                Guia pública:
                %s

                %s
                SecretáriaPay Académico
                """.formatted(
                firstName(student.getFullName()),
                charge.getDescription(),
                charge.getChargeCode(),
                money(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate().format(DATE_FORMATTER),
                guideUrl,
                displayInstitutionName(institution)
        ).trim();

        SecretariaPayMessage message = new SecretariaPayMessage()
                .setInstitutionId(institution != null ? institution.getId() : null)
                .setInstitutionName(displayInstitutionName(institution))
                .setStudentId(student.getId())
                .setStudentNumber(student.getStudentNumber())
                .setStudentName(student.getFullName())
                .setChargeId(charge.getId())
                .setChargeCode(charge.getChargeCode())
                .setType("PAYMENT_GUIDE")
                .setChannel("WHATSAPP")
                .setLanguage("pt-AO")
                .setRecipientPhone(student.getWhatsapp())
                .setMessage(messageText)
                .setStatus(SecretariaPayMessageStatus.GENERATED);

        return toResponse(messageRepository.save(message));
    }

    private Optional<SecretariaPayMessage> findAlreadySentPaymentGuide(UUID chargeId) {
        if (chargeId == null) {
            return Optional.empty();
        }

        return messageRepository.findByChargeIdOrderByCreatedAtDesc(chargeId)
                .stream()
                .filter(message -> message.getStatus() == SecretariaPayMessageStatus.SENT)
                .filter(message -> "PAYMENT_GUIDE".equalsIgnoreCase(message.getType()))
                .findFirst();
    }

    private String paymentGuideUrl(Charge charge) {
        return PUBLIC_BASE_URL + "/api/v1/public/payment-guides/" + charge.getChargeCode() + "/pdf";
    }

    private Student student(Charge charge) {
        if (charge == null || charge.getStudent() == null) {
            throw new NotFoundException("Estudante da cobrança não encontrado.");
        }
        return charge.getStudent();
    }

    private Institution institution(Student student) {
        if (student == null || student.getAcademicClass() == null) return null;
        AcademicClass academicClass = student.getAcademicClass();
        if (academicClass.getCourse() == null) return null;
        Course course = academicClass.getCourse();
        return course.getInstitution();
    }

    private String displayInstitutionName(Institution institution) {
        if (institution == null) return "SecretáriaPay Académico";
        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) return institution.getLegalName();
        if (institution.getName() != null && !institution.getName().isBlank()) return institution.getName();
        return "SecretáriaPay Académico";
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "estudante";
        return fullName.trim().split("\\s+")[0];
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return "AOA".equalsIgnoreCase(safeCurrency) ? formatter.format(safeAmount) + " Kz" : formatter.format(safeAmount) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private SecretariaPayMessageResponse toResponse(SecretariaPayMessage message) {
        return new SecretariaPayMessageResponse()
                .setId(message.getId())
                .setInstitutionId(message.getInstitutionId())
                .setInstitutionName(message.getInstitutionName())
                .setStudentId(message.getStudentId())
                .setStudentNumber(message.getStudentNumber())
                .setStudentName(message.getStudentName())
                .setChargeId(message.getChargeId())
                .setChargeCode(message.getChargeCode())
                .setPaymentProofId(message.getPaymentProofId())
                .setReceiptId(message.getReceiptId())
                .setReceiptCode(message.getReceiptCode())
                .setType(message.getType())
                .setChannel(message.getChannel())
                .setLanguage(message.getLanguage())
                .setRecipientPhone(message.getRecipientPhone())
                .setMessage(message.getMessage())
                .setStatus(message.getStatus())
                .setProviderMessageId(message.getProviderMessageId())
                .setFailureReason(message.getFailureReason())
                .setSentAt(message.getSentAt())
                .setReadAt(message.getReadAt())
                .setCreatedAt(message.getCreatedAt())
                .setUpdatedAt(message.getUpdatedAt());
    }
}
