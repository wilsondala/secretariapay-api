package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
public class ReceiptDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptDeliveryService.class);

    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    public ReceiptDeliveryService(WhatsAppCloudApiClient whatsAppCloudApiClient) {
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
    }

    public void sendAfterGateway(Charge charge, ReceiptResponse receipt) {
        send(charge, receipt, "Pagamento confirmado via AppyPay / IMETRO.", "");
    }

    public void sendAfterDcrApproval(Charge charge, ReceiptResponse receipt, String phoneFallback) {
        send(charge, receipt, "Pagamento confirmado pela DCR / IMETRO.", phoneFallback);
    }

    private void send(Charge charge, ReceiptResponse receipt, String title, String phoneFallback) {
        if (charge == null) {
            log.warn("Recibo não enviado: cobrança nula.");
            return;
        }
        if (receipt == null) {
            log.warn("Recibo não enviado para cobrança {}: recibo nulo.", charge.getChargeCode());
            return;
        }
        if (charge.getStudent() == null) {
            log.warn("Recibo {} não enviado: cobrança {} sem estudante associado.", receipt.getReceiptCode(), charge.getChargeCode());
            return;
        }

        Student student = charge.getStudent();
        String pdfUrl = firstNonBlank(receipt.getPdfUrl(), receipt.getValidationUrl());
        if (pdfUrl.isBlank()) {
            log.warn("Recibo {} não enviado: sem URL PDF/validação.", receipt.getReceiptCode());
            return;
        }

        String phone = firstNonBlank(student.getWhatsapp(), student.getPhone(), phoneFallback);
        if (phone.isBlank()) {
            log.warn("Recibo {} não enviado: estudante {} sem WhatsApp/telefone.", receipt.getReceiptCode(), student.getStudentNumber());
            return;
        }

        String text = buildMessage(title, student, charge, receipt, pdfUrl);
        String fileName = "recibo-secretariapay-" + firstNonBlank(receipt.getReceiptCode(), charge.getChargeCode(), "pagamento") + ".pdf";

        log.info("Enviando recibo {} por WhatsApp para estudante {} telefone {}.", receipt.getReceiptCode(), student.getStudentNumber(), maskPhone(phone));
        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, text);

        if (result != null && result.isSuccess()) {
            log.info("Recibo {} enviado por WhatsApp como documento. providerMessageId={}", receipt.getReceiptCode(), result.getProviderMessageId());
            return;
        }

        log.warn("Falha ao enviar recibo {} como documento. Motivo: {}. Tentando texto com link.", receipt.getReceiptCode(), result != null ? result.getErrorMessage() : "resultado nulo");
        WhatsAppCloudSendResult fallback = whatsAppCloudApiClient.sendText(phone, text + "\n\nLink do recibo PDF:\n" + pdfUrl);

        if (fallback != null && fallback.isSuccess()) {
            log.info("Recibo {} enviado por WhatsApp como texto. providerMessageId={}", receipt.getReceiptCode(), fallback.getProviderMessageId());
        } else {
            log.warn("Falha ao enviar recibo {} também como texto. Motivo: {}", receipt.getReceiptCode(), fallback != null ? fallback.getErrorMessage() : "resultado nulo");
        }
    }

    private String buildMessage(String title, Student student, Charge charge, ReceiptResponse receipt, String pdfUrl) {
        return ("""
                ✅ %s

                Estudante: %s
                Matrícula: %s
                Cobrança: %s
                Valor: %s
                Recibo: %s

                O recibo institucional segue em PDF.
                Validação pública:
                %s

                SecretáriaPay Académico
                """).formatted(
                title,
                safe(student.getFullName()),
                firstNonBlank(student.getStudentNumber(), "-"),
                firstNonBlank(charge.getChargeCode(), "-"),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                firstNonBlank(receipt.getReceiptCode(), "-"),
                firstNonBlank(receipt.getValidationUrl(), pdfUrl)
        ).trim();
    }

    private String maskPhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 5) return "***";
        return digits.substring(0, Math.min(5, digits.length())) + "***" + digits.substring(Math.max(0, digits.length() - 2));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private String safe(String value) { return value == null ? "" : value; }

    private String formatMoney(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-AO"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(safeAmount) + " " + firstNonBlank(currency, "Kz");
    }
}
