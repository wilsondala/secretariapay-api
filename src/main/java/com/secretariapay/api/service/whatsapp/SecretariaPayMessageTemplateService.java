package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessagePreviewResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class SecretariaPayMessageTemplateService {

    private static final String CHANNEL_WHATSAPP = "WHATSAPP";
    private static final String LANGUAGE_PT_AO = "pt-AO";
    private static final String PUBLIC_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ChargeRepository chargeRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final ReceiptRepository receiptRepository;
    private final StudentRepository studentRepository;

    public SecretariaPayMessageTemplateService(
            ChargeRepository chargeRepository,
            PaymentProofRepository paymentProofRepository,
            ReceiptRepository receiptRepository,
            StudentRepository studentRepository
    ) {
        this.chargeRepository = chargeRepository;
        this.paymentProofRepository = paymentProofRepository;
        this.receiptRepository = receiptRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse beforeDue(UUID chargeId, Integer daysBefore) {
        Charge charge = findCharge(chargeId);
        int safeDaysBefore = daysBefore == null || daysBefore < 1 ? 5 : daysBefore;

        String message = """
                Olá, %s.

                A sua %s vence em %d dias.
                Valor: %s.
                Vencimento: %s.

                Deseja receber a referência de pagamento ou falar com a secretaria?

                %s
                """.formatted(
                firstName(student(charge).getFullName()),
                charge.getDescription(),
                safeDaysBefore,
                money(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate().format(DATE_FORMATTER),
                signature(charge)
        );

        return baseFromCharge("BEFORE_DUE", charge).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse dueToday(UUID chargeId) {
        Charge charge = findCharge(chargeId);

        String message = """
                Olá, %s.

                A sua %s vence hoje.
                Valor: %s.

                Para evitar restrições académicas, regularize o pagamento e envie o comprovativo por aqui.

                %s
                """.formatted(
                firstName(student(charge).getFullName()),
                charge.getDescription(),
                money(charge.getTotalAmount(), charge.getCurrency()),
                signature(charge)
        );

        return baseFromCharge("DUE_TODAY", charge).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse overdue(UUID chargeId, Integer daysLate) {
        Charge charge = findCharge(chargeId);
        int safeDaysLate = daysLate == null || daysLate < 1 ? 1 : daysLate;

        String message = """
                Olá, %s.

                A sua %s encontra-se em atraso há %d dia(s).
                Valor atualizado: %s.
                Vencimento original: %s.

                Regularize para evitar restrições académicas conforme as regras da instituição.

                %s
                """.formatted(
                firstName(student(charge).getFullName()),
                charge.getDescription(),
                safeDaysLate,
                money(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate().format(DATE_FORMATTER),
                signature(charge)
        );

        return baseFromCharge("OVERDUE", charge).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse proofReceived(UUID paymentProofId) {
        PaymentProof proof = findPaymentProof(paymentProofId);
        Charge charge = proof.getCharge();

        String message = """
                Olá, %s.

                Comprovativo recebido.
                A tesouraria fará a validação do pagamento da %s.

                Assim que aprovado, enviaremos o recibo digital e atualizaremos a sua situação académica.

                %s
                """.formatted(
                firstName(student(charge).getFullName()),
                charge.getDescription(),
                signature(charge)
        );

        return baseFromProof("PROOF_RECEIVED", proof).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse proofApproved(UUID paymentProofId) {
        PaymentProof proof = findPaymentProof(paymentProofId);
        Charge charge = proof.getCharge();

        String reviewedAt = proof.getReviewedAt() != null
                ? proof.getReviewedAt().format(DATE_TIME_FORMATTER)
                : "agora";

        String message = """
                Pagamento confirmado, %s.

                A tesouraria aprovou o comprovativo da %s.
                Valor confirmado: %s.
                Data de validação: %s.

                O recibo digital já pode ser emitido.

                %s
                """.formatted(
                firstName(student(charge).getFullName()),
                charge.getDescription(),
                money(charge.getTotalAmount(), charge.getCurrency()),
                reviewedAt,
                signature(charge)
        );

        return baseFromProof("PROOF_APPROVED", proof).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse receiptIssued(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Recibo não encontrado."));

        Charge charge = receipt.getCharge();

        String message = """
                Recibo digital emitido.

                Recibo nº: %s
                Estudante: %s
                Valor: %s
                Status: %s

                Baixar recibo:
                %s

                Validar recibo:
                %s

                %s
                """.formatted(
                receipt.getReceiptCode(),
                student(charge).getFullName(),
                money(charge.getTotalAmount(), charge.getCurrency()),
                receipt.getStatus(),
                pdfUrl(receipt),
                receipt.getValidationUrl(),
                signature(charge)
        );

        return baseFromReceipt("RECEIPT_ISSUED", receipt).setMessage(message.trim());
    }

    @Transactional(readOnly = true)
    public SecretariaPayMessagePreviewResponse regularized(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));

        String message = """
                Olá, %s.

                A sua situação financeira foi regularizada.
                Os serviços académicos autorizados pela instituição podem ser reativados conforme as regras internas.

                %s
                """.formatted(
                firstName(student.getFullName()),
                signature(student)
        );

        return baseFromStudent("REGULARIZED", student).setMessage(message.trim());
    }

    private SecretariaPayMessagePreviewResponse baseFromProof(String type, PaymentProof proof) {
        return baseFromCharge(type, proof.getCharge())
                .setPaymentProofId(proof.getId());
    }

    private SecretariaPayMessagePreviewResponse baseFromReceipt(String type, Receipt receipt) {
        return baseFromCharge(type, receipt.getCharge())
                .setReceiptId(receipt.getId())
                .setReceiptCode(receipt.getReceiptCode());
    }

    private SecretariaPayMessagePreviewResponse baseFromCharge(String type, Charge charge) {
        Student student = student(charge);
        Institution institution = institution(student);

        return new SecretariaPayMessagePreviewResponse()
                .setType(type)
                .setChannel(CHANNEL_WHATSAPP)
                .setLanguage(LANGUAGE_PT_AO)
                .setInstitutionId(institution != null ? institution.getId() : null)
                .setInstitutionName(displayInstitutionName(institution))
                .setStudentId(student.getId())
                .setStudentNumber(student.getStudentNumber())
                .setStudentName(student.getFullName())
                .setStudentWhatsapp(student.getWhatsapp())
                .setChargeId(charge.getId())
                .setChargeCode(charge.getChargeCode());
    }

    private SecretariaPayMessagePreviewResponse baseFromStudent(String type, Student student) {
        Institution institution = institution(student);

        return new SecretariaPayMessagePreviewResponse()
                .setType(type)
                .setChannel(CHANNEL_WHATSAPP)
                .setLanguage(LANGUAGE_PT_AO)
                .setInstitutionId(institution != null ? institution.getId() : null)
                .setInstitutionName(displayInstitutionName(institution))
                .setStudentId(student.getId())
                .setStudentNumber(student.getStudentNumber())
                .setStudentName(student.getFullName())
                .setStudentWhatsapp(student.getWhatsapp());
    }

    private Charge findCharge(UUID chargeId) {
        return chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
    }

    private PaymentProof findPaymentProof(UUID paymentProofId) {
        return paymentProofRepository.findById(paymentProofId)
                .orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));
    }

    private Student student(Charge charge) {
        if (charge == null || charge.getStudent() == null) {
            throw new NotFoundException("Estudante da cobrança não encontrado.");
        }

        return charge.getStudent();
    }

    private Institution institution(Student student) {
        if (student == null || student.getAcademicClass() == null) {
            return null;
        }

        AcademicClass academicClass = student.getAcademicClass();

        if (academicClass.getCourse() == null) {
            return null;
        }

        Course course = academicClass.getCourse();
        return course.getInstitution();
    }

    private String signature(Charge charge) {
        return signature(student(charge));
    }

    private String signature(Student student) {
        Institution institution = institution(student);
        return displayInstitutionName(institution) + "\nSecretáriaPay Académico";
    }

    private String displayInstitutionName(Institution institution) {
        if (institution == null) {
            return "SecretáriaPay Académico";
        }

        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) {
            return institution.getLegalName();
        }

        if (institution.getName() != null && !institution.getName().isBlank()) {
            return institution.getName();
        }

        return "SecretáriaPay Académico";
    }

    private String pdfUrl(Receipt receipt) {
        if (receipt.getPdfUrl() != null && !receipt.getPdfUrl().isBlank()) {
            return receipt.getPdfUrl();
        }

        if (receipt.getId() == null) {
            return "-";
        }

        return PUBLIC_BASE_URL + "/api/v1/receipts/" + receipt.getId() + "/pdf";
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "estudante";
        }

        return fullName.trim().split("\\s+")[0];
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

        if ("AOA".equalsIgnoreCase(safeCurrency)) {
            return formatter.format(safeAmount) + " Kz";
        }

        return formatter.format(safeAmount) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }
}
