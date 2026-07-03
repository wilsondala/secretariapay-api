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
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentGuidePdfService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChargeRepository chargeRepository;
    private final String bankName;
    private final String accountHolder;
    private final String iban;
    private final String accountNumber;
    private final String multicaixaReference;
    private final String mobileMoneyInfo;

    public PaymentGuidePdfService(
            ChargeRepository chargeRepository,
            @Value("${secretariapay.payment.bank-name:Banco da instituição}") String bankName,
            @Value("${secretariapay.payment.account-holder:Instituto Superior Politécnico Metropolitano de Angola}") String accountHolder,
            @Value("${secretariapay.payment.iban:A definir pela tesouraria}") String iban,
            @Value("${secretariapay.payment.account-number:A definir}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Transferência via Multicaixa Express para a conta indicada}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money/Afrimoney quando autorizado pela instituição}") String mobileMoneyInfo
    ) {
        this.chargeRepository = chargeRepository;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
        this.iban = iban;
        this.accountNumber = accountNumber;
        this.multicaixaReference = multicaixaReference;
        this.mobileMoneyInfo = mobileMoneyInfo;
    }

    @Transactional(readOnly = true)
    public byte[] generateByChargeId(UUID chargeId) {
        return generate(chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada.")));
    }

    @Transactional(readOnly = true)
    public byte[] generateByChargeCode(String chargeCode) {
        return generate(chargeRepository.findByChargeCode(chargeCode)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada.")));
    }

    private byte[] generate(Charge charge) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(document, page, content, charge);
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a guia de pagamento.", exception);
        }
    }

    private void draw(PDDocument document, PDPage page, PDPageContentStream content, Charge charge) throws Exception {
        Student student = charge.getStudent();
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 48;
        float y = pageHeight - 50;

        drawHeader(document, content, institution, pageWidth, y);
        y -= 116;
        drawCenteredText(content, "GUIA DE PAGAMENTO", PDType1Font.HELVETICA_BOLD, 17, pageWidth / 2, y, new Color(15, 23, 42));
        y -= 26;
        drawCenteredText(content, "SecretáriaPay Académico", PDType1Font.HELVETICA, 10, pageWidth / 2, y, new Color(71, 85, 105));

        y -= 32;
        drawAlertBox(content, margin, y, "Este documento é uma guia de pagamento. O recibo institucional só será emitido após confirmação do pagamento pela tesouraria ou integração bancária.");
        y -= 62;

        drawSectionTitle(content, "Dados da cobrança", margin, y);
        y -= 24;
        drawInfoRow(content, "Código", safe(charge.getChargeCode()), margin, y);
        drawInfoRow(content, "Estado", safe(charge.getStatus()), margin + 260, y);
        y -= 19;
        drawInfoRow(content, "Descrição", safe(charge.getDescription()), margin, y);
        y -= 19;
        drawInfoRow(content, "Referência", safe(charge.getReferenceMonth()), margin, y);
        drawInfoRow(content, "Vencimento", formatDate(charge.getDueDate()), margin + 260, y);
        y -= 19;
        drawInfoRow(content, "Valor base", formatMoney(charge.getAmount(), charge.getCurrency()), margin, y);
        drawInfoRow(content, "Valor total", formatMoney(charge.getTotalAmount(), charge.getCurrency()), margin + 260, y);

        y -= 38;
        drawSectionTitle(content, "Dados do estudante", margin, y);
        y -= 24;
        drawInfoRow(content, "Estudante", student != null ? safe(student.getFullName()) : "-", margin, y);
        y -= 19;
        drawInfoRow(content, "Nº estudante", student != null ? safe(student.getStudentNumber()) : "-", margin, y);
        drawInfoRow(content, "Documento", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", margin + 260, y);
        y -= 19;
        drawInfoRow(content, "Curso", course != null ? safe(course.getName()) : "-", margin, y);
        y -= 19;
        drawInfoRow(content, "Turma", academicClass != null ? safe(academicClass.getName()) : "-", margin, y);
        drawInfoRow(content, "Ano académico", academicClass != null ? safe(academicClass.getAcademicYear()) : "-", margin + 260, y);

        y -= 38;
        drawSectionTitle(content, "Dados bancários e formas de pagamento", margin, y);
        y -= 24;
        drawInfoRow(content, "Titular", safe(accountHolder), margin, y); y -= 18;
        drawInfoRow(content, "Banco", safe(bankName), margin, y); y -= 18;
        drawInfoRow(content, "IBAN", safe(iban), margin, y); y -= 18;
        drawInfoRow(content, "Nº conta", safe(accountNumber), margin, y); y -= 18;
        drawInfoRow(content, "Multicaixa", safe(multicaixaReference), margin, y); y -= 18;
        drawInfoRow(content, "Carteiras", safe(mobileMoneyInfo), margin, y);

        y -= 38;
        drawSectionTitle(content, "Instruções", margin, y);
        y -= 24;
        drawWrappedText(content, "1. Após pagar, envie o comprovativo pelo WhatsApp institucional.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));
        y -= 15;
        drawWrappedText(content, "2. Transferências por IBAN podem depender de compensação bancária. A tesouraria confirma a entrada antes da emissão do recibo.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));
        y -= 27;
        drawWrappedText(content, "3. Depósitos e comprovativos manuais podem ser conferidos em até 24 horas úteis, conforme regra da instituição.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));

        y -= 54;
        drawValidationBox(document, content, charge, margin, y);
        drawFooter(content, pageWidth, 48);
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Institution institution, float pageWidth, float y) throws Exception {
        float logoWidth = 86, logoHeight = 64, logoX = (pageWidth - logoWidth) / 2;
        try {
            ClassPathResource logoResource = new ClassPathResource("static/assets/imetro.png");
            if (logoResource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoResource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, logoX, y - logoHeight, logoWidth, logoHeight);
            }
        } catch (Exception ignored) {}
        drawCenteredText(content, resolveInstitutionDisplayName(institution), PDType1Font.HELVETICA_BOLD, 10, pageWidth / 2, y - 82, new Color(15, 23, 42));
        drawCenteredText(content, "A Marca da Educação", PDType1Font.HELVETICA_OBLIQUE, 10, pageWidth / 2, y - 99, new Color(30, 64, 175));
    }

    private void drawAlertBox(PDPageContentStream content, float x, float y, String text) throws Exception {
        content.setNonStrokingColor(new Color(255, 251, 235)); content.addRect(x, y - 45, 500, 45); content.fill();
        content.setStrokingColor(new Color(212, 175, 55)); content.addRect(x, y - 45, 500, 45); content.stroke();
        drawWrappedText(content, text, PDType1Font.HELVETICA_BOLD, 8.5f, x + 12, y - 17, 476, 11, new Color(120, 53, 15));
    }

    private void drawValidationBox(PDDocument document, PDPageContentStream content, Charge charge, float x, float y) throws Exception {
        String url = publicGuideUrl(charge.getChargeCode());
        content.setNonStrokingColor(new Color(248, 250, 252)); content.addRect(x, y - 110, 500, 110); content.fill();
        content.setStrokingColor(new Color(203, 213, 225)); content.addRect(x, y - 110, 500, 110); content.stroke();
        drawText(content, "Consulta digital da guia", PDType1Font.HELVETICA_BOLD, 12, x + 16, y - 24, new Color(15, 23, 42));
        drawText(content, "Use o QR Code ou o link público para baixar esta guia.", PDType1Font.HELVETICA, 9, x + 16, y - 42, new Color(71, 85, 105));
        drawText(content, "Código: " + safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 10, x + 16, y - 64, new Color(15, 23, 42));
        drawWrappedText(content, url, PDType1Font.HELVETICA, 8, x + 16, y - 82, 330, 10, new Color(30, 64, 175));
        BufferedImage qrImage = createQrCode(url);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + 392, y - 98, 82, 82);
    }

    private BufferedImage createQrCode(String value) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(value, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void drawSectionTitle(PDPageContentStream content, String title, float x, float y) throws Exception {
        content.setNonStrokingColor(new Color(15, 23, 42)); content.addRect(x, y - 4, 500, 18); content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 10, x + 10, y + 2, Color.WHITE);
    }

    private void drawInfoRow(PDPageContentStream content, String label, Object value, float x, float y) throws Exception {
        drawText(content, label + ":", PDType1Font.HELVETICA_BOLD, 8.5f, x, y, new Color(51, 65, 85));
        drawWrappedText(content, safe(value), PDType1Font.HELVETICA, 8.5f, x + 96, y, 155, 10, new Color(15, 23, 42));
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, float y) throws Exception {
        drawCenteredText(content, "Documento emitido eletronicamente pelo SecretáriaPay Académico.", PDType1Font.HELVETICA, 8, pageWidth / 2, y + 16, new Color(100, 116, 139));
        drawCenteredText(content, "TRIA Company · Plataforma institucional de gestão de propinas, cobranças e regularização académica.", PDType1Font.HELVETICA, 8, pageWidth / 2, y, new Color(100, 116, 139));
    }

    private void drawText(PDPageContentStream content, String text, PDType1Font font, float size, float x, float y, Color color) throws Exception {
        content.beginText(); content.setNonStrokingColor(color); content.setFont(font, size); content.newLineAtOffset(x, y); content.showText(toPdfSafeText(text)); content.endText();
    }

    private void drawCenteredText(PDPageContentStream content, String text, PDType1Font font, float size, float centerX, float y, Color color) throws Exception {
        String safeText = toPdfSafeText(text); float width = font.getStringWidth(safeText) / 1000 * size; drawText(content, safeText, font, size, centerX - (width / 2), y, color);
    }

    private void drawWrappedText(PDPageContentStream content, String text, PDType1Font font, float size, float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String safeText = toPdfSafeText(text); String[] words = safeText.split(" "); StringBuilder line = new StringBuilder(); float currentY = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * size;
            if (width > maxWidth && line.length() > 0) { drawText(content, line.toString(), font, size, x, currentY, color); currentY -= lineHeight; line = new StringBuilder(word); }
            else { line = new StringBuilder(candidate); }
        }
        if (line.length() > 0) drawText(content, line.toString(), font, size, x, currentY, color);
    }

    private String resolveInstitutionDisplayName(Institution institution) {
        if (institution == null) return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) return institution.getLegalName();
        if (institution.getName() != null && !institution.getName().isBlank()) return institution.getName();
        return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
    }

    private String publicGuideUrl(String chargeCode) { return API_BASE_URL + "/api/v1/public/payment-guides/" + chargeCode + "/pdf"; }
    private String formatDate(LocalDate value) { return value == null ? "-" : value.format(DATE_FORMATTER); }

    private String formatMoney(BigDecimal value, String currency) {
        if (value == null) return "-";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "AO")); symbols.setGroupingSeparator('.'); symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols); String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;
        return "AOA".equalsIgnoreCase(safeCurrency) ? formatter.format(value) + " Kz" : formatter.format(value) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private String safe(Object value) { return value == null ? "-" : String.valueOf(value); }

    private String toPdfSafeText(String value) {
        if (value == null) return "-";
        return value.replace("•", "-").replace("–", "-").replace("—", "-").replace("“", "\"").replace("”", "\"").replace("’", "'").replace("º", "o").replace("ª", "a").replaceAll("[^\\x20-\\x7EÀ-ÖØ-öø-ÿ]", "");
    }
}
