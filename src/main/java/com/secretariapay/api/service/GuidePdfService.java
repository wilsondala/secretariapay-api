package com.secretariapay.api.service;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class GuidePdfService {

    public byte[] generateGuidePdf(GuideFallbackRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float width = page.getMediaBox().getWidth();
                float y;
                float left = 54;

                drawHeader(content, width, left);

                content.setNonStrokingColor(212, 163, 54);
                write(content, PDType1Font.HELVETICA_BOLD, 15, left, 755, safe(request.getGuideCode(), "GUIA-TESTE"));

                y = 700;
                content.setNonStrokingColor(6, 25, 54);
                write(content, PDType1Font.HELVETICA_BOLD, 18, left, y, "Dados do estudante");
                y -= 26;
                writeLine(content, left, y, "Nome", safe(request.getStudentName(), "Estudante teste"));
                y -= 22;
                writeLine(content, left, y, "Número", safe(request.getStudentNumber(), "TESTE-001"));

                y -= 48;
                write(content, PDType1Font.HELVETICA_BOLD, 18, left, y, "Dados da cobrança");
                y -= 26;
                writeLine(content, left, y, "Valor", money(request.getAmount(), request.getCurrency()));
                y -= 22;
                writeLine(content, left, y, "Moeda", safe(request.getCurrency(), "AOA"));
                y -= 22;
                writeLine(content, left, y, "Vencimento", formatDate(request.getDueDate()));
                y -= 22;
                writeLine(content, left, y, "Estado", "Pendente de pagamento / validação DCR");

                y -= 48;
                write(content, PDType1Font.HELVETICA_BOLD, 18, left, y, "Orientação ao estudante");
                y -= 26;
                writeParagraph(content, left, y, 490,
                        safe(request.getMessage(), "Após o pagamento, envie o comprovativo pelo WhatsApp institucional ou apresente-o à DCR para validação."));

                content.setNonStrokingColor(245, 247, 250);
                content.addRect(left, 115, 490, 88);
                content.fill();

                content.setNonStrokingColor(6, 25, 54);
                write(content, PDType1Font.HELVETICA_BOLD, 13, left + 18, 177, "Regra de validação");
                write(content, PDType1Font.HELVETICA, 10, left + 18, 156, "Este documento não substitui o recibo institucional.");
                write(content, PDType1Font.HELVETICA, 10, left + 18, 140, "O recibo será emitido somente após confirmação e validação pela DCR.");

                drawFooter(content, left);
            }

            document.save(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar PDF da guia: " + ex.getMessage(), ex);
        }
    }

    public GuideFallbackRequest buildDemoGuide(String guideCode) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName("Wilson Dala");
        request.setStudentNumber("TESTE-SECRETARIAPAY-001");
        request.setGuideCode(safe(guideCode, "GUIA-WILSON-001"));
        request.setGuideUrl("https://painel-secretariapay.paixaoangola.com/guias/" + safe(guideCode, "GUIA-WILSON-001"));
        request.setAmount(new BigDecimal("45000"));
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 31));
        request.setMessage("Teste real de guia pública com PDF pelo SecretáriaPay Académico.");
        return request;
    }

    private void drawHeader(PDPageContentStream content, float width, float left) throws Exception {
        content.setNonStrokingColor(6, 25, 54);
        content.addRect(0, 745, width, 97);
        content.fill();

        content.setNonStrokingColor(212, 163, 54);
        content.addRect(left, 785, 42, 42);
        content.fill();

        content.setNonStrokingColor(6, 25, 54);
        write(content, PDType1Font.HELVETICA_BOLD, 16, left + 10, 800, "SP");

        content.setNonStrokingColor(255, 255, 255);
        write(content, PDType1Font.HELVETICA_BOLD, 23, left + 55, 810, "SecretáriaPay");
        content.setNonStrokingColor(212, 163, 54);
        write(content, PDType1Font.HELVETICA_BOLD, 12, left + 55, 792, "ACADÉMICO");
        content.setNonStrokingColor(226, 232, 240);
        write(content, PDType1Font.HELVETICA, 10, left + 55, 775, "Gestão inteligente de pagamentos académicos");

        content.setNonStrokingColor(255, 255, 255);
        write(content, PDType1Font.HELVETICA_BOLD, 12, width - 196, 808, "GUIA DE PAGAMENTO");
        content.setNonStrokingColor(203, 213, 225);
        write(content, PDType1Font.HELVETICA, 9, width - 196, 790, "Documento institucional com validação DCR");
    }

    private void drawFooter(PDPageContentStream content, float left) throws Exception {
        content.setNonStrokingColor(6, 25, 54);
        write(content, PDType1Font.HELVETICA_BOLD, 9, left, 82, "SecretáriaPay Académico");
        content.setNonStrokingColor(100, 116, 139);
        write(content, PDType1Font.HELVETICA, 9, left, 68,
                "Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " - TRIA Company / IMETRO.");
    }

    private void writeLine(PDPageContentStream content, float x, float y, String label, String value) throws Exception {
        write(content, PDType1Font.HELVETICA_BOLD, 11, x, y, label + ":");
        write(content, PDType1Font.HELVETICA, 11, x + 105, y, value);
    }

    private void writeParagraph(PDPageContentStream content, float x, float y, float maxWidth, String text) throws Exception {
        String[] words = safe(text, "-").split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float size = PDType1Font.HELVETICA.getStringWidth(candidate) / 1000 * 11;
            if (size > maxWidth && !line.isEmpty()) {
                write(content, PDType1Font.HELVETICA, 11, x, currentY, line.toString());
                line = new StringBuilder(word);
                currentY -= 17;
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            write(content, PDType1Font.HELVETICA, 11, x, currentY, line.toString());
        }
    }

    private void write(PDPageContentStream content, PDType1Font font, int fontSize, float x, float y, String text) throws Exception {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(cleanForPdf(text));
        content.endText();
    }

    private String money(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        String safeCurrency = safe(currency, "AOA");
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-AO"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(safeAmount) + " " + ("AOA".equalsIgnoreCase(safeCurrency) ? "Kz" : safeCurrency);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String safe(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private String cleanForPdf(String value) {
        return safe(value, "-")
                .replace("•", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("’", "'");
    }
}
