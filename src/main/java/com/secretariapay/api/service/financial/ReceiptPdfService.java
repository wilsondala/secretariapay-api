package com.secretariapay.api.service.financial;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
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
import java.util.Locale;
import java.util.UUID;

@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReceiptRepository receiptRepository;

    public ReceiptPdfService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Recibo não encontrado."));

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawReceipt(document, page, content, receipt);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF do recibo.", exception);
        }
    }

    private void drawReceipt(PDDocument document, PDPage page, PDPageContentStream content, Receipt receipt) throws Exception {
        Charge charge = receipt.getCharge();
        Student student = charge != null ? charge.getStudent() : null;
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 48;
        float y = pageHeight - 50;

        drawHeader(document, content, institution, pageWidth, y);

        y -= 118;
        drawCenteredText(content, "RECIBO DIGITAL DE PAGAMENTO", PDType1Font.HELVETICA_BOLD, 16, pageWidth / 2, y, new Color(15, 23, 42));

        y -= 30;
        drawCenteredText(content, "SecretáriaPay Académico", PDType1Font.HELVETICA, 10, pageWidth / 2, y, new Color(71, 85, 105));

        y -= 34;
        drawSectionTitle(content, "Dados do recibo", margin, y);
        y -= 24;
        drawInfoRow(content, "Nº do recibo", safe(receipt.getReceiptCode()), margin, y);
        drawInfoRow(content, "Status", safe(receipt.getStatus()), margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Data de emissão", formatDateTime(receipt.getIssuedAt()), margin, y);
        drawInfoRow(content, "Código da cobrança", charge != null ? safe(charge.getChargeCode()) : "-", margin + 260, y);

        y -= 42;
        drawSectionTitle(content, "Dados do estudante", margin, y);
        y -= 24;
        drawInfoRow(content, "Estudante", student != null ? safe(student.getFullName()) : "-", margin, y);
        y -= 20;
        drawInfoRow(content, "Nº de estudante", student != null ? safe(student.getStudentNumber()) : "-", margin, y);
        drawInfoRow(content, "Documento", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Curso", course != null ? safe(course.getName()) : "-", margin, y);
        y -= 20;
        drawInfoRow(content, "Turma", academicClass != null ? safe(academicClass.getName()) : "-", margin, y);
        drawInfoRow(content, "Ano académico", academicClass != null ? safe(academicClass.getAcademicYear()) : "-", margin + 260, y);

        y -= 42;
        drawSectionTitle(content, "Dados financeiros", margin, y);
        y -= 24;
        drawInfoRow(content, "Descrição", charge != null ? safe(charge.getDescription()) : "-", margin, y);
        y -= 20;
        drawInfoRow(content, "Referência", charge != null ? safe(charge.getReferenceMonth()) : "-", margin, y);
        drawInfoRow(content, "Vencimento", charge != null ? formatDate(charge.getDueDate()) : "-", margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Valor pago", charge != null ? formatMoney(charge.getTotalAmount(), charge.getCurrency()) : "-", margin, y);
        drawInfoRow(content, "Data do pagamento", charge != null ? formatDateTime(charge.getPaidAt()) : "-", margin + 260, y);

        y -= 54;
        drawValidationBox(document, content, receipt, margin, y);

        drawFooter(content, pageWidth, 48);
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Institution institution, float pageWidth, float y) throws Exception {
        float logoWidth = 86;
        float logoHeight = 64;
        float logoX = (pageWidth - logoWidth) / 2;

        try {
            ClassPathResource logoResource = new ClassPathResource("static/assets/imetro.png");
            if (logoResource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoResource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, logoX, y - logoHeight, logoWidth, logoHeight);
            }
        } catch (Exception ignored) {
            // O PDF continua sendo emitido mesmo se a imagem institucional não estiver disponível.
        }

        String institutionName = institution != null ? institution.getName() : "Universidade Metropolitana de Angola";
        drawCenteredText(content, institutionName, PDType1Font.HELVETICA_BOLD, 13, pageWidth / 2, y - 82, new Color(15, 23, 42));
        drawCenteredText(content, "A Marca da Educação", PDType1Font.HELVETICA_OBLIQUE, 10, pageWidth / 2, y - 99, new Color(30, 64, 175));
    }

    private void drawValidationBox(PDDocument document, PDPageContentStream content, Receipt receipt, float x, float y) throws Exception {
        float boxWidth = 500;
        float boxHeight = 120;

        content.setNonStrokingColor(new Color(248, 250, 252));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.fill();

        content.setStrokingColor(new Color(203, 213, 225));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.stroke();

        drawText(content, "Validação digital", PDType1Font.HELVETICA_BOLD, 12, x + 16, y - 24, new Color(15, 23, 42));
        drawText(content, "Este recibo pode ser validado pelo QR Code ou pelo link público.", PDType1Font.HELVETICA, 9, x + 16, y - 42, new Color(71, 85, 105));
        drawText(content, "Código: " + safe(receipt.getReceiptCode()), PDType1Font.HELVETICA_BOLD, 10, x + 16, y - 66, new Color(15, 23, 42));
        drawWrappedText(content, safe(receipt.getValidationUrl()), PDType1Font.HELVETICA, 8, x + 16, y - 86, 330, 10, new Color(30, 64, 175));

        BufferedImage qrImage = createQrCode(receipt.getValidationUrl());
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + 392, y - 104, 86, 86);
    }

    private BufferedImage createQrCode(String value) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(value, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void drawSectionTitle(PDPageContentStream content, String title, float x, float y) throws Exception {
        content.setNonStrokingColor(new Color(15, 23, 42));
        content.addRect(x, y - 4, 500, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 10, x + 10, y + 2, Color.WHITE);
    }

    private void drawInfoRow(PDPageContentStream content, String label, Object value, float x, float y) throws Exception {
        drawText(content, label + ":", PDType1Font.HELVETICA_BOLD, 9, x, y, new Color(51, 65, 85));
        drawText(content, safe(value), PDType1Font.HELVETICA, 9, x + 92, y, new Color(15, 23, 42));
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, float y) throws Exception {
        drawCenteredText(content, "Documento emitido eletronicamente pelo SecretáriaPay Académico.", PDType1Font.HELVETICA, 8, pageWidth / 2, y + 16, new Color(100, 116, 139));
        drawCenteredText(content, "TRIA Company · Plataforma institucional de gestão de propinas, cobranças e regularização académica.", PDType1Font.HELVETICA, 8, pageWidth / 2, y, new Color(100, 116, 139));
    }

    private void drawText(PDPageContentStream content, String text, PDType1Font font, float size, float x, float y, Color color) throws Exception {
        content.beginText();
        content.setNonStrokingColor(color);
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(toPdfSafeText(text));
        content.endText();
    }

    private void drawCenteredText(PDPageContentStream content, String text, PDType1Font font, float size, float centerX, float y, Color color) throws Exception {
        String safeText = toPdfSafeText(text);
        float width = font.getStringWidth(safeText) / 1000 * size;
        drawText(content, safeText, font, size, centerX - (width / 2), y, color);
    }

    private void drawWrappedText(PDPageContentStream content, String text, PDType1Font font, float size, float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String safeText = toPdfSafeText(text);
        String[] words = safeText.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * size;
            if (width > maxWidth && line.length() > 0) {
                drawText(content, line.toString(), font, size, x, currentY, color);
                currentY -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            drawText(content, line.toString(), font, size, x, currentY, color);
        }
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatDate(java.time.LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal value, String currency) {
        if (value == null) {
            return "-";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(value) + " " + safe(currency);
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String toPdfSafeText(String value) {
        if (value == null) {
            return "-";
        }

        return value
                .replace("–", "-")
                .replace("—", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replace("•", "-");
    }
}
