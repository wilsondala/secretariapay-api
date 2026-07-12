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
    private static final Color NAVY = new Color(4, 42, 104);
    private static final Color NAVY_2 = new Color(8, 58, 133);
    private static final Color GREEN = new Color(22, 145, 58);
    private static final Color GOLD = new Color(216, 164, 37);
    private static final Color LIGHT = new Color(247, 249, 252);
    private static final Color BORDER = new Color(205, 216, 230);
    private static final Color MUTED = new Color(91, 104, 120);
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
        if (receipts.isEmpty()) receipts = List.of(anchor);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            try (PDPageContentStream c = new PDPageContentStream(document, page)) {
                draw(document, page, c, anchor, receipts, student);
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o comprovativo oficial.", e);
        }
    }

    private void draw(PDDocument doc, PDPage page, PDPageContentStream c, Receipt anchor,
                      List<Receipt> receipts, Student student) throws Exception {
        float w = page.getMediaBox().getWidth();
        float h = page.getMediaBox().getHeight();
        float m = 22;

        fill(c, Color.WHITE, 0, 0, w, h);
        stroke(c, NAVY, m, m, w - 2 * m, h - 2 * m, .8f);

        image(doc, c, "static/assets/imetro.png", 34, h - 102, 118, 72);
        brand(c, w - 218, h - 88);
        center(c, "COMPROVATIVO DE PAGAMENTOS", 22, w / 2, h - 58, NAVY, true);
        line(c, GOLD, w / 2 - 100, h - 72, w / 2 + 100, h - 72, 1.4f);
        center(c, "Emitido em: " + DATE_TIME.format(anchor.getIssuedAt()), 9.5f, w / 2, h - 88, NAVY, false);
        center(c, "Comprovativo Nº " + anchor.getReceiptCode(), 9.5f, w / 2, h - 102, NAVY, false);

        float infoTop = h - 122;
        roundedBox(c, m + 1, infoTop - 82, w - 2 * m - 2, 82, LIGHT, NAVY);
        AcademicClass ac = student.getAcademicClass();
        Course course = ac == null ? null : ac.getCourse();
        pair(c, "NOME", student.getFullName(), 36, infoTop - 23, 220);
        pair(c, "MATRÍCULA", student.getStudentNumber(), 36, infoTop - 47, 220);
        pair(c, "CURSO", course == null ? "-" : course.getName(), 36, infoTop - 71, 220);
        pair(c, "ANO ACADÉMICO", ac == null ? "-" : ac.getAcademicYear(), 325, infoTop - 23, 160);
        pair(c, "TURMA", ac == null ? "-" : ac.getName(), 325, infoTop - 47, 190);
        pair(c, "TELEFONE", mask(student.getPhone()), 325, infoTop - 71, 180);
        pair(c, "DOCUMENTO", mask(student.getDocumentNumber()), 570, infoTop - 32, 190);
        pair(c, "E-MAIL", student.getEmail(), 570, infoTop - 62, 190);

        float sectionY = infoTop - 104;
        roundedBox(c, m + 1, sectionY - 24, w - 2 * m - 2, 24, NAVY, NAVY);
        center(c, "DETALHAMENTO DOS PAGAMENTOS", 11.5f, w / 2, sectionY - 16, Color.WHITE, true);

        float headerY = sectionY - 30;
        fill(c, NAVY_2, m + 1, headerY - 27, w - 2 * m - 2, 27);
        String[] heads = {"Nº", "DESCRIÇÃO DA COBRANÇA", "REF. PERÍODO", "REFERÊNCIA / GUIA", "DATA DO PAGAMENTO", "FORMA DE PAGAMENTO", "VALOR BRUTO", "DESCONTOS", "JUROS / MULTA", "VALOR LÍQUIDO"};
        float[] xs = {29, 58, 190, 260, 355, 452, 565, 630, 692, 757};
        for (int i = 0; i < heads.length; i++) text(c, heads[i], 6.5f, xs[i], headerY - 18, Color.WHITE, true);

        float y = headerY - 47;
        int n = 1;
        BigDecimal gross = BigDecimal.ZERO, disc = BigDecimal.ZERO, fees = BigDecimal.ZERO, total = BigDecimal.ZERO;
        List<Receipt> visible = receipts.stream().limit(5).toList();
        for (Receipt receipt : visible) {
            Charge ch = receipt.getCharge();
            text(c, String.valueOf(n++), 8.2f, 31, y, NAVY, false);
            text(c, clip(ch.getDescription(), 29), 8.2f, 58, y, NAVY, false);
            text(c, safe(ch.getReferenceMonth()), 8, 190, y, NAVY, false);
            text(c, clip(ch.getChargeCode(), 22), 7.4f, 260, y, NAVY, false);
            text(c, ch.getPaidAt() == null ? "-" : DATE_TIME.format(ch.getPaidAt()), 7.5f, 355, y, NAVY, false);
            paymentMethod(c, safe(ch.getPaymentMethod()), 452, y - 7);
            text(c, money(ch.getAmount()), 8, 565, y, NAVY, false);
            text(c, money(ch.getDiscountAmount()), 8, 630, y, NAVY, false);
            BigDecimal fee = nz(ch.getFineAmount()).add(nz(ch.getInterestAmount()));
            text(c, money(fee), 8, 692, y, NAVY, false);
            text(c, money(ch.getTotalAmount()), 8.5f, 757, y, GREEN, true);
            line(c, BORDER, m + 2, y - 14, w - m - 2, y - 14, .45f);
            y -= 39;
            gross = gross.add(nz(ch.getAmount()));
            disc = disc.add(nz(ch.getDiscountAmount()));
            fees = fees.add(fee);
            total = total.add(nz(ch.getTotalAmount()));
        }

        if (receipts.size() > visible.size()) {
            text(c, "+ " + (receipts.size() - visible.size()) + " pagamento(s) adicional(is) no histórico", 7.5f, 58, y + 10, MUTED, false);
            y -= 18;
        }

        line(c, NAVY, m + 1, y + 17, w - m - 1, y + 17, .9f);
        text(c, "Nº TOTAL DE TÍTULOS:  " + receipts.size(), 8.5f, 32, y, NAVY, true);
        text(c, "SUBTOTAL:", 8.5f, 312, y, NAVY, true);
        text(c, money(gross), 8.5f, 455, y, NAVY, false);
        text(c, "ACRÉSCIMOS:", 8.5f, 312, y - 17, NAVY, true);
        text(c, money(fees), 8.5f, 455, y - 17, NAVY_2, false);
        text(c, "DESCONTOS:", 8.5f, 312, y - 34, NAVY, true);
        text(c, money(disc), 8.5f, 455, y - 34, Color.RED, false);
        roundedBox(c, 300, y - 62, 218, 22, NAVY, NAVY);
        text(c, "TOTAL LÍQUIDO:", 8.5f, 314, y - 56, Color.WHITE, true);
        text(c, money(total) + " KZ", 10.5f, 430, y - 56, Color.WHITE, true);

        float boxY = 42;
        float boxH = 116;
        roundedBox(c, m + 1, boxY, w - 2 * m - 2, boxH, Color.WHITE, NAVY);
        String hash = authenticityService.hash(anchor);
        String url = anchor.getValidationUrl();
        if (url == null || !url.contains("hash=")) {
            url = "https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/"
                    + anchor.getReceiptCode() + "/authentic?hash=" + hash;
        }

        BufferedImage qr = createQr(url);
        c.drawImage(LosslessFactory.createFromImage(doc, qr), 34, 55, 90, 90);
        roundedBox(c, 142, 92, 30, 30, new Color(235, 249, 240), GREEN);
        center(c, "V", 15, 157, 101, GREEN, true);
        text(c, "COMPROVATIVO VÁLIDO", 9.5f, 184, 119, GREEN, true);
        text(c, "Este documento comprova os pagamentos recebidos", 8.2f, 184, 101, NAVY, false);
        text(c, "e registados pela tesouraria do IMETRO.", 8.2f, 184, 86, NAVY, false);
        text(c, "Valide pelo QR Code ou pela página pública de consulta.", 8, 184, 69, GREEN, true);
        text(c, "Código de verificação: " + authenticityService.shortHash(anchor) + "-IMETRO", 7.3f, 184, 53, NAVY, false);

        line(c, BORDER, 432, 52, 432, 145, .8f);
        center(c, "RESPONSÁVEL / TESOURARIA", 8.5f, 548, 122, NAVY, true);
        center(c, "Assinado digitalmente", 11, 548, 93, new Color(39, 54, 205), false);
        line(c, NAVY, 478, 79, 618, 79, .6f);
        center(c, "Tesouraria IMETRO", 8.2f, 548, 62, NAVY, false);

        center(c, "SELO DIGITAL", 8.2f, 714, 123, new Color(112, 148, 211), true);
        roundedBox(c, 666, 59, 96, 66, Color.WHITE, new Color(112, 148, 211));
        center(c, "IMETRO", 15, 714, 91, new Color(112, 148, 211), true);
        center(c, "PAGO", 7.5f, 714, 75, GREEN, true);
        center(c, "LIQUIDADO", 7.5f, 714, 63, GREEN, true);

        line(c, NAVY, m + 1, 33, w - m - 1, 33, .8f);
        center(c, "Documento emitido eletronicamente pelo SecretáriaPay Académico - IMETRO", 8, w / 2, 22, NAVY, false);
        center(c, "Este documento não substitui o recibo individual do estudante.", 7.4f, w / 2, 12, NAVY, false);
    }

    private void paymentMethod(PDPageContentStream c, String method, float x, float y) throws Exception {
        String normalized = method == null ? "PAGAMENTO CONFIRMADO" : method.replace('_', ' ').toUpperCase(Locale.ROOT);
        roundedBox(c, x, y, 14, 14, new Color(232, 248, 237), GREEN);
        center(c, normalized.startsWith("TRANSFER") ? "B" : normalized.startsWith("MULTICAIXA") ? "M" : normalized.startsWith("UNITEL") ? "U" : "P", 7, x + 7, y + 4, GREEN, true);
        text(c, clip(normalized, 18), 6.4f, x + 20, y + 4, GREEN, true);
    }

    private void brand(PDPageContentStream c, float x, float y) throws Exception {
        roundedBox(c, x, y, 30, 30, NAVY, NAVY);
        roundedBox(c, x + 5, y + 5, 20, 20, NAVY, GOLD);
        center(c, "SP", 8.5f, x + 15, y + 10, Color.WHITE, true);
        text(c, "Secretária", 14, x + 40, y + 16, NAVY, true);
        text(c, "Pay", 14, x + 105, y + 16, GOLD, true);
        text(c, "ACADÉMICO", 5.8f, x + 41, y + 4, MUTED, true);
    }

    private BufferedImage createQr(String value) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 600, 600, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void image(PDDocument d, PDPageContentStream c, String path, float x, float y, float w, float h) {
        try {
            ClassPathResource r = new ClassPathResource(path);
            if (r.exists()) {
                PDImageXObject i = PDImageXObject.createFromByteArray(d, r.getInputStream().readAllBytes(), path);
                c.drawImage(i, x, y, w, h);
            }
        } catch (Exception ignored) {
        }
    }

    private void pair(PDPageContentStream c, String label, Object value, float x, float y, float maxWidth) throws Exception {
        text(c, label + ":", 8.2f, x, y, NAVY, true);
        fittedText(c, safe(value), 8.2f, 6.8f, x + 76, y, maxWidth - 76, Color.BLACK, false);
    }

    private void fittedText(PDPageContentStream c, String value, float maxSize, float minSize,
                            float x, float y, float maxWidth, Color color, boolean bold) throws Exception {
        String p = pdf(value);
        PDType1Font f = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        float size = maxSize;
        while (size > minSize && f.getStringWidth(p) / 1000 * size > maxWidth) size -= .3f;
        text(c, p, size, x, y, color, bold);
    }

    private void text(PDPageContentStream c, String s, float z, float x, float y, Color color, boolean bold) throws Exception {
        c.beginText();
        c.setNonStrokingColor(color);
        c.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, z);
        c.newLineAtOffset(x, y);
        c.showText(pdf(s));
        c.endText();
    }

    private void center(PDPageContentStream c, String s, float z, float x, float y, Color color, boolean bold) throws Exception {
        String p = pdf(s);
        PDType1Font f = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        text(c, p, z, x - (f.getStringWidth(p) / 1000 * z) / 2, y, color, bold);
    }

    private void fill(PDPageContentStream c, Color color, float x, float y, float w, float h) throws Exception {
        c.setNonStrokingColor(color); c.addRect(x, y, w, h); c.fill();
    }

    private void stroke(PDPageContentStream c, Color color, float x, float y, float w, float h, float z) throws Exception {
        c.setStrokingColor(color); c.setLineWidth(z); c.addRect(x, y, w, h); c.stroke();
    }

    private void roundedBox(PDPageContentStream c, float x, float y, float w, float h, Color fill, Color stroke) throws Exception {
        fill(c, fill, x, y, w, h); stroke(c, stroke, x, y, w, h, .7f);
    }

    private void line(PDPageContentStream c, Color color, float x1, float y1, float x2, float y2, float z) throws Exception {
        c.setStrokingColor(color); c.setLineWidth(z); c.moveTo(x1, y1); c.lineTo(x2, y2); c.stroke();
    }

    private String money(BigDecimal v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("pt", "AO"));
        s.setGroupingSeparator('.'); s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", s).format(nz(v));
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String safe(Object v) { return v == null ? "-" : String.valueOf(v); }
    private String clip(String v, int n) { return v == null ? "-" : v.length() > n ? v.substring(0, n - 3) + "..." : v; }
    private String mask(String v) {
        if (v == null || v.isBlank()) return "-";
        if (v.length() < 7) return "***";
        return v.substring(0, Math.min(4, v.length())) + " *** *** " + v.substring(v.length() - 3);
    }
    private String pdf(String v) {
        return safe(v).replace("–", "-").replace("—", "-").replace("“", "\"")
                .replace("”", "\"").replace("’", "'").replace("…", "...").replace("✓", "V");
    }
}
