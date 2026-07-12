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

    private static final Color NAVY = new Color(4, 34, 82);
    private static final Color NAVY_DARK = new Color(2, 24, 58);
    private static final Color BLUE = new Color(18, 74, 155);
    private static final Color GOLD = new Color(219, 168, 28);
    private static final Color GOLD_DARK = new Color(166, 116, 0);
    private static final Color LIGHT = new Color(247, 249, 252);
    private static final Color BORDER = new Color(194, 207, 224);
    private static final Color MUTED = new Color(72, 88, 111);
    private static final Color GREEN = new Color(15, 130, 86);

    private final ChargeRepository chargeRepository;
    private final String bankName;
    private final String accountHolder;
    private final String iban;
    private final String accountNumber;
    private final String multicaixaReference;
    private final String mobileMoneyInfo;
    private final boolean paymentQrEnabled;
    private final String paymentQrPayloadTemplate;
    private final String whatsappNumber;
    private final String institutionWebsite;
    private final String financeEmail;

    public PaymentGuidePdfService(
            ChargeRepository chargeRepository,
            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:06014467710001}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Multicaixa Express / transferência bancária}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money / Afrimoney quando autorizado}") String mobileMoneyInfo,
            @Value("${secretariapay.payment.qr-enabled:true}") boolean paymentQrEnabled,
            @Value("${secretariapay.payment.qr-payload-template:}") String paymentQrPayloadTemplate,
            @Value("${secretariapay.contact.whatsapp:+244 923 168 085}") String whatsappNumber,
            @Value("${secretariapay.contact.website:www.imetroangola.com}") String institutionWebsite,
            @Value("${secretariapay.contact.finance-email:secretaria.financeira@imetroangola.com}") String financeEmail
    ) {
        this.chargeRepository = chargeRepository;
        this.bankName = clean(bankName, "Banco Angolano de Investimento");
        this.accountHolder = clean(accountHolder, "OMNEN INTELENGENDA");
        this.iban = clean(iban, "AO06 0040 0000 6014 4677 1017 1");
        this.accountNumber = clean(accountNumber, "06014467710001");
        this.multicaixaReference = clean(multicaixaReference, "Multicaixa Express / transferência bancária");
        this.mobileMoneyInfo = clean(mobileMoneyInfo, "Unitel Money / Afrimoney quando autorizado");
        this.paymentQrEnabled = paymentQrEnabled;
        this.paymentQrPayloadTemplate = paymentQrPayloadTemplate == null ? "" : paymentQrPayloadTemplate.trim();
        this.whatsappNumber = clean(whatsappNumber, "+244 923 168 085");
        this.institutionWebsite = clean(institutionWebsite, "www.imetroangola.com");
        this.financeEmail = clean(financeEmail, "secretaria.financeira@imetroangola.com");
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
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
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
        float margin = 34;
        float innerWidth = width - (margin * 2);

        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, page.getMediaBox().getHeight());
        content.fill();

        drawTopBand(content, width);
        drawHeader(document, content, institution, margin, 553, innerWidth);
        drawTitle(content, width, 478);
        drawIdentityStrip(content, charge, margin, 433, innerWidth);
        drawStudentSection(content, student, academicClass, course, margin, 370, innerWidth);
        drawChargeSection(content, charge, margin, 292, innerWidth);
        drawPaymentAndInstructions(document, content, charge, margin, 205, innerWidth);
        drawValidationAndContact(document, content, charge, margin, 83, innerWidth);
        drawFooter(content, width);
    }

    private void drawTopBand(PDPageContentStream content, float width) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 578, width, 18);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, 573, width, 5);
        content.fill();
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Institution institution,
                            float x, float y, float width) throws Exception {
        drawImetroLogo(document, content, x + 8, y - 48, 78, 54);

        drawCenteredText(content, institutionName(institution).toUpperCase(Locale.ROOT),
                PDType1Font.HELVETICA_BOLD, 13, x + width / 2, y - 15, NAVY);
        drawCenteredText(content, "Secretaria Financeira | Gestão Académica e Financeira",
                PDType1Font.HELVETICA_BOLD, 10, x + width / 2, y - 34, NAVY);
        drawCenteredText(content, "Documento emitido eletronicamente pelo SecretáriaPay",
                PDType1Font.HELVETICA, 9, x + width / 2, y - 51, BLUE);

        drawSecretariaPayBrand(content, x + width - 180, y - 48);
    }

    private void drawImetroLogo(PDDocument document, PDPageContentStream content,
                                float x, float y, float width, float height) {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/assets/imetro.png");
            if (logoResource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document,
                        logoResource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, x, y, width, height);
            }
        } catch (Exception ignored) {
        }
    }

    private void drawSecretariaPayBrand(PDPageContentStream content, float x, float y) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, y + 6, 34, 34);
        content.fill();
        content.setStrokingColor(GOLD);
        content.setLineWidth(2);
        content.addRect(x + 7, y + 13, 20, 20);
        content.stroke();
        drawCenteredText(content, "SP", PDType1Font.HELVETICA_BOLD, 9, x + 17, y + 20, Color.WHITE);

        drawText(content, "Secretária", PDType1Font.HELVETICA_BOLD, 18, x + 44, y + 23, NAVY);
        drawText(content, "Pay", PDType1Font.HELVETICA_BOLD, 18, x + 121, y + 23, GOLD);
        drawText(content, "SISTEMA DE GESTÃO FINANCEIRA ACADÉMICA",
                PDType1Font.HELVETICA_BOLD, 5.8f, x + 45, y + 9, MUTED);
    }

    private void drawTitle(PDPageContentStream content, float width, float y) throws Exception {
        drawCenteredText(content, "GUIA DE PAGAMENTO ACADÉMICO",
                PDType1Font.HELVETICA_BOLD, 22, width / 2, y, NAVY);
        drawCenteredText(content, "Documento oficial para regularização financeira",
                PDType1Font.HELVETICA, 10, width / 2, y - 19, MUTED);
    }

    private void drawIdentityStrip(PDPageContentStream content, Charge charge,
                                   float x, float y, float width) throws Exception {
        roundedBox(content, x, y - 48, width, 48, Color.WHITE, BORDER);
        float col = width / 4;

        identityItem(content, "Nº DA GUIA", safe(charge.getChargeCode()), x + 18, y - 16, col - 25);
        identityItem(content, "DATA DE EMISSÃO", LocalDate.now().format(DATE_FORMATTER), x + col + 16, y - 16, col - 25);
        identityItem(content, "VALIDADE", formatDate(charge.getDueDate()), x + col * 2 + 16, y - 16, col - 25);
        identityItem(content, "SITUAÇÃO", statusLabel(charge), x + col * 3 + 16, y - 16, col - 25);
    }

    private void identityItem(PDPageContentStream content, String label, String value,
                              float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 8, x, y, MUTED);
        drawWrappedText(content, value, PDType1Font.HELVETICA_BOLD, 11, x, y - 17, maxWidth, 12, NAVY);
    }

    private void drawStudentSection(PDPageContentStream content, Student student,
                                    AcademicClass academicClass, Course course,
                                    float x, float y, float width) throws Exception {
        sectionTitle(content, "DADOS DO ESTUDANTE", x, y, width);
        roundedBox(content, x, y - 66, width, 48, Color.WHITE, BORDER);

        float c1 = x + 16;
        float c2 = x + width * 0.44f;
        float c3 = x + width * 0.78f;

        info(content, "NOME COMPLETO", student != null ? student.getFullName() : "-", c1, y - 34, width * 0.37f);
        info(content, "MATRÍCULA", student != null ? student.getStudentNumber() : "-", c2, y - 34, width * 0.27f);
        info(content, "DOCUMENTO", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", c3, y - 34, width * 0.19f);

        info(content, "CURSO", course != null ? course.getName() : "-", c1, y - 55, width * 0.37f);
        info(content, "TURMA", academicClass != null ? academicClass.getName() : "-", c2, y - 55, width * 0.27f);
        info(content, "ANO ACADÉMICO", academicClass != null ? academicClass.getAcademicYear() : "-", c3, y - 55, width * 0.19f);
    }

    private void drawChargeSection(PDPageContentStream content, Charge charge,
                                   float x, float y, float width) throws Exception {
        sectionTitle(content, "DETALHES DA COBRANÇA", x, y, width);
        roundedBox(content, x, y - 72, width, 54, Color.WHITE, BORDER);

        info(content, "DESCRIÇÃO", charge.getDescription(), x + 16, y - 35, width * 0.40f);
        info(content, "REFERÊNCIA", charge.getReferenceMonth(), x + width * 0.47f, y - 35, width * 0.24f);
        info(content, "VENCIMENTO", formatDate(charge.getDueDate()), x + width * 0.79f, y - 35, width * 0.18f);

        BigDecimal base = value(charge.getAmount());
        BigDecimal total = charge.getTotalAmount() == null ? base : charge.getTotalAmount();
        BigDecimal adjustments = total.subtract(base);

        drawText(content, "VALOR BASE", PDType1Font.HELVETICA_BOLD, 7.5f, x + 16, y - 58, MUTED);
        drawText(content, formatMoney(base, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 11, x + 16, y - 70, NAVY);
        drawText(content, "ACRÉSCIMOS/AJUSTES", PDType1Font.HELVETICA_BOLD, 7.5f, x + width * 0.39f, y - 58, MUTED);
        drawText(content, formatMoney(adjustments, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 11, x + width * 0.39f, y - 70, NAVY);
        drawText(content, "TOTAL A PAGAR", PDType1Font.HELVETICA_BOLD, 8, x + width * 0.74f, y - 58, NAVY);
        drawText(content, formatMoney(total, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 16, x + width * 0.74f, y - 72, GOLD_DARK);
    }

    private void drawPaymentAndInstructions(PDDocument document, PDPageContentStream content, Charge charge,
                                            float x, float y, float width) throws Exception {
        float leftWidth = width * 0.76f;
        float rightX = x + leftWidth + 12;
        float rightWidth = width - leftWidth - 12;

        sectionTitle(content, "FORMAS DE PAGAMENTO", x, y, leftWidth);
        roundedBox(content, x, y - 105, leftWidth, 87, Color.WHITE, BORDER);

        info(content, "BENEFICIÁRIO", accountHolder, x + 16, y - 36, leftWidth * 0.34f);
        info(content, "BANCO", bankName, x + 16, y - 62, leftWidth * 0.34f);
        info(content, "Nº DA CONTA (AKZ)", accountNumber, x + 16, y - 88, leftWidth * 0.34f);
        info(content, "IBAN", iban, x + leftWidth * 0.40f, y - 36, leftWidth * 0.36f);
        info(content, "MEIOS DE PAGAMENTO", multicaixaReference + " | " + mobileMoneyInfo,
                x + leftWidth * 0.40f, y - 62, leftWidth * 0.36f);

        if (paymentQrEnabled) {
            BufferedImage qrImage = createQr(buildPaymentPayload(charge));
            PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
            content.drawImage(qr, x + leftWidth - 92, y - 96, 76, 76);
            drawCenteredText(content, "QR CODE PARA PAGAMENTO", PDType1Font.HELVETICA_BOLD, 7,
                    x + leftWidth - 54, y - 14, NAVY);
        }

        roundedBox(content, rightX, y - 105, rightWidth, 105, NAVY, NAVY);
        drawText(content, "INSTRUÇÕES IMPORTANTES", PDType1Font.HELVETICA_BOLD, 9, rightX + 12, y - 18, Color.WHITE);
        drawBullet(content, "Efetue o pagamento até à data de vencimento.", rightX + 12, y - 39, rightWidth - 24);
        drawBullet(content, "Após o pagamento, envie o comprovativo pelo WhatsApp institucional.", rightX + 12, y - 63, rightWidth - 24);
        drawBullet(content, "O recibo oficial será emitido após validação ou confirmação automática.", rightX + 12, y - 87, rightWidth - 24);
    }

    private void drawValidationAndContact(PDDocument document, PDPageContentStream content, Charge charge,
                                          float x, float y, float width) throws Exception {
        float leftWidth = width * 0.64f;
        float rightX = x + leftWidth + 12;
        float rightWidth = width - leftWidth - 12;

        sectionTitle(content, "VALIDAÇÃO DIGITAL", x, y, leftWidth);
        roundedBox(content, x, y - 72, leftWidth, 54, Color.WHITE, BORDER);
        String url = publicGuideUrl(charge.getChargeCode());
        drawText(content, "Código de validação:", PDType1Font.HELVETICA_BOLD, 8, x + 16, y - 37, MUTED);
        drawText(content, safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 10, x + 16, y - 52, NAVY);
        drawWrappedText(content, "Verifique a autenticidade deste documento no sistema ou através do QR Code.",
                PDType1Font.HELVETICA, 8.5f, x + 16, y - 66, leftWidth - 130, 10, MUTED);

        BufferedImage qrImage = createQr(url);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + leftWidth - 78, y - 67, 58, 58);
        drawCenteredText(content, "QR CODE DE VALIDAÇÃO", PDType1Font.HELVETICA_BOLD, 6.8f,
                x + leftWidth - 49, y - 14, NAVY);

        roundedBox(content, rightX, y - 72, rightWidth, 72, new Color(255, 251, 240), new Color(226, 187, 79));
        drawText(content, "ATENDIMENTO FINANCEIRO", PDType1Font.HELVETICA_BOLD, 10, rightX + 14, y - 22, NAVY);
        drawWrappedText(content, "Em caso de dúvidas, entre em contacto pelo WhatsApp institucional.",
                PDType1Font.HELVETICA, 8.5f, rightX + 14, y - 39, rightWidth - 28, 10, MUTED);
        drawText(content, whatsappNumber, PDType1Font.HELVETICA_BOLD, 11, rightX + 14, y - 61, GOLD_DARK);
    }

    private void drawFooter(PDPageContentStream content, float width) throws Exception {
        content.setNonStrokingColor(NAVY_DARK);
        content.addRect(0, 0, width, 30);
        content.fill();
        drawText(content, "IMETRO - Secretaria Financeira", PDType1Font.HELVETICA_BOLD, 8, 34, 18, Color.WHITE);
        drawCenteredText(content, institutionWebsite + "   |   " + financeEmail,
                PDType1Font.HELVETICA, 8, width / 2, 18, Color.WHITE);
        drawText(content, "Powered by SecretáriaPay", PDType1Font.HELVETICA_BOLD, 8, width - 150, 18, Color.WHITE);
        drawCenteredText(content, "Documento oficial gerado pelo SecretáriaPay Académico",
                PDType1Font.HELVETICA, 6.5f, width / 2, 7, new Color(210, 220, 234));
    }

    private void sectionTitle(PDPageContentStream content, String title, float x, float y, float width) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, y - 18, width, 18);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(x, y - 18, 6, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 9.5f, x + 15, y - 13, Color.WHITE);
    }

    private void info(PDPageContentStream content, String label, String value,
                      float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 7.3f, x, y, MUTED);
        drawWrappedText(content, safe(value), PDType1Font.HELVETICA_BOLD, 9.2f,
                x, y - 13, maxWidth, 10.5f, NAVY);
    }

    private void drawBullet(PDPageContentStream content, String text, float x, float y, float maxWidth) throws Exception {
        drawText(content, "✓", PDType1Font.ZAPF_DINGBATS, 9, x, y, Color.WHITE);
        drawWrappedText(content, text, PDType1Font.HELVETICA_BOLD, 7.8f,
                x + 14, y, maxWidth - 14, 9.5f, Color.WHITE);
    }

    private void roundedBox(PDPageContentStream content, float x, float y, float width, float height,
                            Color fill, Color stroke) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(stroke);
        content.setLineWidth(0.8f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private BufferedImage createQr(String value) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(value == null || value.isBlank() ? "-" : value,
                BarcodeFormat.QR_CODE, 500, 500);
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

    private String statusLabel(Charge charge) {
        if (charge == null || charge.getStatus() == null) return "PENDENTE";
        return switch (charge.getStatus().name()) {
            case "PAID" -> "PAGO";
            case "CANCELLED", "CANCELED" -> "CANCELADO";
            case "OVERDUE" -> "VENCIDO";
            default -> "PENDENTE";
        };
    }

    private String institutionName(Institution institution) {
        if (institution != null && institution.getLegalName() != null && !institution.getLegalName().isBlank()) {
            return institution.getLegalName();
        }
        if (institution != null && institution.getName() != null && !institution.getName().isBlank()) {
            return institution.getName();
        }
        return "Instituto Superior Politécnico Metropolitano de Angola";
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
        drawText(content, safeText, font, size, centerX - textWidth / 2, y, color);
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
