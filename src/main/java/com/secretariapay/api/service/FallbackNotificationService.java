package com.secretariapay.api.service;

import com.secretariapay.api.config.SecretariaPayNotificationProperties;
import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FallbackNotificationService {

    private final SecretariaPayNotificationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestClient restClient;

    public FallbackNotificationService(
            SecretariaPayNotificationProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider
    ) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
        this.restClient = RestClient.builder().build();
    }

    public List<Map<String, Object>> listChannels() {
        return List.of(
                channel(
                        "WHATSAPP",
                        "WhatsApp institucional",
                        "ACTIVE",
                        "Produção",
                        "Envio real de guias e mensagens para contacto oficial cadastrado."
                ),
                channel(
                        "EMAIL",
                        "E-mail institucional",
                        "ACTIVE",
                        properties.isEmailEnabled() ? "Produção" : "Preparado",
                        "Fallback para envio de guia por e-mail oficial cadastrado."
                ),
                channel(
                        "SMS",
                        "SMS com link da guia",
                        "ACTIVE",
                        properties.isSmsEnabled() ? providerLabel() : "Preparado",
                        "Fallback para aluno sem WhatsApp, usando telefone oficial cadastrado."
                ),
                channel(
                        "PDF",
                        "Guia pública em PDF",
                        "ACTIVE",
                        "Produção",
                        "Link público seguro por código da cobrança."
                )
        );
    }

    public Map<String, Object> sendGuideByEmail(GuideFallbackRequest request) {
        String email = clean(request.getEmail());
        if (email.isBlank()) {
            return delivery("EMAIL", "FAILED", false, "E-mail oficial do estudante não informado.", request);
        }

        if (!properties.isEmailEnabled()) {
            return delivery("EMAIL", "PREPARED", false, "E-mail institucional preparado. Ative SECRETARIAPAY_EMAIL_ENABLED e configure SPRING_MAIL_* no servidor para envio real.", request);
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return delivery("EMAIL", "FAILED", false, "JavaMailSender não está disponível. Configure o SMTP institucional no ambiente.", request);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmailFrom());
        message.setTo(email);

        String cc = clean(properties.getEmailCc());
        if (!cc.isBlank()) {
            message.setCc(cc);
        }

        message.setSubject("Guia de pagamento - " + safe(request.getGuideCode(), "SecretáriaPay"));
        message.setText(buildEmailBody(request));
        mailSender.send(message);

        return delivery("EMAIL", "SENT", true, "Guia enviada por e-mail institucional.", request);
    }

    public Map<String, Object> sendGuideBySms(GuideFallbackRequest request) {
        String phone = onlyDigits(request.getPhoneNumber());
        if (phone.isBlank()) {
            return delivery("SMS", "FAILED", false, "Telefone oficial do estudante não informado.", request);
        }

        String smsText = buildSmsText(request);
        if (!properties.isSmsEnabled()) {
            Map<String, Object> response = delivery("SMS", "PREPARED", false, "SMS preparado. Ative SECRETARIAPAY_SMS_ENABLED e configure o provedor no servidor para envio real.", request);
            response.put("smsText", smsText);
            return response;
        }

        String apiUrl = clean(properties.getSmsApiUrl());
        if (apiUrl.isBlank()) {
            Map<String, Object> response = delivery("SMS", "FAILED", false, "URL do provedor SMS não configurada.", request);
            response.put("smsText", smsText);
            return response;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", phone);
        payload.put("from", properties.getSmsSenderId());
        payload.put("message", smsText);
        payload.put("provider", properties.getSmsProvider());

        restClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> response = delivery("SMS", "SENT", true, "SMS com link da guia enviado.", request);
        response.put("smsText", smsText);
        return response;
    }

    private Map<String, Object> channel(String code, String name, String status, String mode, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", name);
        item.put("status", status);
        item.put("mode", mode);
        item.put("description", description);
        item.put("updatedAt", LocalDateTime.now().toString());
        return item;
    }

    private Map<String, Object> delivery(String channel, String status, boolean sent, String message, GuideFallbackRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("channel", channel);
        response.put("status", status);
        response.put("sent", sent);
        response.put("message", message);
        response.put("guideCode", request.getGuideCode());
        response.put("guideUrl", resolveGuideUrl(request));
        response.put("studentName", request.getStudentName());
        response.put("studentNumber", request.getStudentNumber());
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    private String buildEmailBody(GuideFallbackRequest request) {
        return "Olá, " + safe(request.getStudentName(), "estudante") + ".\n\n"
                + "Segue a sua guia de pagamento emitida pelo SecretáriaPay Académico.\n\n"
                + "Código da guia: " + safe(request.getGuideCode(), "-") + "\n"
                + "Valor: " + money(request.getAmount(), request.getCurrency()) + "\n"
                + "Vencimento: " + safe(request.getDueDate(), "-") + "\n"
                + "Link da guia: " + resolveGuideUrl(request) + "\n\n"
                + safe(request.getMessage(), "Após o pagamento, envie o comprovativo pelo WhatsApp institucional ou apresente-o à DCR para validação.")
                + "\n\nAtenciosamente,\n"
                + properties.getEmailSenderName();
    }

    private String buildSmsText(GuideFallbackRequest request) {
        return "SecretariaPay: guia " + safe(request.getGuideCode(), "-")
                + ", valor " + money(request.getAmount(), request.getCurrency())
                + ", venc. " + safe(request.getDueDate(), "-")
                + ". Link: " + resolveGuideUrl(request);
    }

    private String resolveGuideUrl(GuideFallbackRequest request) {
        String provided = clean(request.getGuideUrl());
        if (!provided.isBlank()) {
            return provided;
        }

        String baseUrl = clean(properties.getPublicGuideBaseUrl());
        String guideCode = clean(request.getGuideCode());
        if (baseUrl.isBlank() || guideCode.isBlank()) {
            return "";
        }

        return baseUrl.replaceAll("/+$", "") + "/" + guideCode;
    }

    private String providerLabel() {
        String provider = clean(properties.getSmsProvider());
        return provider.isBlank() || provider.equalsIgnoreCase("MOCK") ? "Produção" : provider;
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = safe(currency, "AOA");
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-AO"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(safeAmount) + " " + ("AOA".equalsIgnoreCase(safeCurrency) ? "Kz" : safeCurrency);
    }

    private String onlyDigits(String value) {
        return clean(value).replaceAll("[^0-9]", "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
