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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentGuidePdfService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final String DOCUMENT_VERSION = "2.0";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Color NAVY = new Color(5, 31, 72);
    private static final Color NAVY_2 = new Color(10, 48, 101);
    private static final Color BLUE = new Color(27, 86, 177);
    private static final Color GOLD = new Color(224, 170, 29);
    private static final Color GOLD_LIGHT = new Color(253, 247, 229);
    private static final Color GREEN = new Color(23, 137, 95);
    private static final Color LIGHT = new Color(247, 249, 252);
    private static final Color BORDER = new Color(208, 218, 231);
    private static final Color MUTED = new Color(87, 101, 119);
    private static final Color SHADOW = new Color(229, 234, 241);

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
    private final String institutionalSite;
    private final String financialEmail;

    public PaymentGuidePdfService(
            ChargeRepository chargeRepository,
            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:06014467710001}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Multicaixa Express / transferência bancária para a conta AKZ indicada}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money/Afrimoney quando autorizado pela instituição}") String mobileMoneyInfo,
            @Value("${secretariapay.payment.qr-enabled:true}") boolean paymentQrEnabled,
            @Value("${secretariapay.payment.qr-payload-template:}") String paymentQrPayloadTemplate,
            @Value("${secretariapay.institution.whatsapp:+244 923 168 085}") String whatsappNumber,
            @Value("${secretariapay.institution.site:www.imetroangola.com}") String institutionalSite,
            @Value("${secretariapay.institution.financial-email:secretaria.financeira@imetroangola.com}") String financialEmail
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
        this.whatsappNumber = clean(whatsappNumber, "+244 923 168 085");
        this.institutionalSite = clean(institutionalSite, "www.imetroangola.com");
        this.financialEmail = clean(financialEmail, "secretaria.financeira@imetroangola.com");
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

        float pageWidth = page.getMediaBox().getWidth();
        float margin = 28;
        float width = pageWidth - (margin * 2);
        LocalDateTime issuedAt = LocalDateTime.now();
        String hash = documentHash(charge, issuedAt);

        drawPageBackground(content, pageWidth, page.getMediaBox().getHeight());
        drawHeader(document, content, institution, margin, 555, width);
        drawTitle(content, pageWidth, 480);
        drawIdentityCards(content, charge, margin, 430, width);
        drawStudentSection(content, student, academicClass, course, margin, 352, width);
        drawChargeSection(content, charge, margin, 274, width);
        drawPaymentAndInstructions(document, content, charge, margin, 183, width);
        drawValidationAndSupport(document, content, charge, issuedAt, hash, margin, 82, width);
        drawFooter(content, pageWidth, issuedAt, hash);
    }

    private void drawPageBackground(PDPageContentStream content, float width, float height) throws Exception {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();
        content.setNonStrokingColor(NAVY);
        content.addRect(0, height - 12, width, 12);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, height - 17, width, 5);
        content.fill();
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Institution institution,
                            float x, float y, float width) throws Exception {
        shadowBox(content, x, y - 62, width, 58, Color.WHITE, BORDER);

        drawImetroLogo(document, content, x + 15, y - 53, 55, 43);
        drawText(content, institutionName(institution), PDType1Font.HELVETICA_BOLD, 12.5f, x + 88, y - 20, NAVY);
        drawText(content, "Secretaria Financeira | Gestão Académica e Financeira", PDType1Font.HELVETICA_BOLD, 9.2f, x + 88, y - 39, NAVY_2);
        drawText(content, "Documento emitido eletronicamente pelo SecretáriaPay", PDType1Font.HELVETICA, 8.2f, x + 88, y - 54, MUTED);

        drawSecretariaPayBrand(content, x + width - 206, y - 50);
    }

    private void drawImetroLogo(PDDocument document, PDPageContentStream content, float x, float y, float w, float h) throws Exception {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/assets/imetro.png");
            if (logoResource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoResource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, x, y, w, h);
                return;
            }
        } catch (Exception ignored) {
        }
        drawText(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 15, x, y + 15, GOLD);
    }

    private void drawSecretariaPayBrand(PDPageContentStream content, float x, float y) throws Exception {
        roundedBox(content, x, y, 34, 34, NAVY, NAVY);
        roundedBox(content, x + 5, y + 5, 24, 24, NAVY, GOLD);
        drawCenteredText(content, "SP", PDType1Font.HELVETICA_BOLD, 10, x + 17, y + 12, Color.WHITE);
        drawText(content, "Secretária", PDType1Font.HELVETICA_BOLD, 16, x + 45, y + 17, NAVY);
        drawText(content, "Pay", PDType1Font.HELVETICA_BOLD, 16, x + 117, y + 17, GOLD);
        drawText(content, "SISTEMA DE GESTÃO FINANCEIRA ACADÉMICA", PDType1Font.HELVETICA_BOLD, 5.8f, x + 45, y + 4, MUTED);
    }

    private void drawTitle(PDPageContentStream content, float pageWidth, float y) throws Exception {
        drawCenteredText(content, "GUIA DE PAGAMENTO ACADÉMICO", PDType1Font.HELVETICA_BOLD, 21, pageWidth / 2, y, NAVY);
        drawCenteredText(content, "Documento oficial para regularização financeira", PDType1Font.HELVETICA, 9.2f, pageWidth / 2, y - 17, MUTED);
        pill(content, pageWidth / 2 - 58, y - 38, 116, 17, GOLD_LIGHT, GOLD, "DOCUMENTO OFICIAL", NAVY);
    }

    private void drawIdentityCards(PDPageContentStream content, Charge charge, float x, float y, float width) throws Exception {
        float gap = 9;
        float cardW = (width - gap * 3) / 4;
        identityCard(content, "GUIA Nº", safe(charge.getChargeCode()), x, y - 48, cardW, NAVY_2);
        identityCard(content, "EMISSÃO", LocalDate.now().format(DATE_FORMATTER), x + cardW + gap, y - 48, cardW, BLUE);
        identityCard(content, "VENCIMENTO", formatDate(charge.getDueDate()), x + (cardW + gap) * 2, y - 48, cardW, NAVY_2);
        identityCard(content, "SITUAÇÃO", chargeStatus(charge), x + (cardW + gap) * 3, y - 48, cardW, GOLD);
    }

    private void identityCard(PDPageContentStream content, String label, String value, float x, float y,
                              float width, Color accent) throws Exception {
        shadowBox(content, x, y, width, 44, Color.WHITE, BORDER);
        content.setNonStrokingColor(accent);
        content.addRect(x, y, 5, 44);
        content.fill();
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 7.2f, x + 13, y + 29, MUTED);
        drawFittedText(content, value, PDType1Font.HELVETICA_BOLD, 10.5f, 7.2f, x + 13, y + 11, width - 23, NAVY);
    }

    private void drawStudentSection(PDPageContentStream content, Student student, AcademicClass academicClass,
                                    Course course, float x, float y, float width) throws Exception {
        sectionHeader(content, "DADOS DO ESTUDANTE", x, y, width);
        shadowBox(content, x, y - 63, width, 49, Color.WHITE, BORDER);
        float col = width / 3;
        infoPair(content, "NOME COMPLETO", student != null ? student.getFullName() : "-", x + 15, y - 31, col - 25);
        infoPair(content, "MATRÍCULA", student != null ? student.getStudentNumber() : "-", x + col + 10, y - 31, col - 20);
        infoPair(content, "DOCUMENTO", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", x + col * 2 + 10, y - 31, col - 25);
        infoPair(content, "CURSO", course != null ? course.getName() : "-", x + 15, y - 52, col - 25);
        infoPair(content, "TURMA", academicClass != null ? academicClass.getName() : "-", x + col + 10, y - 52, col - 20);
        infoPair(content, "ANO ACADÉMICO", academicClass != null ? academicClass.getAcademicYear() : "-", x + col * 2 + 10, y - 52, col - 25);
    }

    private void drawChargeSection(PDPageContentStream content, Charge charge, float x, float y, float width) throws Exception {
        sectionHeader(content, "DETALHES DA COBRANÇA", x, y, width);
        shadowBox(content, x, y - 64, width, 50, Color.WHITE, BORDER);

        float col = width / 3;
        infoPair(content, "DESCRIÇÃO", safe(charge.getDescription()), x + 15, y - 32, col - 25);
        infoPair(content, "REFERÊNCIA", safe(charge.getReferenceMonth()), x + col + 10, y - 32, col - 20);
        infoPair(content, "VENCIMENTO", formatDate(charge.getDueDate()), x + col * 2 + 10, y - 32, col - 25);

        BigDecimal base = charge.getAmount() == null ? BigDecimal.ZERO : charge.getAmount();
        BigDecimal total = charge.getTotalAmount() == null ? base : charge.getTotalAmount();
        BigDecimal adjustments = total.subtract(base);

        moneyCell(content, "VALOR BASE", formatMoney(base, charge.getCurrency()), x + 15, y - 57, col - 25, NAVY);
        moneyCell(content, "ACRÉSCIMOS/AJUSTES", formatMoney(adjustments, charge.getCurrency()), x + col + 10, y - 57, col - 20, NAVY);
        roundedBox(content, x + col * 2 + 5, y - 61, col - 15, 25, GOLD_LIGHT, GOLD);
        drawText(content, "TOTAL A PAGAR", PDType1Font.HELVETICA_BOLD, 7.2f, x + col * 2 + 17, y - 46, MUTED);
        drawFittedText(content, formatMoney(total, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 17, 12,
                x + col * 2 + 17, y - 59, col - 38, GOLD);
    }

    private void drawPaymentAndInstructions(PDDocument document, PDPageContentStream content, Charge charge,
                                            float x, float y, float width) throws Exception {
        float instructionsW = 190;
        float gap = 10;
        float paymentW = width - instructionsW - gap;

        sectionHeader(content, "FORMAS DE PAGAMENTO", x, y, paymentW);
        shadowBox(content, x, y - 81, paymentW, 67, Color.WHITE, BORDER);
        float detailsW = paymentW - 112;
        float col = detailsW / 2;

        infoPair(content, "BENEFICIÁRIO", accountHolder, x + 15, y - 34, col - 20);
        infoPair(content, "IBAN", iban, x + col + 10, y - 34, col - 20);
        infoPair(content, "BANCO", bankName, x + 15, y - 57, col - 20);
        infoPair(content, "Nº DA CONTA (AKZ)", accountNumber, x + col + 10, y - 57, col - 20);
        drawWrappedText(content, multicaixaReference + " | " + mobileMoneyInfo,
                PDType1Font.HELVETICA_BOLD, 7.2f, x + 15, y - 73, detailsW - 22, 8, MUTED);

        if (paymentQrEnabled) {
            drawQr(document, content, buildPaymentPayload(charge), x + paymentW - 92, y - 77, 72);
            drawCenteredText(content, "QR PARA PAGAMENTO", PDType1Font.HELVETICA_BOLD, 6.5f,
                    x + paymentW - 56, y - 12, NAVY);
        }

        float ix = x + paymentW + gap;
        roundedBox(content, ix, y - 81, instructionsW, 81, NAVY, NAVY);
        drawText(content, "INSTRUÇÕES IMPORTANTES", PDType1Font.HELVETICA_BOLD, 9, ix + 13, y - 18, Color.WHITE);
        drawBullet(content, "Efetue o pagamento até à data de vencimento.", ix + 13, y - 36, instructionsW - 26);
        drawBullet(content, "Após o pagamento, envie o comprovativo pelo WhatsApp institucional.", ix + 13, y - 53, instructionsW - 26);
        drawBullet(content, "O recibo será emitido após validação ou confirmação automática.", ix + 13, y - 70, instructionsW - 26);
    }

    private void drawValidationAndSupport(PDDocument document, PDPageContentStream content, Charge charge,
                                          LocalDateTime issuedAt, String hash, float x, float y, float width) throws Exception {
        float gap = 10;
        float validationW = width * 0.62f;
        float supportW = width - validationW - gap;
        String validationUrl = publicGuideUrl(charge.getChargeCode());

        sectionHeader(content, "VALIDAÇÃO DIGITAL", x, y, validationW);
        shadowBox(content, x, y - 66, validationW, 52, Color.WHITE, BORDER);
        pill(content, x + 14, y - 47, 92, 19, new Color(232, 247, 240), GREEN, "EMITIDO DIGITALMENTE", GREEN);
        drawText(content, "Código de validação:", PDType1Font.HELVETICA_BOLD, 7, x + 118, y - 31, MUTED);
        drawFittedText(content, safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 9.2f, 7,
                x + 118, y - 45, validationW - 225, NAVY);
        drawText(content, "Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER),
                PDType1Font.HELVETICA, 6.8f, x + 118, y - 58, MUTED);
        drawQr(document, content, validationUrl, x + validationW - 63, y - 61, 48);

        float sx = x + validationW + gap;
        shadowBox(content, sx, y - 66, supportW, 66, GOLD_LIGHT, new Color(233, 199, 102));
        drawText(content, "ATENDIMENTO FINANCEIRO", PDType1Font.HELVETICA_BOLD, 9.2f, sx + 14, y - 20, NAVY);
        drawWrappedText(content, "Em caso de dúvidas, contacte a Secretaria Financeira pelo WhatsApp institucional.",
                PDType1Font.HELVETICA, 7.5f, sx + 14, y - 36, supportW - 28, 9, MUTED);
        drawText(content, whatsappNumber, PDType1Font.HELVETICA_BOLD, 10, sx + 14, y - 58, GOLD);
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, LocalDateTime issuedAt, String hash) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 0, pageWidth, 36);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, 36, pageWidth, 3);
        content.fill();

        drawText(content, "IMETRO - Secretaria Financeira", PDType1Font.HELVETICA_BOLD, 7.4f, 28, 23, Color.WHITE);
        drawText(content, "Documento oficial gerado pelo SecretáriaPay Académico", PDType1Font.HELVETICA, 6.2f, 28, 11, new Color(220, 228, 238));
        drawCenteredText(content, institutionalSite + "  |  " + financialEmail, PDType1Font.HELVETICA, 7,
                pageWidth / 2, 20, Color.WHITE);
        drawCenteredText(content, "Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER),
                PDType1Font.HELVETICA, 5.8f, pageWidth / 2, 9, new Color(220, 228, 238));
        drawText(content, "Powered by SecretáriaPay | TRIA Company", PDType1Font.HELVETICA_BOLD, 7,
                pageWidth - 192, 23, Color.WHITE);
        drawFittedText(content, "HASH: " + hash, PDType1Font.COURIER, 5.5f, 4.6f,
                pageWidth - 192, 11, 164, new Color(220, 228, 238));
    }

    private void sectionHeader(PDPageContentStream content, String title, float x, float y, float width) throws Exception {
        roundedBox(content, x, y - 14, width, 14, NAVY, NAVY);
        content.setNonStrokingColor(GOLD);
        content.addRect(x, y - 14, 5, 14);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 8.5f, x + 14, y - 10, Color.WHITE);
    }

    private void infoPair(PDPageContentStream content, String label, String value, float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 6.5f, x, y + 8, MUTED);
        drawFittedText(content, safe(value), PDType1Font.HELVETICA_BOLD, 8.6f, 6.5f, x, y - 2, maxWidth, NAVY);
    }

    private void moneyCell(PDPageContentStream content, String label, String value, float x, float y,
                           float maxWidth, Color color) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 6.5f, x, y + 7, MUTED);
        drawFittedText(content, value, PDType1Font.HELVETICA_BOLD, 9.8f, 7.4f, x, y - 4, maxWidth, color);
    }

    private void drawBullet(PDPageContentStream content, String text, float x, float y, float maxWidth) throws Exception {
        drawText(content, "✓", PDType1Font.ZAPF_DINGBATS, 8, x, y + 1, Color.WHITE);
        drawWrappedText(content, text, PDType1Font.HELVETICA_BOLD, 6.7f, x + 12, y,
                maxWidth - 12, 7.6f, Color.WHITE);
    }

    private void drawQr(PDDocument document, PDPageContentStream content, String value,
                        float x, float y, float size) throws Exception {
        roundedBox(content, x - 4, y - 4, size + 8, size + 8, Color.WHITE, BORDER);
        BufferedImage qrImage = createQr(value);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x, y, size, size);
    }

    private void pill(PDPageContentStream content, float x, float y, float width, float height,
                      Color fill, Color stroke, String text, Color textColor) throws Exception {
        roundedBox(content, x, y, width, height, fill, stroke);
        drawCenteredText(content, text, PDType1Font.HELVETICA_BOLD, 6.7f,
                x + width / 2, y + 5.5f, textColor);
    }

    private void shadowBox(PDPageContentStream content, float x, float y, float width, float height,
                           Color fill, Color stroke) throws Exception {
        roundedBox(content, x + 2.5f, y - 2.5f, width, height, SHADOW, SHADOW);
        roundedBox(content, x, y, width, height, fill, stroke);
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

    private String institutionName(Institution institution) {
        if (institution != null && institution.getLegalName() != null && !institution.getLegalName().isBlank()) return institution.getLegalName();
        if (institution != null && institution.getName() != null && !institution.getName().isBlank()) return institution.getName();
        return "Instituto Superior Politécnico Metropolitano de Angola";
    }

    private String chargeStatus(Charge charge) {
        if (charge == null || charge.getStatus() == null) return "PENDENTE";
        String status = charge.getStatus().name();
        return switch (status) {
            case "PAID" -> "PAGO";
            case "CANCELLED" -> "CANCELADO";
            case "OVERDUE" -> "VENCIDO";
            default -> "PENDENTE";
        };
    }

    private String documentHash(Charge charge, LocalDateTime issuedAt) {
        try {
            String source = safe(charge.getId()) + "|" + safe(charge.getChargeCode()) + "|"
                    + amountPlain(charge.getTotalAmount()) + "|" + issuedAt + "|" + DOCUMENT_VERSION;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest).substring(0, 32);
        } catch (Exception ignored) {
            return safe(charge.getId()).replace("-", "").toUpperCase(Locale.ROOT);
        }
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

    private void drawFittedText(PDPageContentStream content, String text, PDType1Font font,
                                float preferredSize, float minSize, float x, float y,
                                float maxWidth, Color color) throws Exception {
        String safeText = pdfSafe(text);
        float size = preferredSize;
        while (size > minSize && font.getStringWidth(safeText) / 1000 * size > maxWidth) {
            size -= 0.4f;
        }
        if (font.getStringWidth(safeText) / 1000 * size <= maxWidth) {
            drawText(content, safeText, font, size, x, y, color);
            return;
        }
        String ellipsis = "...";
        String candidate = safeText;
        while (!candidate.isEmpty() && font.getStringWidth(candidate + ellipsis) / 1000 * size > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        drawText(content, candidate + ellipsis, font, size, x, y, color);
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
