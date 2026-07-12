package com.secretariapay.api.service;

import com.secretariapay.api.config.SecretariaPayNotificationProperties;
import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final GuidePdfService guidePdfService;
    private final RestClient restClient;

    public FallbackNotificationService(
            SecretariaPayNotificationProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            GuidePdfService guidePdfService
    ) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
        this.guidePdfService = guidePdfService;
        this.restClient = RestClient.builder().build();
    }

    public List<Map<String, Object>> listChannels() {
        return List.of(
                channel("WHATSAPP", "WhatsApp institucional", "ACTIVE", "Produção", "Envio real de guias e mensagens para contacto oficial cadastrado."),
                channel("EMAIL", "E-mail institucional", "ACTIVE", properties.isEmailEnabled() ? "Produção" : "Preparado", "Fallback para envio de guia por e-mail oficial cadastrado com PDF em anexo."),
                channel("SMS", "SMS com link da guia", "ACTIVE", properties.isSmsEnabled() ? providerLabel() : "Preparado", "Fallback para aluno sem WhatsApp, usando telefone oficial cadastrado."),
                channel("PDF", "Guia pública em PDF", "ACTIVE", "Produção", "Link público seguro por código da cobrança.")
        );
    }

    public Map<String, Object> sendGuideByEmail(GuideFallbackRequest request) {
        String email = clean(request.getEmail());
        if (email.isBlank()) {
            return delivery("EMAIL", "FAILED", false, "E-mail oficial do estudante não informado.", request);
        }

        if (!properties.isEmailEnabled()) {
            return delivery("EMAIL", "PREPARED", false, "E-mail institucional preparado. Ative SECRETARIAPAY_NOTIFICATIONS_EMAIL_ENABLED e configure SPRING_MAIL_* no servidor para envio real.", request);
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return delivery("EMAIL", "FAILED", false, "JavaMailSender não está disponível. Configure o SMTP institucional no ambiente.", request);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getEmailFrom());
            helper.setTo(email);

            String cc = clean(properties.getEmailCc());
            if (!cc.isBlank()) {
                helper.setCc(cc);
            }

            String guideCode = safe(request.getGuideCode(), "SecretariaPay");
            String attachmentName = buildAttachmentName(request);
            helper.setSubject("Guia de pagamento - " + guideCode);
            helper.setText(buildPlainEmailBody(request), buildHtmlEmailBody(request));

            byte[] pdf = resolveAttachmentPdf(request);
            ByteArrayDataSource pdfSource = new ByteArrayDataSource(pdf, "application/pdf");
            helper.addAttachment(attachmentName, pdfSource);

            mailSender.send(message);

            Map<String, Object> response = delivery("EMAIL", "SENT", true, "Guia enviada por e-mail institucional com PDF oficial em anexo.", request);
            response.put("attachment", attachmentName);
            return response;
        } catch (Exception ex) {
            return delivery("EMAIL", "FAILED", false, "Falha ao enviar e-mail institucional: " + ex.getMessage(), request);
        }
    }

    public Map<String, Object> sendGuideBySms(GuideFallbackRequest request) {
        String phone = onlyDigits(request.getPhoneNumber());
        if (phone.isBlank()) {
            return delivery("SMS", "FAILED", false, "Telefone oficial do estudante não informado.", request);
        }

        String smsText = buildSmsText(request);
        if (!properties.isSmsEnabled()) {
            Map<String, Object> response = delivery("SMS", "PREPARED", false, "SMS preparado. Ative SECRETARIAPAY_NOTIFICATIONS_SMS_ENABLED e configure o provedor no servidor para envio real.", request);
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

        String apiKey = clean(properties.getSmsApiKey());
        if (!apiKey.isBlank()) {
            payload.put("apiKey", apiKey);
        }

        try {
            restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> response = delivery("SMS", "SENT", true, "SMS com link da guia enviado.", request);
            response.put("smsText", smsText);
            return response;
        } catch (Exception ex) {
            Map<String, Object> response = delivery("SMS", "FAILED", false, "Falha ao enviar SMS: " + ex.getMessage(), request);
            response.put("smsText", smsText);
            return response;
        }
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
        response.put("pdfUrl", resolvePdfUrl(request));
        response.put("studentName", request.getStudentName());
        response.put("studentNumber", request.getStudentNumber());
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    private String buildPlainEmailBody(GuideFallbackRequest request) {
        return "Olá, " + safe(request.getStudentName(), "estudante") + ".\n\n"
                + "Segue a sua guia de pagamento emitida pelo SecretáriaPay Académico.\n\n"
                + "Código da guia: " + safe(request.getGuideCode(), "-") + "\n"
                + "Valor: " + money(request.getAmount(), request.getCurrency()) + "\n"
                + "Vencimento: " + safe(request.getDueDate(), "-") + "\n"
                + "Link da guia: " + resolveGuideUrl(request) + "\n"
                + "PDF da guia: " + resolvePdfUrl(request) + "\n\n"
                + safe(request.getMessage(), "Após o pagamento, envie o comprovativo pelo WhatsApp institucional ou apresente-o à DCR para validação.")
                + "\n\nAtenciosamente,\n"
                + properties.getEmailSenderName();
    }

    private String buildHtmlEmailBody(GuideFallbackRequest request) {
        String studentName = escapeHtml(safe(request.getStudentName(), "estudante"));
        String guideCode = escapeHtml(safe(request.getGuideCode(), "-"));
        String amount = escapeHtml(money(request.getAmount(), request.getCurrency()));
        String dueDate = escapeHtml(safe(request.getDueDate(), "-"));
        String guideUrl = escapeHtml(resolveGuideUrl(request));
        String pdfUrl = escapeHtml(resolvePdfUrl(request));
        String customMessage = escapeHtml(safe(request.getMessage(), "Após o pagamento, envie o comprovativo pelo WhatsApp institucional ou apresente-o à DCR para validação."));
        String sender = escapeHtml(properties.getEmailSenderName());

        return "<!doctype html>"
                + "<html><body style='margin:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a;'>"
                + "<div style='max-width:680px;margin:0 auto;padding:24px;'>"
                + "<div style='background:#061936;border-radius:24px 24px 0 0;padding:26px;color:#fff;'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr>"
                + "<td style='width:58px;vertical-align:middle;'><div style='height:48px;width:48px;border-radius:14px;background:#d4a336;color:#061936;font-weight:900;font-size:18px;line-height:48px;text-align:center;'>SP</div></td>"
                + "<td style='vertical-align:middle;'><div style='font-size:24px;font-weight:900;letter-spacing:-.5px;'>SecretáriaPay</div><div style='font-size:12px;font-weight:800;color:#d4a336;letter-spacing:2px;'>ACADÉMICO</div></td>"
                + "</tr></table>"
                + "<p style='margin:20px 0 0;color:#cbd5e1;font-size:14px;'>Gestão inteligente de pagamentos académicos</p>"
                + "</div>"
                + "<div style='background:#ffffff;padding:28px;border:1px solid #e2e8f0;border-top:none;'>"
                + "<h1 style='margin:0 0 12px;font-size:24px;color:#061936;'>Guia de pagamento emitida</h1>"
                + "<p style='margin:0 0 22px;font-size:15px;line-height:1.6;color:#475569;'>Olá, <strong>" + studentName + "</strong>. Segue a sua guia de pagamento emitida pelo SecretáriaPay Académico. O PDF institucional oficial está anexado neste e-mail.</p>"
                + "<div style='border-radius:18px;background:#f8fafc;border:1px solid #e2e8f0;padding:18px;margin:18px 0;'>"
                + "<p style='margin:0 0 10px;font-size:13px;color:#64748b;'>Código da guia</p><p style='margin:0 0 18px;font-size:22px;font-weight:900;color:#061936;'>" + guideCode + "</p>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='font-size:14px;color:#334155;'>"
                + "<tr><td style='padding:8px 0;color:#64748b;'>Valor</td><td style='padding:8px 0;text-align:right;font-weight:800;color:#061936;'>" + amount + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#64748b;'>Vencimento</td><td style='padding:8px 0;text-align:right;font-weight:800;color:#061936;'>" + dueDate + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#64748b;'>Estado</td><td style='padding:8px 0;text-align:right;font-weight:800;color:#b45309;'>Pendente DCR</td></tr>"
                + "</table></div>"
                + "<p style='margin:18px 0;font-size:14px;line-height:1.7;color:#475569;'>" + customMessage + "</p>"
                + "<p style='margin:24px 0;'><a href='" + guideUrl + "' style='display:inline-block;background:#061936;color:#ffffff;text-decoration:none;border-radius:14px;padding:14px 18px;font-weight:900;'>Abrir guia online</a> <a href='" + pdfUrl + "' style='display:inline-block;background:#d4a336;color:#061936;text-decoration:none;border-radius:14px;padding:14px 18px;font-weight:900;margin-left:8px;'>Abrir PDF</a></p>"
                + "<div style='border-left:4px solid #d4a336;background:#fffbeb;padding:14px 16px;border-radius:12px;color:#92400e;font-size:13px;line-height:1.6;'>Este documento não substitui o recibo institucional. O recibo será emitido somente após confirmação e validação pela DCR.</div>"
                + "<p style='margin:26px 0 0;font-size:14px;color:#475569;'>Atenciosamente,<br><strong>" + sender + "</strong></p>"
                + "</div>"
                + "<div style='background:#061936;color:#cbd5e1;border-radius:0 0 24px 24px;padding:18px 26px;font-size:12px;'>SecretáriaPay Académico · TRIA Company · IMETRO</div>"
                + "</div></body></html>";
    }

    private String buildSmsText(GuideFallbackRequest request) {
        return "SecretariaPay: guia " + safe(request.getGuideCode(), "-")
                + ", valor " + money(request.getAmount(), request.getCurrency())
                + ", venc. " + safe(request.getDueDate(), "-")
                + ". Link: " + resolveGuideUrl(request);
    }

    private byte[] resolveAttachmentPdf(GuideFallbackRequest request) {
        String pdfUrl = resolvePdfUrl(request);
        if (!pdfUrl.isBlank()) {
            try {
                byte[] officialPdf = restClient.get()
                        .uri(pdfUrl)
                        .retrieve()
                        .body(byte[].class);
                if (officialPdf != null && officialPdf.length > 0) {
                    return officialPdf;
                }
            } catch (Exception ignored) {
            }
        }
        return guidePdfService.generateGuidePdf(request);
    }

    private String buildAttachmentName(GuideFallbackRequest request) {
        String guideCode = sanitizeFilePart(safe(request.getGuideCode(), "SecretariaPay"));
        String studentNumber = sanitizeFilePart(safe(request.getStudentNumber(), "estudante"));
        if (guideCode.startsWith("BORD") || guideCode.contains("RECEIPT") || guideCode.contains("RECIBO")) {
            return "Comprovativo_Pagamentos_" + studentNumber + "_" + guideCode + ".pdf";
        }
        return "Guia_Pagamento_Academico_" + studentNumber + "_" + guideCode + ".pdf";
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

    private String resolvePdfUrl(GuideFallbackRequest request) {
        String provided = clean(request.getGuideUrl());
        if (!provided.isBlank() && (provided.contains("/pdf") || provided.toLowerCase(Locale.ROOT).endsWith(".pdf"))) {
            return provided;
        }

        String guideCode = clean(request.getGuideCode());
        if (guideCode.isBlank()) {
            return "";
        }
        return "https://secretariapay-api.paixaoangola.com/api/v1/public/guides/" + guideCode + "/pdf";
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

    private String sanitizeFilePart(String value) {
        String sanitized = clean(value)
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    private String escapeHtml(String value) {
        return safe(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
