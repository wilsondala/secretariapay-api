package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class FinancialCommunicationTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildPaymentGuideWhatsapp(Charge charge, Student student, String institutionName, String guideUrl) {
        CommunicationContext context = context(charge);
        String urgency = urgencyText(context);

        return """
                Caro(a) estudante %s,

                %s

                Dados da cobrança:

                Estudante: %s
                Matrícula: %s
                Referência: %s
                Descrição: %s
                Valor total: %s
                Data de vencimento: %s
                Situação: %s

                A Guia de Pagamento Académico segue em anexo.

                %s

                Caso o pagamento já tenha sido efetuado, envie o respetivo comprovativo por este canal para validação e atualização da sua situação financeira.

                Guia digital:
                %s

                Atenciosamente,
                Secretaria Financeira
                %s
                Powered by SecretáriaPay
                """.formatted(
                firstName(student != null ? student.getFullName() : null),
                context.openingMessage(),
                safe(student != null ? student.getFullName() : null, "-"),
                safe(student != null ? student.getStudentNumber() : null, "-"),
                reference(charge),
                safe(charge != null ? charge.getDescription() : null, "Cobrança académica"),
                money(charge != null ? charge.getTotalAmount() : null, charge != null ? charge.getCurrency() : null),
                dueDate(charge),
                context.statusLabel(),
                urgency,
                safe(guideUrl, "-"),
                safe(institutionName, "IMETRO")
        ).trim();
    }

    public String buildPaymentGuideEmail(Charge charge, Student student, String institutionName,
                                         String recipient, String from, String cc, String guideUrl) {
        CommunicationContext context = context(charge);

        return """
                Para: %s
                De: %s
                Cc: %s
                Assunto: %s

                Caro(a) estudante %s,

                %s

                DADOS DA COBRANÇA
                Estudante: %s
                Matrícula: %s
                Referência: %s
                Descrição: %s
                Valor total: %s
                Data de vencimento: %s
                Situação: %s

                A Guia de Pagamento Académico segue em anexo e também pode ser consultada no endereço abaixo:
                %s

                %s

                Caso o pagamento já tenha sido realizado, pedimos que envie o respetivo comprovativo para validação e atualização da sua situação financeira.

                Atenciosamente,
                Secretaria Financeira
                %s
                Powered by SecretáriaPay
                """.formatted(
                safe(recipient, "-"),
                safe(from, "-"),
                safe(cc, "-"),
                emailSubject(charge, context),
                firstName(student != null ? student.getFullName() : null),
                context.openingMessage(),
                safe(student != null ? student.getFullName() : null, "-"),
                safe(student != null ? student.getStudentNumber() : null, "-"),
                reference(charge),
                safe(charge != null ? charge.getDescription() : null, "Cobrança académica"),
                money(charge != null ? charge.getTotalAmount() : null, charge != null ? charge.getCurrency() : null),
                dueDate(charge),
                context.statusLabel(),
                safe(guideUrl, "-"),
                urgencyText(context),
                safe(institutionName, "IMETRO")
        ).trim();
    }

    private String emailSubject(Charge charge, CommunicationContext context) {
        String ref = reference(charge);
        if (context.overdueDays() > 0) {
            return "Regularização financeira urgente – " + ref + " – IMETRO";
        }
        if (context.daysUntilDue() == 0) {
            return "Vencimento hoje – Guia de Pagamento Académico – " + ref;
        }
        if (context.daysUntilDue() > 0 && context.daysUntilDue() <= 3) {
            return "Lembrete de vencimento – Guia de Pagamento Académico – " + ref;
        }
        return "Guia de Pagamento Académico disponível – " + ref + " – IMETRO";
    }

    private CommunicationContext context(Charge charge) {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = charge != null ? charge.getDueDate() : null;
        long daysUntilDue = dueDate == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(today, dueDate);
        long overdueDays = dueDate == null || !dueDate.isBefore(today) ? 0 : ChronoUnit.DAYS.between(dueDate, today);

        if (overdueDays > 30) {
            return new CommunicationContext(overdueDays, daysUntilDue,
                    "Identificámos que a sua obrigação financeira permanece em dívida há mais de 30 dias.",
                    "Em dívida – regularização urgente");
        }
        if (overdueDays > 15) {
            return new CommunicationContext(overdueDays, daysUntilDue,
                    "Identificámos um pagamento em dívida que continua pendente no nosso sistema.",
                    "Em atraso");
        }
        if (overdueDays > 5) {
            return new CommunicationContext(overdueDays, daysUntilDue,
                    "Verificámos que a cobrança abaixo permanece pendente após a data de vencimento.",
                    "Pendente em atraso");
        }
        if (overdueDays > 0) {
            return new CommunicationContext(overdueDays, daysUntilDue,
                    "Verificámos que a cobrança abaixo ainda não foi regularizada.",
                    "Pendente");
        }
        if (daysUntilDue == 0) {
            return new CommunicationContext(0, 0,
                    "Informamos que hoje é o último dia para pagamento da cobrança abaixo.",
                    "Vence hoje");
        }
        if (daysUntilDue > 0 && daysUntilDue <= 3) {
            return new CommunicationContext(0, daysUntilDue,
                    "Este é um lembrete de que a sua Guia de Pagamento Académico está próxima do vencimento.",
                    "Próxima do vencimento");
        }
        return new CommunicationContext(0, daysUntilDue,
                "Informamos que a sua Guia de Pagamento Académico já se encontra disponível.",
                "Disponível para pagamento");
    }

    private String urgencyText(CommunicationContext context) {
        if (context.overdueDays() > 30) {
            return "Solicitamos a regularização imediata para evitar agravamento dos encargos, bloqueios e outros constrangimentos no acesso aos serviços académicos. Caso necessite de apoio, contacte urgentemente a Secretaria Financeira.";
        }
        if (context.overdueDays() > 15) {
            return "Regularize com urgência para evitar multas adicionais, atualização do valor em dívida e eventuais limitações no acesso aos serviços académicos, conforme o regulamento da instituição.";
        }
        if (context.overdueDays() > 5) {
            return "Solicitamos que regularize a situação com a maior brevidade possível para evitar encargos adicionais e constrangimentos académicos.";
        }
        if (context.overdueDays() > 0) {
            return "Efetue o pagamento o quanto antes para evitar multas, juros, bloqueios ou outros constrangimentos no acesso aos serviços académicos.";
        }
        if (context.daysUntilDue() == 0) {
            return "Recomendamos que efetue o pagamento ainda hoje para evitar a incidência de multas, juros e outros constrangimentos académicos.";
        }
        return "Solicitamos que o pagamento seja efetuado até à data de vencimento, a fim de manter a sua situação financeira regular e evitar multas, bloqueios ou outros constrangimentos no acesso aos serviços académicos.";
    }

    private String reference(Charge charge) {
        if (charge == null) return "-";
        if (charge.getReferenceMonth() != null && !charge.getReferenceMonth().isBlank()) return charge.getReferenceMonth();
        return safe(charge.getChargeCode(), "-");
    }

    private String dueDate(Charge charge) {
        return charge != null && charge.getDueDate() != null ? charge.getDueDate().format(DATE_FORMATTER) : "-";
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
        return "AOA".equalsIgnoreCase(safeCurrency)
                ? formatter.format(safeAmount) + " Kz"
                : formatter.format(safeAmount) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record CommunicationContext(long overdueDays, long daysUntilDue, String openingMessage, String statusLabel) {}
}
