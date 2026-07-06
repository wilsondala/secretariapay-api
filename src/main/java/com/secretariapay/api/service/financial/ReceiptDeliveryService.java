package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
public class ReceiptDeliveryService {

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
        if (charge == null || receipt == null || charge.getStudent() == null) return;
        Student student = charge.getStudent();
        String pdfUrl = firstNonBlank(receipt.getPdfUrl(), receipt.getValidationUrl());
        if (pdfUrl.isBlank()) return;
        String phone = firstNonBlank(student.getWhatsapp(), student.getPhone(), phoneFallback);
        if (phone.isBlank()) return;
        String text = buildMessage(title, student, charge, receipt, pdfUrl);
        String fileName = "recibo-secretariapay-" + firstNonBlank(receipt.getReceiptCode(), charge.getChargeCode(), "pagamento") + ".pdf";
        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, text);
        if (result == null || !result.isSuccess()) {
            whatsAppCloudApiClient.sendText(phone, text + "\n\nLink do recibo PDF:\n" + pdfUrl);
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
