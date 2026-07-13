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
    private static final String DOCUMENT_VERSION = "3.1";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Color NAVY = new Color(5, 31, 72);
    private static final Color NAVY_2 = new Color(10, 48, 101);
    private static final Color BLUE = new Color(27, 86, 177);
    private static final Color GOLD = new Color(224, 170, 29);
    private static final Color GOLD_LIGHT = new Color(253, 247, 229);
    private static final Color LIGHT = new Color(247, 249, 252);
    private static final Color BORDER = new Color(208, 218, 231);
    private static final Color MUTED = new Color(87, 101, 119);
    private static final Color GREEN = new Color(23, 137, 95);

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

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 34;
        float width = pageWidth - margin * 2;
        LocalDateTime issuedAt = LocalDateTime.now();
        String hash = documentHash(charge, issuedAt);

        paintBackground(content, pageWidth, pageHeight);
        drawHeader(document, content, institution, margin, 790, width);
        drawTitle(content, pageWidth, 708);
        drawIdentity(content, charge, margin, 655, width);
        drawStudent(content, student, academicClass, course, margin, 574, width);
        drawCharge(content, charge, margin, 463, width);
        drawPayment(content, document, charge, margin, 332, width);
        drawInstructions(content, margin, 208, width);
        drawValidation(content, document, charge, issuedAt, hash, margin, 115, width);
        drawFooter(content, pageWidth, issuedAt, hash);
    }

    private void paintBackground(PDPageContentStream content, float width, float height) throws Exception {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();
        content.setNonStrokingColor(NAVY);
        content.addRect(0, height - 14, width, 14);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(0, height - 19, width, 5);
        content.fill();
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Institution institution,
                            float x, float y, float width) throws Exception {
        box(content, x, y - 62, width, 58, Color.WHITE, BORDER);
        drawImetroLogo(document, content, x + 12, y - 54, 58, 44);
        drawText(content, institutionName(institution), PDType1Font.HELVETICA_BOLD, 9.8f, x + 78, y - 20, NAVY);
        drawText(content, "Secretaria Financeira | Gestão Académica e Financeira", PDType1Font.HELVETICA_BOLD, 7.6f, x + 78, y - 36, NAVY_2);
        drawText(content, "Documento emitido eletronicamente pelo SecretáriaPay", PDType1Font.HELVETICA, 6.8f, x + 78, y - 50, MUTED);
        drawSecretariaPayBrand(content, x + width - 150, y - 50);
    }

    private void drawTitle(PDPageContentStream content, float pageWidth, float y) throws Exception {
        drawCenteredText(content, "GUIA DE PAGAMENTO ACADÉMICO", PDType1Font.HELVETICA_BOLD, 18, pageWidth / 2, y, NAVY);
        drawCenteredText(content, "Regularização financeira académica", PDType1Font.HELVETICA, 8.3f, pageWidth / 2, y - 16, MUTED);
        pill(content, pageWidth / 2 - 47, y - 38, 94, 16, GOLD_LIGHT, GOLD, "PAGAMENTO ACADÉMICO", NAVY);
    }

    private void drawIdentity(PDPageContentStream content, Charge charge, float x, float y, float width) throws Exception {
        float gap = 8;
        float cardW = (width - gap) / 2;
        identityCard(content, "GUIA Nº", safe(charge.getChargeCode()), x, y - 48, cardW, NAVY_2);
        identityCard(content, "EMISSÃO", LocalDate.now().format(DATE_FORMATTER), x + cardW + gap, y - 48, cardW, BLUE);
        identityCard(content, "VENCIMENTO", formatDate(charge.getDueDate()), x, y - 101, cardW, NAVY_2);
        identityCard(content, "SITUAÇÃO", chargeStatus(charge), x + cardW + gap, y - 101, cardW, GOLD);
    }

    private void drawStudent(PDPageContentStream content, Student student, AcademicClass academicClass,
                             Course course, float x, float y, float width) throws Exception {
        sectionHeader(content, "DADOS DO ESTUDANTE", x, y, width);
        box(content, x, y - 91, width, 72, Color.WHITE, BORDER);
        float c1 = width * .52f;
        float c2 = width * .24f;
        infoPair(content, "NOME COMPLETO", student != null ? student.getFullName() : "-", x + 14, y - 37, c1 - 18);
        infoPair(content, "MATRÍCULA", student != null ? student.getStudentNumber() : "-", x + c1 + 6, y - 37, c2 - 12);
        infoPair(content, "DOCUMENTO", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", x + c1 + c2 + 2, y - 37, width - c1 - c2 - 16);
        infoPair(content, "CURSO", course != null ? course.getName() : "-", x + 14, y - 68, c1 - 18);
        infoPair(content, "TURMA", academicClass != null ? academicClass.getName() : "-", x + c1 + 6, y - 68, c2 - 12);
        infoPair(content, "ANO ACADÉMICO", academicClass != null ? academicClass.getAcademicYear() : "-", x + c1 + c2 + 2, y - 68, width - c1 - c2 - 16);
    }

    private void drawCharge(PDPageContentStream content, Charge charge, float x, float y, float width) throws Exception {
        sectionHeader(content, "DETALHES DA COBRANÇA", x, y, width);
        box(content, x, y - 103, width, 84, Color.WHITE, BORDER);
        infoPair(content, "DESCRIÇÃO", safe(charge.getDescription()), x + 14, y - 38, width * .50f);
        infoPair(content, "REFERÊNCIA", safe(charge.getReferenceMonth()), x + width * .58f, y - 38, width * .20f);
        infoPair(content, "VENCIMENTO", formatDate(charge.getDueDate()), x + width * .82f, y - 38, width * .15f);

        BigDecimal base = charge.getAmount() == null ? BigDecimal.ZERO : charge.getAmount();
        BigDecimal total = charge.getTotalAmount() == null ? base : charge.getTotalAmount();
        BigDecimal adjustments = total.subtract(base);
        infoPair(content, "VALOR BASE", formatMoney(base, charge.getCurrency()), x + 14, y - 76, width * .28f);
        infoPair(content, "ACRÉSCIMOS/AJUSTES", formatMoney(adjustments, charge.getCurrency()), x + width * .36f, y - 76, width * .28f);
        box(content, x + width * .69f, y - 93, width * .29f, 42, GOLD_LIGHT, GOLD);
        drawText(content, "TOTAL A PAGAR", PDType1Font.HELVETICA_BOLD, 7.2f, x + width * .71f, y - 67, NAVY);
        drawFittedText(content, formatMoney(total, charge.getCurrency()), PDType1Font.HELVETICA_BOLD, 17, 12, x + width * .71f, y - 87, width * .25f, GOLD);
    }

    private void drawPayment(PDPageContentStream content, PDDocument document, Charge charge,
                             float x, float y, float width) throws Exception {
        sectionHeader(content, "DADOS PARA PAGAMENTO", x, y, width);
        box(content, x, y - 103, width, 84, LIGHT, BORDER);
        float qrArea = 112;
        float textWidth = width - qrArea - 20;
        infoPair(content, "BENEFICIÁRIO", accountHolder, x + 14, y - 37, textWidth * .45f);
        infoPair(content, "IBAN", iban, x + textWidth * .48f, y - 37, textWidth * .49f);
        infoPair(content, "BANCO", bankName, x + 14, y - 67, textWidth * .45f);
        infoPair(content, "Nº DA CONTA (AKZ)", accountNumber, x + textWidth * .48f, y - 67, textWidth * .49f);
        drawWrappedText(content,
                "Referência de pagamento: " + reconciliationCode(charge)
                        + " | Em transferência BAI, informe esta referência na descrição da operação.",
                PDType1Font.HELVETICA_BOLD, 7.1f, x + 14, y - 88, textWidth - 8, 9, NAVY);

        if (paymentQrEnabled) {
            BufferedImage qrImage = createQr(buildPaymentPayload(charge));
            PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
            content.drawImage(qr, x + width - 94, y - 95, 78, 78);
            drawCenteredText(content, "QR PARA PAGAMENTO", PDType1Font.HELVETICA_BOLD, 6.3f, x + width - 55, y - 13, NAVY);
        }
    }

    private void drawInstructions(PDPageContentStream content, float x, float y, float width) throws Exception {
        sectionHeader(content, "INSTRUÇÕES IMPORTANTES", x, y, width);
        box(content, x, y - 75, width, 56, NAVY, NAVY);
        drawWrappedText(content,
                "1. Efetue o pagamento até à data de vencimento.  2. Na transferência BAI, informe a referência de pagamento na descrição.  3. O recibo será emitido após validação da tesouraria ou confirmação automática da integração de pagamento.",
                PDType1Font.HELVETICA_BOLD, 8.2f, x + 14, y - 37, width - 28, 12, Color.WHITE);
    }

    private void drawValidation(PDPageContentStream content, PDDocument document, Charge charge,
                                LocalDateTime issuedAt, String hash, float x, float y, float width) throws Exception {
        float gap = 10;
        float leftW = width * .62f;
        float rightW = width - leftW - gap;
        sectionHeader(content, "VALIDAÇÃO DIGITAL", x, y, leftW);
        box(content, x, y - 75, leftW, 56, Color.WHITE, BORDER);
        pill(content, x + 14, y - 47, 98, 17, new Color(233, 248, 242), GREEN, "EMITIDO DIGITALMENTE", GREEN);
        drawText(content, "Cobrança: " + safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 7.4f, x + 14, y - 64, NAVY);
        drawText(content, "Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER), PDType1Font.HELVETICA, 6.4f, x + 14, y - 73, MUTED);

        String url = publicGuideUrl(charge.getChargeCode());
        BufferedImage qrImage = createQr(url);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + leftW - 67, y - 70, 52, 52);

        sectionHeader(content, "ATENDIMENTO FINANCEIRO", x + leftW + gap, y, rightW);
        box(content, x + leftW + gap, y - 75, rightW, 56, GOLD_LIGHT, new Color(236, 207, 126));
        drawWrappedText(content, "WhatsApp: " + whatsappNumber + "\n" + financialEmail,
                PDType1Font.HELVETICA_BOLD, 7.2f, x + leftW + gap + 12, y - 39, rightW - 24, 11, NAVY);
        drawText(content, "Hash: " + hash.substring(0, Math.min(18, hash.length())), PDType1Font.HELVETICA, 5.8f, x + leftW + gap + 12, y - 68, MUTED);
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, LocalDateTime issuedAt, String hash) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 0, pageWidth, 38);
        content.fill();
        drawText(content, "IMETRO - Secretaria Financeira", PDType1Font.HELVETICA_BOLD, 6.7f, 34, 24, Color.WHITE);
        drawCenteredText(content, institutionalSite + " | " + financialEmail, PDType1Font.HELVETICA, 6.2f, pageWidth / 2, 24, Color.WHITE);
        drawText(content, "Powered by SecretáriaPay | TRIA Company", PDType1Font.HELVETICA_BOLD, 6.4f, pageWidth - 190, 24, Color.WHITE);
        drawCenteredText(content, "Documento financeiro | Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER), PDType1Font.HELVETICA, 5.6f, pageWidth / 2, 10, new Color(220, 228, 240));
    }

    private void drawImetroLogo(PDDocument document, PDPageContentStream content, float x, float y, float w, float h) throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("static/assets/imetro.png");
            if (resource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(document, resource.getInputStream().readAllBytes(), "imetro-logo");
                content.drawImage(logo, x, y, w, h);
                return;
            }
        } catch (Exception ignored) {
        }
        drawText(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 14, x, y + 16, GOLD);
    }

    private void drawSecretariaPayBrand(PDPageContentStream content, float x, float y) throws Exception {
        box(content, x, y, 28, 28, NAVY, GOLD);
        drawCenteredText(content, "SP", PDType1Font.HELVETICA_BOLD, 8.5f, x + 14, y + 9, Color.WHITE);
        drawText(content, "Secretária", PDType1Font.HELVETICA_BOLD, 11.5f, x + 36, y + 14, NAVY);
        drawText(content, "Pay", PDType1Font.HELVETICA_BOLD, 11.5f, x + 92, y + 14, GOLD);
        drawText(content, "GESTÃO FINANCEIRA ACADÉMICA", PDType1Font.HELVETICA_BOLD, 4.8f, x + 36, y + 3, MUTED);
    }

    private void sectionHeader(PDPageContentStream content, String title, float x, float y, float width) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, y - 18, width, 18);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(x, y - 18, 5, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 8.2f, x + 13, y - 13, Color.WHITE);
    }

    private void identityCard(PDPageContentStream content, String label, String value, float x, float y,
                              float width, Color accent) throws Exception {
        box(content, x, y, width, 44, Color.WHITE, BORDER);
        content.setNonStrokingColor(accent);
        content.addRect(x, y, 5, 44);
        content.fill();
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 7, x + 13, y + 29, MUTED);
        drawFittedText(content, value, PDType1Font.HELVETICA_BOLD, 10.5f, 7, x + 13, y + 11, width - 23, NAVY);
    }

    private void infoPair(PDPageContentStream content, String label, Object value, float x, float y, float maxWidth) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 6.7f, x, y + 9, MUTED);
        drawFittedText(content, safe(value), PDType1Font.HELVETICA_BOLD, 8.3f, 6.3f, x, y - 2, maxWidth, NAVY);
    }

    private void box(PDPageContentStream content, float x, float y, float width, float height, Color fill, Color stroke) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(stroke);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void pill(PDPageContentStream content, float x, float y, float width, float height,
                      Color fill, Color stroke, String label, Color textColor) throws Exception {
        box(content, x, y, width, height, fill, stroke);
        drawCenteredText(content, label, PDType1Font.HELVETICA_BOLD, 6.4f, x + width / 2, y + 5.5f, textColor);
    }

    private BufferedImage createQr(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(value == null || value.isBlank() ? "-" : value,
                BarcodeFormat.QR_CODE, 420, 420);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String buildPaymentPayload(Charge charge) {
        if (!paymentQrPayloadTemplate.isBlank()) {
            return paymentQrPayloadTemplate
                    .replace("{chargeCode}", safe(charge.getChargeCode()))
                    .replace("{paymentReference}", reconciliationCode(charge))
                    .replace("{amount}", amountPlain(charge.getTotalAmount()))
                    .replace("{currency}", safe(charge.getCurrency()))
                    .replace("{dueDate}", charge.getDueDate() == null ? "" : charge.getDueDate().toString())
                    .replace("{iban}", iban)
                    .replace("{accountNumber}", accountNumber)
                    .replace("{accountHolder}", accountHolder)
                    .replace("{studentNumber}", charge.getStudent() == null ? "" : safe(charge.getStudent().getStudentNumber()));
        }
        return "SECRETARIAPAY|TYPE=PAYMENT|CHARGE=" + safe(charge.getChargeCode())
                + "|REFERENCE=" + reconciliationCode(charge)
                + "|AMOUNT=" + amountPlain(charge.getTotalAmount())
                + "|CURRENCY=" + safe(charge.getCurrency())
                + "|BANK=" + bankName
                + "|HOLDER=" + accountHolder
                + "|IBAN=" + iban
                + "|ACCOUNT_AKZ=" + accountNumber;
    }

    private String reconciliationCode(Charge charge) {
        String chargeCode = charge == null ? "" : safe(charge.getChargeCode());
        String normalized = chargeCode.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            normalized = charge != null && charge.getId() != null
                    ? charge.getId().toString().replace("-", "").toUpperCase(Locale.ROOT)
                    : "PAGAMENTO";
        }
        if (normalized.length() > 22) {
            normalized = normalized.substring(0, 13) + shortDigest(chargeCode).substring(0, 8);
        }
        return "SPAY-BAI-" + normalized;
    }

    private String shortDigest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(safe(value).getBytes(StandardCharsets.UTF_8))).toUpperCase(Locale.ROOT);
        } catch (Exception exception) {
            return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        }
    }

    private String publicGuideUrl(String chargeCode) {
        return API_BASE_URL + "/api/v1/public/payment-guides/" + safe(chargeCode) + "/pdf";
    }

    private String documentHash(Charge charge, LocalDateTime issuedAt) {
        try {
            String source = safe(charge.getId()) + "|" + safe(charge.getChargeCode()) + "|" + amountPlain(charge.getTotalAmount()) + "|" + issuedAt;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))).toUpperCase(Locale.ROOT);
        } catch (Exception exception) {
            return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        }
    }

    private String chargeStatus(Charge charge) {
        if (charge == null || charge.getStatus() == null) return "PENDENTE";
        String value = charge.getStatus().toString().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PAID" -> "PAGO";
            case "CANCELLED" -> "CANCELADO";
            case "OVERDUE" -> "VENCIDO";
            default -> "PENDENTE";
        };
    }

    private String institutionName(Institution institution) {
        if (institution != null && institution.getLegalName() != null && !institution.getLegalName().isBlank()) return institution.getLegalName();
        if (institution != null && institution.getName() != null && !institution.getName().isBlank()) return institution.getName();
        return "Instituto Superior Politécnico Metropolitano de Angola";
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value).trim();
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

    private void drawFittedText(PDPageContentStream content, String text, PDType1Font font,
                                float preferredSize, float minimumSize, float x, float y,
                                float maxWidth, Color color) throws Exception {
        String value = pdfSafe(text);
        float size = preferredSize;
        while (size > minimumSize && font.getStringWidth(value) / 1000 * size > maxWidth) size -= .4f;
        if (font.getStringWidth(value) / 1000 * size > maxWidth) {
            while (value.length() > 4 && font.getStringWidth(value + "...") / 1000 * size > maxWidth) value = value.substring(0, value.length() - 1);
            value += "...";
        }
        drawText(content, value, font, size, x, y, color);
    }

    private void drawWrappedText(PDPageContentStream content, String text, PDType1Font font, float size,
                                 float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String normalized = pdfSafe(text).replace("\n", " ");
        String[] words = normalized.split("\\s+");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * size > maxWidth && line.length() > 0) {
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
