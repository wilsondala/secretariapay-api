package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.academic.Student;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Service
public class AcademicServiceOrderEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AcademicServiceOrderEmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final String cc;
    private final String senderName;

    public AcademicServiceOrderEmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${SECRETARIAPAY_NOTIFICATIONS_EMAIL_ENABLED:${SECRETARIAPAY_EMAIL_ENABLED:false}}") boolean enabled,
            @Value("${SECRETARIAPAY_NOTIFICATIONS_EMAIL_FROM:${SECRETARIAPAY_EMAIL_FROM:dcr_pay@imetroangola.com}}") String from,
            @Value("${SECRETARIAPAY_NOTIFICATIONS_EMAIL_CC:${SECRETARIAPAY_EMAIL_CC:}}") String cc,
            @Value("${SECRETARIAPAY_NOTIFICATIONS_EMAIL_SENDER_NAME:SecretáriaPay Académico — IMETRO}") String senderName
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = trimToNull(from);
        this.cc = trimToNull(cc);
        this.senderName = firstNonBlank(senderName, "SecretáriaPay Académico — IMETRO");
    }

    /**
     * Envia a comunicação complementar de levantamento. A falha do canal de
     * e-mail não deve reverter o WhatsApp já confirmado nem provocar reenvio
     * duplicado do canal principal.
     */
    public DeliveryResult sendReadyForPickup(AcademicServiceOrder order) {
        if (order == null || order.getStudent() == null) {
            return DeliveryResult.skipped("SKIPPED_INVALID_ORDER", null,
                    "Pedido ou estudante não informado para a notificação por e-mail.");
        }

        Student student = order.getStudent();
        String recipient = firstNonBlank(student.getEmail(), student.getGuardianEmail());

        if (!enabled) {
            log.info("Notificação por e-mail desativada para o pedido {}.", order.getOrderCode());
            return DeliveryResult.skipped("SKIPPED_DISABLED", recipient,
                    "O envio institucional por e-mail está desativado.");
        }

        if (isBlank(recipient)) {
            log.warn("Pedido {} sem e-mail do estudante ou responsável.", order.getOrderCode());
            return DeliveryResult.skipped("SKIPPED_NO_RECIPIENT", null,
                    "O estudante não possui e-mail cadastrado.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || hasInvalidSmtpHost(mailSender)) {
            log.warn("SMTP não configurado corretamente para o pedido {}.", order.getOrderCode());
            return DeliveryResult.skipped("SKIPPED_NOT_CONFIGURED", recipient,
                    "O servidor SMTP institucional ainda não está configurado corretamente.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            if (from != null) {
                helper.setFrom(from, senderName);
            }

            String[] ccRecipients = splitRecipients(cc);
            if (ccRecipients.length > 0) {
                helper.setCc(ccRecipients);
            }

            helper.setSubject("Documento disponível para levantamento — " + order.getOrderCode());
            helper.setText(buildBody(order), false);
            mailSender.send(message);

            log.info("Notificação de levantamento enviada por e-mail para {} no pedido {}.", recipient, order.getOrderCode());
            return DeliveryResult.sent(recipient);
        } catch (Exception exception) {
            String technicalDetail = firstNonBlank(exception.getMessage(), exception.getClass().getSimpleName());
            log.error("Falha ao enviar e-mail de levantamento do pedido {} para {}: {}",
                    order.getOrderCode(), recipient, technicalDetail, exception);
            return DeliveryResult.failed(recipient, friendlyFailureMessage(exception));
        }
    }

    private boolean hasInvalidSmtpHost(JavaMailSender mailSender) {
        if (!(mailSender instanceof JavaMailSenderImpl sender)) return false;
        String host = trimToNull(sender.getHost());
        if (host == null) return true;

        String normalized = host.toLowerCase(Locale.ROOT);
        return host.contains("<")
                || host.contains(">")
                || normalized.contains("smtp institucional")
                || normalized.contains("host smtp")
                || normalized.contains("smtp-host")
                || normalized.contains("example")
                || normalized.contains("changeme");
    }

    private String friendlyFailureMessage(Exception exception) {
        String detail = collectExceptionMessages(exception).toLowerCase(Locale.ROOT);

        if (detail.contains("unknownhost") || detail.contains("couldn't connect")
                || detail.contains("could not connect") || detail.contains("mailconnectexception")) {
            return "Não foi possível conectar ao servidor SMTP institucional. Verifique o host, a porta e a ligação de rede.";
        }
        if (detail.contains("authenticationfailed") || detail.contains("authentication failed")
                || detail.contains("535") || detail.contains("bad credentials")) {
            return "A autenticação SMTP falhou. Verifique o utilizador e a senha da conta institucional.";
        }
        if (detail.contains("sender address rejected") || detail.contains("from address")
                || detail.contains("not authorized to send")) {
            return "O servidor SMTP não autorizou o endereço remetente configurado.";
        }
        if (detail.contains("recipient address rejected") || detail.contains("invalid addresses")) {
            return "O endereço de e-mail do estudante ou responsável foi rejeitado pelo servidor SMTP.";
        }
        return "Não foi possível enviar o e-mail de levantamento. Verifique a configuração SMTP institucional.";
    }

    private String collectExceptionMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (!messages.isEmpty()) messages.append(' ');
                messages.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            } else {
                if (!messages.isEmpty()) messages.append(' ');
                messages.append(current.getClass().getSimpleName());
            }
            current = current.getCause();
            depth++;
        }
        return messages.toString();
    }

    private String buildBody(AcademicServiceOrder order) {
        Student student = order.getStudent();
        String documentName = order.getService() == null
                ? "Documento académico"
                : firstNonBlank(order.getService().getName(), "Documento académico");
        String location = firstNonBlank(order.getPhysicalLocation(), "Secretaria Académica do IMETRO");

        return """
                Caro(a) estudante,

                Informamos que o seu documento já se encontra assinado e disponível para levantamento.

                Pedido: %s
                Documento: %s
                Matrícula: %s
                Local de levantamento: %s

                Apresente um documento de identificação no momento do levantamento.

                Caso já tenha efetuado o levantamento, desconsidere esta mensagem.

                Atenciosamente,
                Secretaria Académica
                IMETRO
                """.formatted(
                order.getOrderCode(),
                documentName,
                student.getStudentNumber(),
                location
        ).trim();
    }

    private String[] splitRecipients(String value) {
        if (isBlank(value)) return new String[0];
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toArray(String[]::new);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DeliveryResult(
            boolean sent,
            String status,
            String recipient,
            String detail
    ) {
        public static DeliveryResult sent(String recipient) {
            return new DeliveryResult(true, "SENT", recipient, null);
        }

        public static DeliveryResult skipped(String status, String recipient, String detail) {
            return new DeliveryResult(false, status, recipient, detail);
        }

        public static DeliveryResult failed(String recipient, String detail) {
            return new DeliveryResult(false, "FAILED", recipient, detail);
        }
    }
}
