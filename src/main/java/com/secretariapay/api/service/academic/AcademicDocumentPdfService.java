package com.secretariapay.api.service.academic;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AcademicDocumentPdfService {

    private static final Color NAVY = new Color(8, 38, 74);
    private static final Color NAVY_2 = new Color(14, 55, 104);
    private static final Color GOLD = new Color(216, 169, 40);
    private static final Color LIGHT = new Color(245, 248, 252);
    private static final Color BORDER = new Color(184, 199, 217);
    private static final Color MUTED = new Color(95, 111, 130);
    private static final Color GREEN = new Color(21, 154, 109);
    private static final Color RED = new Color(179, 38, 30);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("pt-AO"));
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String apiBaseUrl;

    public AcademicDocumentPdfService(
            @Value("${secretariapay.public-api-base-url:https://secretariapay-api.paixaoangola.com}") String apiBaseUrl
    ) {
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    public byte[] generate(AcademicDocumentRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(document, page, content, request);
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a declaração académica.", exception);
        }
    }

    private void draw(PDDocument document, PDPage page, PDPageContentStream content, AcademicDocumentRequest request) throws Exception {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float margin = 42;
        float contentWidth = width - margin * 2;
        Student student = request.getStudent();
        AcademicClass academicClass = student == null ? null : student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();

        paintPage(content, width, height);
        if (request.isDemoMode()) drawWatermark(content, width, height);

        drawHeader(content, margin, height - 50, contentWidth);
        drawCentered(content, "DECLARACAO", PDType1Font.HELVETICA_BOLD, 20, width / 2, height - 145, NAVY);
        drawCentered(content, "Declaracao academica simples", PDType1Font.HELVETICA, 8.5f, width / 2, height - 163, MUTED);

        drawInfoGrid(content, request, margin, height - 192, contentWidth);

        float bodyY = height - 315;
        String body = firstNonBlank(request.getDeclarationText(), defaultDeclarationText(student, academicClass, course));
        bodyY = drawWrappedParagraph(content, body, PDType1Font.HELVETICA, 11.2f, margin + 12, bodyY, contentWidth - 24, 18, new Color(31, 41, 55));
        bodyY -= 20;
        drawText(content, "A presente declaracao e emitida a pedido do interessado para os fins declarados.", PDType1Font.HELVETICA, 10.5f, margin + 12, bodyY, new Color(31, 41, 55));
        bodyY -= 44;
        drawText(content, "Luanda, " + LocalDate.now().format(DATE_FORMAT) + ".", PDType1Font.HELVETICA, 10.5f, margin + 12, bodyY, new Color(31, 41, 55));

        float signatureTop = 280;
        drawSignatureAndValidation(document, content, request, margin, signatureTop, contentWidth);

        if (request.isDemoMode()) {
            drawDemoWarning(content, margin, 82, contentWidth);
        }
        drawFooter(content, width, request);
    }

    private void paintPage(PDPageContentStream content, float width, float height) throws Exception {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();
        content.setNonStrokingColor(NAVY);
        content.addRect(0, height - 36, width, 36);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, height - 42, width, 6);
        content.fill();
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 0, width, 39);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, 39, width, 4);
        content.fill();
    }

    private void drawWatermark(PDPageContentStream content, float width, float height) throws Exception {
        content.saveGraphicsState();
        content.setNonStrokingColor(new Color(220, 225, 233));
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 34);
        content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(32), width / 2 - 210, height / 2 - 10));
        content.showText("DEMONSTRACAO - SEM VALIDADE");
        content.endText();
        content.restoreGraphicsState();
    }

    private void drawHeader(PDPageContentStream content, float x, float y, float width) throws Exception {
        box(content, x, y - 65, width, 60, Color.WHITE, BORDER);
        drawText(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 17, x + 14, y - 25, NAVY);
        drawText(content, "Instituto Superior Politecnico Metropolitano de Angola", PDType1Font.HELVETICA_BOLD, 8.3f, x + 14, y - 41, NAVY);
        drawText(content, "Secretaria Academica e Financeira", PDType1Font.HELVETICA, 7.2f, x + 14, y - 54, MUTED);

        drawText(content, "SecretariaPay", PDType1Font.HELVETICA_BOLD, 11, x + width - 105, y - 28, NAVY);
        drawText(content, "Gestao documental", PDType1Font.HELVETICA, 7, x + width - 105, y - 43, MUTED);
    }

    private void drawInfoGrid(PDPageContentStream content, AcademicDocumentRequest request, float x, float y, float width) throws Exception {
        float half = width / 2;
        box(content, x, y - 94, width, 94, LIGHT, BORDER);
        line(content, x + half, y, x + half, y - 94, BORDER);
        line(content, x, y - 47, x + width, y - 47, BORDER);

        info(content, "TIPO DE DOCUMENTO", documentTypeLabel(request.getDocumentType()), x + 12, y - 20, half - 24);
        info(content, "CODIGO", request.getDocumentCode(), x + half + 12, y - 20, half - 24);
        info(content, "ESTADO", statusLabel(request.getStatus()), x + 12, y - 67, half - 24);
        info(content, "EMISSAO", formatDateTime(firstNonNull(request.getIssuedAt(), request.getCreatedAt())), x + half + 12, y - 67, half - 24);
    }

    private void drawSignatureAndValidation(PDDocument document, PDPageContentStream content, AcademicDocumentRequest request,
                                            float x, float y, float width) throws Exception {
        float signatureWidth = width * .68f;
        float validationWidth = width - signatureWidth;
        box(content, x, y - 142, width, 142, Color.WHITE, BORDER);
        line(content, x + signatureWidth, y, x + signatureWidth, y - 142, BORDER);
        fill(content, x + signatureWidth, y - 142, validationWidth, 142, LIGHT);

        drawCentered(content, "Assinado eletronicamente por", PDType1Font.HELVETICA, 8, x + signatureWidth / 2, y - 28, MUTED);
        drawCentered(content, pdfSafe(request.getSignatoryName()), PDType1Font.HELVETICA_OBLIQUE, 14, x + signatureWidth / 2, y - 55, NAVY);
        drawCentered(content, pdfSafe(request.getSignatoryRole()), PDType1Font.HELVETICA_BOLD, 8.3f, x + signatureWidth / 2, y - 73, NAVY_2);
        drawCentered(content, signatureStatus(request), PDType1Font.HELVETICA, 7.2f, x + signatureWidth / 2, y - 95, MUTED);
        drawCentered(content, "Metodo: " + firstNonBlank(request.getSignatureMethod(), "Aguardando assinatura"), PDType1Font.HELVETICA, 6.8f, x + signatureWidth / 2, y - 114, MUTED);

        BufferedImage qrImage = qrCode(validationUrl(request), 240);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + signatureWidth + 24, y - 88, 62, 62);
        drawCentered(content, "VALIDACAO DIGITAL", PDType1Font.HELVETICA_BOLD, 6.8f, x + signatureWidth + validationWidth / 2, y - 103, NAVY);
        drawCentered(content, hashSummary(request.getDocumentHash()), PDType1Font.HELVETICA, 5.7f, x + signatureWidth + validationWidth / 2, y - 119, MUTED);
        drawCentered(content, "Versao " + request.getVersionNumber(), PDType1Font.HELVETICA, 5.8f, x + signatureWidth + validationWidth / 2, y - 132, MUTED);
    }

    private void drawDemoWarning(PDPageContentStream content, float x, float y, float width) throws Exception {
        box(content, x, y - 31, width, 31, new Color(255, 244, 243), new Color(230, 163, 160));
        drawFittedText(content,
                "DEMONSTRACAO SEM VALIDADE: documento criado para aprovacao visual e validacao do fluxo de assinatura eletronica.",
                PDType1Font.HELVETICA_BOLD, 7.2f, 5.8f, x + 10, y - 19, width - 20, RED);
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, AcademicDocumentRequest request) throws Exception {
        drawText(content, "IMETRO - Secretaria Academica", PDType1Font.HELVETICA, 6.5f, 42, 17, Color.WHITE);
        drawCentered(content, "Codigo: " + request.getDocumentCode(), PDType1Font.HELVETICA, 6.2f, pageWidth / 2, 17, Color.WHITE);
        drawTextRight(content, "Powered by SecretariaPay | TRIA Company", PDType1Font.HELVETICA, 6.2f, pageWidth - 42, 17, Color.WHITE);
    }

    private String defaultDeclarationText(Student student, AcademicClass academicClass, Course course) {
        String studentName = student == null ? "-" : student.getFullName();
        String document = student == null ? "-" : firstNonBlank(student.getDocumentNumber(), "nao informado");
        String studentNumber = student == null ? "-" : student.getStudentNumber();
        String courseName = course == null ? "curso registado na instituicao" : course.getName();
        String academicYear = academicClass == null ? String.valueOf(LocalDate.now().getYear()) : academicClass.getAcademicYear();
        return "O Instituto Superior Politecnico Metropolitano de Angola - IMETRO, por intermedio da sua Secretaria Academica, declara, para os devidos efeitos, que "
                + studentName + ", portador do documento de identificacao n. " + document + ", matricula n. " + studentNumber
                + ", encontra-se regularmente matriculado no curso de " + courseName + ", no ano academico de " + academicYear + ".";
    }

    private float drawWrappedParagraph(PDPageContentStream content, String text, PDFont font, float fontSize,
                                       float x, float y, float maxWidth, float leading, Color color) throws Exception {
        List<String> lines = wrap(text, font, fontSize, maxWidth);
        content.setNonStrokingColor(color);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        for (String line : lines) {
            content.showText(pdfSafe(line));
            content.newLineAtOffset(0, -leading);
        }
        content.endText();
        return y - lines.size() * leading;
    }

    private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String paragraph : pdfSafe(text).split("\\R")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void info(PDPageContentStream content, String label, String value, float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 6.5f, x, y, MUTED);
        drawFittedText(content, value, PDType1Font.HELVETICA_BOLD, 9, 6.5f, x, y - 15, maxWidth, NAVY);
    }

    private void box(PDPageContentStream content, float x, float y, float width, float height, Color fill, Color stroke) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(stroke);
        content.setLineWidth(.7f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void fill(PDPageContentStream content, float x, float y, float width, float height, Color fill) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void line(PDPageContentStream content, float x1, float y1, float x2, float y2, Color color) throws Exception {
        content.setStrokingColor(color);
        content.setLineWidth(.6f);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void drawText(PDPageContentStream content, String text, PDFont font, float size, float x, float y, Color color) throws Exception {
        content.setNonStrokingColor(color);
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(text));
        content.endText();
    }

    private void drawTextRight(PDPageContentStream content, String text, PDFont font, float size, float rightX, float y, Color color) throws Exception {
        String safe = pdfSafe(text);
        float textWidth = font.getStringWidth(safe) / 1000f * size;
        drawText(content, safe, font, size, rightX - textWidth, y, color);
    }

    private void drawCentered(PDPageContentStream content, String text, PDFont font, float size, float centerX, float y, Color color) throws Exception {
        String safe = pdfSafe(text);
        float textWidth = font.getStringWidth(safe) / 1000f * size;
        drawText(content, safe, font, size, centerX - textWidth / 2, y, color);
    }

    private void drawFittedText(PDPageContentStream content, String text, PDFont font, float preferredSize, float minSize,
                                float x, float y, float maxWidth, Color color) throws Exception {
        String safe = pdfSafe(text);
        float size = preferredSize;
        while (size > minSize && font.getStringWidth(safe) / 1000f * size > maxWidth) size -= .4f;
        if (font.getStringWidth(safe) / 1000f * size > maxWidth && safe.length() > 4) {
            while (safe.length() > 4 && font.getStringWidth(safe + "...") / 1000f * size > maxWidth) {
                safe = safe.substring(0, safe.length() - 1);
            }
            safe += "...";
        }
        drawText(content, safe, font, size, x, y, color);
    }

    private BufferedImage qrCode(String payload, int size) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String validationUrl(AcademicDocumentRequest request) {
        return apiBaseUrl + "/api/v1/public/academic-documents/" + request.getDocumentCode() + "/validate";
    }

    private String documentTypeLabel(String value) {
        return "SIMPLE_DECLARATION".equalsIgnoreCase(value) ? "Declaracao simples" : firstNonBlank(value, "Documento academico");
    }

    private String statusLabel(String value) {
        return switch (firstNonBlank(value, "DRAFT")) {
            case "READY_FOR_SIGNATURE" -> "Pronto para assinatura";
            case "SIGNED" -> "Assinado eletronicamente";
            case "SENT" -> "Assinado e disponibilizado";
            case "CANCELLED" -> "Cancelado";
            default -> "Em preparacao";
        };
    }

    private String signatureStatus(AcademicDocumentRequest request) {
        if (request.getSignedAt() == null) return "Aguardando assinatura institucional";
        return "Assinado em " + request.getSignedAt().format(DATE_TIME_FORMAT);
    }

    private String hashSummary(String hash) {
        if (hash == null || hash.isBlank()) return "Hash pendente";
        if (hash.length() <= 28) return hash;
        return hash.substring(0, 14) + "..." + hash.substring(hash.length() - 10);
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMAT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String stripTrailingSlash(String value) {
        String resolved = value == null || value.isBlank() ? "https://secretariapay-api.paixaoangola.com" : value.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String pdfSafe(String value) {
        if (value == null) return "";
        return value
                .replace('—', '-')
                .replace('–', '-')
                .replace('“', '"')
                .replace('”', '"')
                .replace('’', '\'')
                .replace("•", "-")
                .replace("ª", "a")
                .replace("º", "o");
    }
}
