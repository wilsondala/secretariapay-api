package com.secretariapay.api.service.financial;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReceiptPdfService {

    private static final Color NAVY = new Color(4, 34, 87);
    private static final Color NAVY_2 = new Color(7, 56, 132);
    private static final Color GREEN = new Color(27, 139, 62);
    private static final Color GREEN_LIGHT = new Color(235, 249, 239);
    private static final Color GOLD = new Color(218, 166, 38);
    private static final Color LIGHT = new Color(248, 250, 253);
    private static final Color BORDER = new Color(194, 207, 226);
    private static final Color MUTED = new Color(76, 91, 113);
    private static final Color ORANGE = new Color(242, 120, 24);
    private static final Color PURPLE = new Color(112, 69, 190);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReceiptRepository receiptRepository;
    private final ReceiptAuthenticityService authenticityService;

    public ReceiptPdfService(ReceiptRepository receiptRepository, ReceiptAuthenticityService authenticityService) {
        this.receiptRepository = receiptRepository;
        this.authenticityService = authenticityService;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(UUID receiptId) {
        Receipt anchor = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));

        Student student = anchor.getCharge().getStudent();
        List<Receipt> receipts = receiptRepository
                .findByChargeStudentIdAndStatusOrderByChargePaidAtAsc(student.getId(), ReceiptStatus.VALID);

        if (receipts.isEmpty()) {
            receipts = List.of(anchor);
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(document, page, content, anchor, receipts, student);
            }

            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o comprovativo oficial.", exception);
        }
    }

    private void draw(PDDocument document, PDPage page, PDPageContentStream content,
                      Receipt anchor, List<Receipt> receipts, Student student) throws Exception {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float margin = 18;

        fill(content, Color.WHITE, 0, 0, width, height);
        stroke(content, NAVY, margin, margin, width - 2 * margin, height - 2 * margin, 0.8f);

        drawHeader(document, content, anchor, width, height);
        drawStudentSummary(content, student, width, height);

        float tableBottom = drawPaymentsTable(content, receipts, width, height);
        drawValidationArea(document, content, anchor, width, tableBottom);
        drawFooter(content, width);
    }

    private void drawHeader(PDDocument document, PDPageContentStream content,
                            Receipt anchor, float width, float height) throws Exception {
        image(document, content, "static/assets/imetro.png", 28, height - 91, 132, 68);
        image(document, content, "static/branding/secretariapay-logo.png", width - 215, height - 87, 184, 58);

        center(content, "COMPROVATIVO DE PAGAMENTOS", 21, width / 2, height - 51, NAVY, true);
        line(content, GOLD, width / 2 - 102, height - 65, width / 2 + 102, height - 65, 1.2f);
        fill(content, GOLD, width / 2 - 2, height - 67, 4, 4);

        center(content,
                "Emitido em: " + DATE_TIME.format(anchor.getIssuedAt()) + "  •  Comprovativo Nº " + safe(anchor.getReceiptCode()),
                9.5f, width / 2, height - 83, NAVY, false);
    }

    private void drawStudentSummary(PDPageContentStream content, Student student,
                                    float width, float height) throws Exception {
        float x = 32;
        float y = height - 106;
        float boxHeight = 69;
        float boxWidth = width - 64;

        roundedBox(content, x, y - boxHeight, boxWidth, boxHeight, Color.WHITE, NAVY);

        AcademicClass academicClass = student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();

        pair(content, "NOME", student.getFullName(), x + 14, y - 19, 230);
        pair(content, "MATRÍCULA", student.getStudentNumber(), x + 14, y - 39, 230);
        pair(content, "CURSO", course == null ? "-" : course.getName(), x + 14, y - 59, 230);

        pair(content, "ANO ACADÉMICO", academicClass == null ? "-" : academicClass.getAcademicYear(), x + 305, y - 19, 185);
        pair(content, "TURMA", academicClass == null ? "-" : academicClass.getName(), x + 305, y - 39, 185);
        pair(content, "TELEFONE", mask(student.getPhone()), x + 305, y - 59, 185);

        pair(content, "DOCUMENTO", mask(student.getDocumentNumber()), x + 570, y - 29, 210);
        pair(content, "E-MAIL", student.getEmail(), x + 570, y - 52, 210);
    }

    private float drawPaymentsTable(PDPageContentStream content, List<Receipt> receipts,
                                    float width, float height) throws Exception {
        float left = 32;
        float right = width - 32;
        float titleY = height - 196;

        fill(content, NAVY, left, titleY - 23, right - left, 23);
        center(content, "DETALHAMENTO DOS PAGAMENTOS", 11.5f, width / 2, titleY - 16, Color.WHITE, true);

        float headerY = titleY - 28;
        fill(content, NAVY_2, left, headerY - 26, right - left, 26);
        fill(content, GREEN, 744, headerY - 26, right - 744, 26);

        String[] headers = {
                "Nº", "DESCRIÇÃO DA COBRANÇA", "REF. PERÍODO", "REFERÊNCIA / GUIA",
                "DATA DO PAGAMENTO", "FORMA DE PAGAMENTO", "VALOR BRUTO (KZ)",
                "DESCONTOS (KZ)", "JUROS / MULTA (KZ)", "VALOR LÍQUIDO (KZ)"
        };
        float[] xPositions = {39, 78, 192, 258, 349, 450, 552, 620, 682, 752};
        float[] maxWidths = {26, 108, 62, 88, 96, 94, 64, 60, 66, 70};

        for (int i = 0; i < headers.length; i++) {
            fittedText(content, headers[i], 6.2f, 5.2f, xPositions[i], headerY - 17, maxWidths[i], Color.WHITE, true);
        }

        int maxRows = Math.min(receipts.size(), 5);
        float rowHeight = maxRows <= 3 ? 39 : 31;
        float y = headerY - 45;

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (int index = 0; index < maxRows; index++) {
            Receipt receipt = receipts.get(index);
            Charge charge = receipt.getCharge();
            BigDecimal fee = nz(charge.getFineAmount()).add(nz(charge.getInterestAmount()));

            text(content, String.valueOf(index + 1), 8, 43, y, NAVY, false);
            fittedText(content, safe(charge.getDescription()), 8, 6.5f, 78, y, 108, NAVY, false);
            fittedText(content, safe(charge.getReferenceMonth()), 8, 6.5f, 192, y, 60, NAVY, false);
            fittedText(content, safe(charge.getChargeCode()), 7.6f, 6.2f, 258, y, 86, NAVY, false);
            fittedText(content, charge.getPaidAt() == null ? "-" : DATE_TIME.format(charge.getPaidAt()), 7.4f, 6.2f, 349, y, 94, NAVY, false);

            drawPaymentMethod(content, charge, 450, y - 4);

            text(content, money(charge.getAmount()), 8, 554, y, NAVY, false);
            text(content, money(charge.getDiscountAmount()), 8, 624, y, NAVY, false);
            text(content, money(fee), 8, 690, y, NAVY, false);
            text(content, money(charge.getTotalAmount()), 8.4f, 760, y, GREEN, true);

            line(content, BORDER, left, y - 13, right, y - 13, 0.45f);
            y -= rowHeight;

            gross = gross.add(nz(charge.getAmount()));
            discounts = discounts.add(nz(charge.getDiscountAmount()));
            fees = fees.add(fee);
            total = total.add(nz(charge.getTotalAmount()));
        }

        if (receipts.size() > maxRows) {
            text(content, "+ " + (receipts.size() - maxRows) + " pagamento(s) adicional(is)", 7.2f, 78, y + 7, MUTED, true);
            y -= 16;
        }

        line(content, NAVY, left, y + 14, right, y + 14, 0.9f);
        text(content, "Nº TOTAL DE TÍTULOS:", 8, 40, y - 2, NAVY, true);
        text(content, String.valueOf(receipts.size()), 8, 138, y - 2, NAVY, false);

        text(content, "SUBTOTAL:", 8, 316, y - 2, NAVY, true);
        text(content, money(gross), 8, 462, y - 2, NAVY, false);
        text(content, "ACRÉSCIMOS:", 8, 316, y - 18, NAVY, true);
        text(content, money(fees), 8, 462, y - 18, NAVY_2, false);
        text(content, "DESCONTOS:", 8, 316, y - 34, NAVY, true);
        text(content, money(discounts), 8, 462, y - 34, Color.RED, false);

        fill(content, NAVY, 305, y - 60, 215, 22);
        text(content, "TOTAL LÍQUIDO:", 8.5f, 318, y - 53, Color.WHITE, true);
        text(content, money(total) + " KZ", 10.5f, 432, y - 53, Color.WHITE, true);

        return y - 75;
    }

    private void drawPaymentMethod(PDPageContentStream content, Charge charge, float x, float y) throws Exception {
        String method = "PAGAMENTO CONFIRMADO";
        Color color = GREEN;

        if (charge.getDescription() != null) {
            String normalized = charge.getDescription().toUpperCase(Locale.ROOT);
            if (normalized.contains("MULTICAIXA")) {
                method = "MULTICAIXA EXPRESS";
                color = ORANGE;
            } else if (normalized.contains("UNITEL")) {
                method = "UNITEL MONEY";
                color = PURPLE;
            } else if (normalized.contains("TRANSFER")) {
                method = "TRANSFERÊNCIA BANCÁRIA";
                color = GREEN;
            }
        }

        roundedBox(content, x, y - 8, 16, 16, color, color);
        center(content, method.startsWith("MULTICAIXA") ? "M" : method.startsWith("UNITEL") ? "U" : "B",
                7, x + 8, y - 2, Color.WHITE, true);
        fittedText(content, method, 6.8f, 5.8f, x + 22, y + 1, 70, color, true);
    }

    private void drawValidationArea(PDDocument document, PDPageContentStream content,
                                    Receipt anchor, float width, float top) throws Exception {
        float boxX = 32;
        float boxY = 43;
        float boxW = width - 64;
        float boxH = Math.max(105, top - boxY);

        roundedBox(content, boxX, boxY, boxW, boxH, Color.WHITE, NAVY);

        String hash = authenticityService.hash(anchor);
        String validationUrl = anchor.getValidationUrl();
        if (validationUrl == null || !validationUrl.contains("hash=")) {
            validationUrl = "https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/"
                    + anchor.getReceiptCode() + "/authentic?hash=" + hash;
        }

        BufferedImage qr = createQr(validationUrl);
        content.drawImage(LosslessFactory.createFromImage(document, qr), 44, boxY + 14, 86, 86);

        roundedBox(content, 145, boxY + 53, 30, 30, GREEN_LIGHT, GREEN);
        center(content, "V", 17, 160, boxY + 61, GREEN, true);

        text(content, "COMPROVATIVO VÁLIDO", 9.5f, 187, boxY + 81, GREEN, true);
        text(content, "Este documento comprova os pagamentos recebidos", 8, 187, boxY + 62, NAVY, false);
        text(content, "e registados pela tesouraria do IMETRO.", 8, 187, boxY + 47, NAVY, false);
        text(content, "Valide pelo QR Code ou pela página pública de consulta.", 7.6f, 187, boxY + 30, GREEN, true);
        fittedText(content, "Código de verificação: " + authenticityService.shortHash(anchor) + "-IMETRO",
                7.4f, 6.2f, 187, boxY + 15, 220, NAVY, false);

        line(content, BORDER, 420, boxY + 12, 420, boxY + boxH - 12, 0.7f);
        center(content, "RESPONSÁVEL / TESOURARIA", 8.3f, 530, boxY + 80, NAVY, true);
        center(content, "Assinado digitalmente", 11, 530, boxY + 51, new Color(45, 60, 190), false);
        line(content, NAVY, 460, boxY + 39, 600, boxY + 39, 0.55f);
        center(content, "Tesouraria IMETRO", 8, 530, boxY + 22, NAVY, false);

        line(content, BORDER, 640, boxY + 12, 640, boxY + boxH - 12, 0.7f);
        center(content, "SELO DIGITAL", 8.2f, 710, boxY + 82, new Color(118, 151, 213), true);
        stroke(content, new Color(118, 151, 213), 666, boxY + 22, 88, 62, 1.3f);
        center(content, "IMETRO", 16, 710, boxY + 51, new Color(118, 151, 213), true);
        center(content, "PAGO", 7.5f, 710, boxY + 35, GREEN, true);
        center(content, "LIQUIDADO", 7.5f, 710, boxY + 24, GREEN, true);
    }

    private void drawFooter(PDPageContentStream content, float width) throws Exception {
        line(content, NAVY, 32, 35, width - 32, 35, 0.7f);
        center(content, "Documento emitido eletronicamente pelo SecretáriaPay Académico - IMETRO", 8, width / 2, 23, NAVY, false);
        center(content, "Este documento não substitui o recibo individual do estudante.", 7.5f, width / 2, 13, NAVY, false);
    }

    private BufferedImage createQr(String value) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 600, 600, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void image(PDDocument document, PDPageContentStream content,
                       String path, float x, float y, float w, float h) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                PDImageXObject image = PDImageXObject.createFromByteArray(
                        document, resource.getInputStream().readAllBytes(), path);
                content.drawImage(image, x, y, w, h);
            }
        } catch (Exception ignored) {
        }
    }

    private void pair(PDPageContentStream content, String label, Object value,
                      float x, float y, float maxWidth) throws Exception {
        text(content, label + ":", 8, x, y, NAVY, true);
        fittedText(content, safe(value), 8, 6.4f, x + 72, y, maxWidth - 72, Color.BLACK, false);
    }

    private void fittedText(PDPageContentStream content, String value, float maxSize, float minSize,
                            float x, float y, float maxWidth, Color color, boolean bold) throws Exception {
        String safeValue = pdf(value);
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        float size = maxSize;
        while (size > minSize && font.getStringWidth(safeValue) / 1000f * size > maxWidth) {
            size -= 0.2f;
        }
        if (font.getStringWidth(safeValue) / 1000f * size > maxWidth) {
            safeValue = clipToWidth(safeValue, font, size, maxWidth);
        }
        text(content, safeValue, size, x, y, color, bold);
    }

    private String clipToWidth(String value, PDType1Font font, float size, float maxWidth) throws Exception {
        String suffix = "...";
        String candidate = value;
        while (!candidate.isEmpty() && font.getStringWidth(candidate + suffix) / 1000f * size > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate.isEmpty() ? suffix : candidate + suffix;
    }

    private void text(PDPageContentStream content, String value, float size,
                      float x, float y, Color color, boolean bold) throws Exception {
        content.beginText();
        content.setNonStrokingColor(color);
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, size);
        content.newLineAtOffset(x, y);
        content.showText(pdf(value));
        content.endText();
    }

    private void center(PDPageContentStream content, String value, float size,
                        float centerX, float y, Color color, boolean bold) throws Exception {
        String safeValue = pdf(value);
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        float textWidth = font.getStringWidth(safeValue) / 1000f * size;
        text(content, safeValue, size, centerX - textWidth / 2, y, color, bold);
    }

    private void fill(PDPageContentStream content, Color color,
                      float x, float y, float w, float h) throws Exception {
        content.setNonStrokingColor(color);
        content.addRect(x, y, w, h);
        content.fill();
    }

    private void stroke(PDPageContentStream content, Color color,
                        float x, float y, float w, float h, float lineWidth) throws Exception {
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.addRect(x, y, w, h);
        content.stroke();
    }

    private void roundedBox(PDPageContentStream content, float x, float y, float w, float h,
                            Color fillColor, Color strokeColor) throws Exception {
        fill(content, fillColor, x, y, w, h);
        stroke(content, strokeColor, x, y, w, h, 0.7f);
    }

    private void line(PDPageContentStream content, Color color,
                      float x1, float y1, float x2, float y2, float lineWidth) throws Exception {
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(nz(value));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "-";
        if (value.length() < 7) return "***";
        return value.substring(0, Math.min(4, value.length())) + " *** *** " + value.substring(value.length() - 3);
    }

    private String pdf(String value) {
        return safe(value)
                .replace("•", "|")
                .replace("–", "-")
                .replace("—", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("’", "'")
                .replace("…", "...")
                .replace("✓", "V")
                .replaceAll("[^\\x20-\\x7EÀ-ÖØ-öø-ÿ]", "");
    }
}
