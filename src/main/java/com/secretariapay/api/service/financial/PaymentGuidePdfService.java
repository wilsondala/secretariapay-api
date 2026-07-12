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
    private static final Color NAVY = new Color(7, 30, 66);
    private static final Color BLUE = new Color(25, 76, 150);
    private static final Color GOLD = new Color(214, 171, 52);
    private static final Color LIGHT = new Color(245, 248, 252);
    private static final Color BORDER = new Color(214, 222, 232);
    private static final Color MUTED = new Color(88, 102, 121);

    private final ChargeRepository chargeRepository;
    private final String bankName;
    private final String accountHolder;
    private final String iban;
    private final String accountNumber;
    private final String multicaixaReference;
    private final String mobileMoneyInfo;
    private final boolean paymentQrEnabled;
    private final String paymentQrPayloadTemplate;

    public PaymentGuidePdfService(
            ChargeRepository chargeRepository,
            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:06014467710001}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Multicaixa Express / transferência bancária para a conta AKZ indicada}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money/Afrimoney quando autorizado pela instituição}") String mobileMoneyInfo,
            @Value("${secretariapay.payment.qr-enabled:true}") boolean paymentQrEnabled,
            @Value("${secretariapay.payment.qr-payload-template:}") String paymentQrPayloadTemplate
    ) {
        this.chargeRepository = chargeRepository;
        this.bankName = clean(bankName, "Banco Angolano de Investimento");
        this.accountHolder = clean(accountHolder, "OMNEN INTELENGENDA");
        this.iban = clean(iban, "AO06 0040 0000 6014 4677 1017 1");
        this.accountNumber = clean(accountNumber, "06014467710001");
        this.multicaixaReference = clean(multicaixaReference, "Multicaixa Express / transferência bancária para a conta AKZ indicada");
        this.mobileMoneyInfo = clean(mobileMoneyInfo, "Unitel Money/Afrimoney quando autorizado pela instituição");
        this.paymentQrEnabled = paymentQrEnabled;
        this.paymentQrPayloadTemplate = paymentQrPayloadTemplate == null ? "" : paymentQrPayloadTemplate.trim();
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
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(document, page, content, charge);
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a Guia de Pagamento Académico.", exception);
        }
    }

    private void draw(PDDocument document, PDPage page, PDPageContentStream content, Charge charge) throws Exception {
        Student student = charge.getStudent();
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        float width = page.getMediaBox().getWidth();
        float margin = 42;
        float innerWidth = width - (margin * 2);

        drawTopBand(content, width);
        drawInstitutionHeader(document, content, institution, margin, 790, innerWidth);
        drawDocumentIdentity(content, charge, width, 690);
        drawStudentBlock(content, student, academicClass, course, margin, 625, innerWidth);
        drawChargeTable(content, charge, margin, 495, innerWidth);
        drawPaymentBlock(document, content, charge, margin, 350, innerWidth);
        drawInstructions(content, margin, 188, innerWidth);
        drawValidation(document, content, charge, margin, 105, innerWidth);
        drawFooter(content, width);
    }

    private void drawTopBand(PDPageContentStream content, float width) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 814, width, 28);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, 809, width, 5);
        content.fill();
    }

    private void drawInstitutionHeader(PDDocument document, PDPageContentStream content, Institution institution,
                                       float x, float y, float width) throws Exception {
        roundedBox(content, x, y - 82, width, 78, Color.WHITE, BORDER);
        try {
            ClassPathResource logoResource = new ClassPathResource("static/assets/imetro.png");
            if (logoResource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoResource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, x + 14, y - 70, 68, 54);
            }
        } catch (Exception ignored) {
        }

        drawText(content, institutionName(institution), PDType1Font.HELVETICA_BOLD, 11, x + 94, y - 28, NAVY);
        drawText(content, "Secretaria Financeira · Gestão Académica e Financeira", PDType1Font.HELVETICA, 9, x + 94, y - 47, MUTED);
        drawText(content, "Documento emitido eletronicamente pelo SecretáriaPay", PDType1Font.HELVETICA_OBLIQUE, 8, x + 94, y - 64, BLUE);
        drawText(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 15, x + width - 86, y - 37, GOLD);
    }

    private void drawDocumentIdentity(PDPageContentStream content, Charge charge, float width, float y) throws Exception {
        drawCenteredText(content, "GUIA DE PAGAMENTO ACADÉMICO", PDType1Font.HELVETICA_BOLD, 18, width / 2, y, NAVY);
        drawCenteredText(content, "Documento oficial para regularização financeira", PDType1Font.HELVETICA, 9, width / 2, y - 18, MUTED);

        float boxX = 42;
        float boxWidth = width - 84;
        roundedBox(content, boxX, y - 66, boxWidth, 34, LIGHT, BORDER);
        drawText(content, "Nº da guia", PDType1Font.HELVETICA_BOLD, 8, boxX + 12, y - 46, MUTED);
        drawText(content, safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 10, boxX + 80, y - 46, NAVY);
        drawText(content, "Emissão", PDType1Font.HELVETICA_BOLD, 8, boxX + 285, y - 46, MUTED);
        drawText(content, LocalDate.now().format(DATE_FORMATTER), PDType1Font.HELVETICA_BOLD, 10, boxX + 335, y - 46, NAVY);
        drawText(content, "Validade", PDType1Font.HELVETICA_BOLD, 8, boxX + 410, y - 46, MUTED);
        drawText(content, formatDate(charge.getDueDate()), PDType1Font.HELVETICA_BOLD, 10, boxX + 462, y - 46, NAVY);
    }

    private void drawStudentBlock(PDPageContentStream content, Student student, AcademicClass academicClass,
                                  Course course, float x, float y, float width) throws Exception {
        sectionTitle(content, "DADOS DO ESTUDANTE", x, y, width);
        roundedBox(content, x, y - 112, width, 88, Color.WHITE, BORDER);

        info(content, "Nome", student != null ? student.getFullName() : "-", x + 14, y - 43, 210);
        info(content, "Matrícula", student != null ? student.getStudentNumber() : "-", x + 285, y - 43, 120);
        info(content, "Documento", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", x + 414, y - 43, 100);
        info(content, "Curso", course != null ? course.getName() : "-", x + 14, y - 75, 210);
        info(content, "Turma", academicClass != null ? academicClass.getName() : "-", x + 285, y - 75, 120);
        info(content, "Ano académico", academicClass != null ? academicClass.getAcademicYear() : "-", x + 414, y - 75, 100);
    }

    private void drawChargeTable(PDPageContentStream content, Charge charge, float x, float y, float width) throws Exception {
        sectionTitle(content, "DETALHES DA COBRANÇA", x, y, width);
        roundedBox(content, x, y - 118, width, 94, Color.WHITE, BORDER);

        drawText(content, "Descrição", PDType1Font.HELVETICA_BOLD, 8, x + 14, y - 44, MUTED);
        drawText(content, "Referência", PDType1Font.HELVETICA_BOLD, 8, x + 290, y - 44, MUTED);
        drawText(content, "Vencimento", PDType1Font.HELVETICA_BOLD, 8, x + 410, y - 44, MUTED);
        drawWrappedText(content, safe(charge.getDescription()), PDType1Font.HELVETICA, 9, x + 14, y - 61, 250, 10, NAVY);
        drawText(content, safe(charge.getReferenceMonth()), PDType1Font.HELVETICA_BOLD, 9, x + 290, y - 61, NAVY);
        drawText(content, formatDate(charge.getDueDate()), PDType1Font.HELVETICA_BOLD, 9, x + 410, y - 61, NAVY);

        content.setStrokingColor(BORDER);
        content.moveTo(x + 14, y - 77);
        content.lineTo(x + width - 14, y - 77);
        content.stroke();

        BigDecimal base = charge.getAmount() == null ? BigDecimal.ZERO : charge.getAmount();
        BigDecimal total = charge.getTotalAmount() == null ? base : charge.getTotalAmount();
        BigDecimal adjustments = total.subtract(base);

        drawText(content, "Valor base", PDType1Font.HELVETICA_BOLD, 8, x + 14, y - 96, MUTED);
        drawText(content, formatMoney(base, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 10, x + 82, y - 96, NAVY);
        drawText(content, "Acréscimos/ajustes", PDType1Font.HELVETICA_BOLD, 8, x + 230, y - 96, MUTED);
        drawText(content, formatMoney(adjustments, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 10, x + 328, y - 96, NAVY);
        drawText(content, "TOTAL", PDType1Font.HELVETICA_BOLD, 9, x + 430, y - 96, NAVY);
        drawText(content, formatMoney(total, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 12, x + 472, y - 96, GOLD);
    }

    private void drawPaymentBlock(PDDocument document, PDPageContentStream content, Charge charge,
                                  float x, float y, float width) throws Exception {
        sectionTitle(content, "FORMAS DE PAGAMENTO", x, y, width);
        roundedBox(content, x, y - 138, width, 114, LIGHT, BORDER);

        info(content, "Beneficiário", accountHolder, x + 14, y - 45, 220);
        info(content, "Banco", bankName, x + 14, y - 73, 220);
        info(content, "Nº Conta AKZ", accountNumber, x + 14, y - 101, 220);
        info(content, "IBAN", iban, x + 270, y - 45, 195);
        info(content, "Multicaixa", multicaixaReference, x + 270, y - 73, 195);
        info(content, "Outros meios", mobileMoneyInfo, x + 270, y - 101, 195);

        if (paymentQrEnabled) {
            BufferedImage qrImage = createQr(buildPaymentPayload(charge));
            PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
            content.drawImage(qr, x + width - 78, y - 126, 60, 60);
            drawText(content, "QR de pagamento", PDType1Font.HELVETICA_BOLD, 7, x + width - 84, y - 61, BLUE);
        }
    }

    private void drawInstructions(PDPageContentStream content, float x, float y, float width) throws Exception {
        sectionTitle(content, "INSTRUÇÕES IMPORTANTES", x, y, width);
        roundedBox(content, x, y - 66, width, 44, new Color(255, 250, 235), new Color(235, 202, 119));
        drawWrappedText(content,
                "Efetue o pagamento até à data de vencimento. Após o pagamento, envie o comprovativo pelo WhatsApp institucional. O recibo oficial será emitido somente após validação da tesouraria ou confirmação automática da integração de pagamento.",
                PDType1Font.HELVETICA_BOLD, 8.3f, x + 12, y - 37, width - 24, 11, new Color(115, 69, 15));
    }

    private void drawValidation(PDDocument document, PDPageContentStream content, Charge charge,
                                float x, float y, float width) throws Exception {
        roundedBox(content, x, y - 66, width, 58, Color.WHITE, BORDER);
        String url = publicGuideUrl(charge.getChargeCode());
        drawText(content, "VALIDAÇÃO DIGITAL", PDType1Font.HELVETICA_BOLD, 9, x + 12, y - 25, NAVY);
        drawText(content, "Código: " + safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 8, x + 12, y - 42, MUTED);
        drawWrappedText(content, url, PDType1Font.HELVETICA, 7.5f, x + 150, y - 34, width - 250, 9, BLUE);

        BufferedImage qrImage = createQr(url);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + width - 53, y - 58, 42, 42);
    }

    private void drawFooter(PDPageContentStream content, float width) throws Exception {
        content.setStrokingColor(BORDER);
        content.moveTo(42, 28);
        content.lineTo(width - 42, 28);
        content.stroke();
        drawCenteredText(content, "IMETRO · Secretaria Financeira · Documento oficial gerado pelo SecretáriaPay Académico", PDType1Font.HELVETICA, 7.5f, width / 2, 15, MUTED);
    }

    private void sectionTitle(PDPageContentStream content, String title, float x, float y, float width) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, y - 18, width, 18);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(x, y - 18, 5, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 9, x + 14, y - 13, Color.WHITE);
    }

    private void info(PDPageContentStream content, String label, String value, float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 7.5f, x, y, MUTED);
        drawWrappedText(content, safe(value), PDType1Font.HELVETICA_BOLD, 8.5f, x, y - 13, maxWidth, 9, NAVY);
    }

    private void roundedBox(PDPageContentStream content, float x, float y, float width, float height,
                            Color fill, Color stroke) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(stroke);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private BufferedImage createQr(String value) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(value == null || value.isBlank() ? "-" : value, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String buildPaymentPayload(Charge charge) {
        if (!paymentQrPayloadTemplate.isBlank()) {
            return paymentQrPayloadTemplate
                    .replace("{chargeCode}", safe(charge.getChargeCode()))
                    .replace("{amount}", amountPlain(charge.getTotalAmount()))
                    .replace("{currency}", safe(charge.getCurrency()))
                    .replace("{dueDate}", charge.getDueDate() == null ? "" : charge.getDueDate().toString())
                    .replace("{iban}", iban)
                    .replace("{accountNumber}", accountNumber)
                    .replace("{accountHolder}", accountHolder)
                    .replace("{studentNumber}", charge.getStudent() == null ? "" : safe(charge.getStudent().getStudentNumber()));
        }
        return "SECRETARIAPAY|TYPE=PAYMENT|CHARGE=" + safe(charge.getChargeCode())
                + "|AMOUNT=" + amountPlain(charge.getTotalAmount())
                + "|CURRENCY=" + safe(charge.getCurrency())
                + "|BANK=" + bankName
                + "|HOLDER=" + accountHolder
                + "|IBAN=" + iban
                + "|ACCOUNT_AKZ=" + accountNumber;
    }

    private String publicGuideUrl(String chargeCode) {
        return API_BASE_URL + "/api/v1/public/payment-guides/" + chargeCode + "/pdf";
    }

    private String institutionName(Institution institution) {
        if (institution != null && institution.getLegalName() != null && !institution.getLegalName().isBlank()) return institution.getLegalName();
        if (institution != null && institution.getName() != null && !institution.getName().isBlank()) return institution.getName();
        return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
    }

    private String amountPlain(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal value, String currency) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;
        return "AOA".equalsIgnoreCase(safeCurrency)
                ? formatter.format(safeValue) + " Kz"
                : formatter.format(safeValue) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private void drawText(PDPageContentStream content, String text, PDType1Font font, float size,
                          float x, float y, Color color) throws Exception {
        content.beginText();
        content.setNonStrokingColor(color);
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(text));
        content.endText();
    }

    private void drawCenteredText(PDPageContentStream content, String text, PDType1Font font,
                                  float size, float centerX, float y, Color color) throws Exception {
        String safeText = pdfSafe(text);
        float textWidth = font.getStringWidth(safeText) / 1000 * size;
        drawText(content, safeText, font, size, centerX - (textWidth / 2), y, color);
    }

    private void drawWrappedText(PDPageContentStream content, String text, PDType1Font font, float size,
                                 float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String[] words = pdfSafe(text).split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float lineWidth = font.getStringWidth(candidate) / 1000 * size;
            if (lineWidth > maxWidth && line.length() > 0) {
                drawText(content, line.toString(), font, size, x, currentY, color);
                currentY -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) drawText(content, line.toString(), font, size, x, currentY, color);
    }

    private String pdfSafe(String value) {
        if (value == null) return "-";
        return value
                .replace("•", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("’", "'")
                .replace("º", "o")
                .replace("ª", "a")
                .replaceAll("[^\\x20-\\x7EÀ-ÖØ-öø-ÿ]", "");
    }
}
