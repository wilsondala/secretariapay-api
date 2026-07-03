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
    private final boolean paymentQrEnabled;
    private final String paymentQrLabel;
    private final String paymentQrPayloadTemplate;
    private final String paymentQrInstructions;

    public PaymentGuidePdfService(
            ChargeRepository chargeRepository,

            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:060144677 10 001}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Multicaixa Express / transferência bancária para a conta AKZ indicada}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money/Afrimoney quando autorizado pela instituição}") String mobileMoneyInfo,
            @Value("${secretariapay.payment.qr-enabled:true}") boolean paymentQrEnabled,
            @Value("${secretariapay.payment.qr-label:QRCode de pagamento}") String paymentQrLabel,
            @Value("${secretariapay.payment.qr-payload-template:}") String paymentQrPayloadTemplate,
            @Value("${secretariapay.payment.qr-instructions:Ler no aplicativo bancário/Multicaixa autorizado, quando a instituição disponibilizar QR de pagamento.}") String paymentQrInstructions
    ) {
        this.chargeRepository = chargeRepository;
        this.bankName = defaultIfBlankOrPlaceholder(bankName, "Banco Angolano de Investimento");
        this.accountHolder = defaultIfBlankOrPlaceholder(accountHolder, "OMNEN INTELENGENDA");
        this.iban = defaultIfBlankOrPlaceholder(iban, "AO06 0040 0000 6014 4677 1017 1");
        this.accountNumber = defaultIfBlankOrPlaceholder(accountNumber, "060144677 10 001");
        this.multicaixaReference = defaultIfBlankOrPlaceholder(multicaixaReference, "Multicaixa Express / transferência bancária para a conta AKZ indicada");
        this.mobileMoneyInfo = defaultIfBlankOrPlaceholder(mobileMoneyInfo, "Unitel Money/Afrimoney quando autorizado pela instituição");
        this.paymentQrEnabled = paymentQrEnabled;
        this.paymentQrLabel = defaultIfBlankOrPlaceholder(paymentQrLabel, "QRCode de pagamento");
        this.paymentQrPayloadTemplate = paymentQrPayloadTemplate;
        this.paymentQrInstructions = defaultIfBlankOrPlaceholder(paymentQrInstructions, "Ler no aplicativo bancário/Multicaixa autorizado, quando a instituição disponibilizar QR de pagamento.");
    }

    @Transactional(readOnly = true)
    public byte[] generateByChargeId(UUID chargeId) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        return generate(charge);
    }

    @Transactional(readOnly = true)
    public byte[] generateByChargeCode(String chargeCode) {
        Charge charge = chargeRepository.findByChargeCode(chargeCode)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        return generate(charge);
    }

    private byte[] generate(Charge charge) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawPaymentGuide(document, page, content, charge);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a guia de pagamento.", exception);
        }
    }

    private void drawPaymentGuide(PDDocument document, PDPage page, PDPageContentStream content, Charge charge) throws Exception {
        Student student = charge.getStudent();
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 48;
        float y = pageHeight - 50;

        drawHeader(document, content, institution, pageWidth, y);

        y -= 118;
        drawCenteredText(content, "GUIA DE PAGAMENTO", PDType1Font.HELVETICA_BOLD, 17, pageWidth / 2, y, new Color(15, 23, 42));

        y -= 28;
        drawCenteredText(content, "SecretáriaPay Académico", PDType1Font.HELVETICA, 10, pageWidth / 2, y, new Color(71, 85, 105));

        y -= 32;
        drawAlertBox(content, margin, y, "Este documento é uma guia de pagamento. O recibo institucional só será emitido após confirmação do pagamento pela tesouraria ou integração bancária.");
        y -= 64;

        drawSectionTitle(content, "Dados da cobrança", margin, y);
        y -= 24;
        drawInfoRow(content, "Código", safe(charge.getChargeCode()), margin, y);
        drawInfoRow(content, "Estado", safe(charge.getStatus()), margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Descrição", safe(charge.getDescription()), margin, y);
        y -= 20;
        drawInfoRow(content, "Referência", safe(charge.getReferenceMonth()), margin, y);
        drawInfoRow(content, "Vencimento", formatDate(charge.getDueDate()), margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Valor base", formatMoney(charge.getAmount(), charge.getCurrency()), margin, y);
        drawInfoRow(content, "Valor total", formatMoney(charge.getTotalAmount(), charge.getCurrency()), margin + 260, y);

        y -= 42;
        drawSectionTitle(content, "Dados do estudante", margin, y);
        y -= 24;
        drawInfoRow(content, "Estudante", student != null ? safe(student.getFullName()) : "-", margin, y);
        y -= 20;
        drawInfoRow(content, "Nº estudante", student != null ? safe(student.getStudentNumber()) : "-", margin, y);
        drawInfoRow(content, "Documento", student != null ? safe(student.getDocumentType()) + " " + safe(student.getDocumentNumber()) : "-", margin + 260, y);
        y -= 20;
        drawInfoRow(content, "Curso", course != null ? safe(course.getName()) : "-", margin, y);
        y -= 20;
        drawInfoRow(content, "Turma", academicClass != null ? safe(academicClass.getName()) : "-", margin, y);
        drawInfoRow(content, "Ano académico", academicClass != null ? safe(academicClass.getAcademicYear()) : "-", margin + 260, y);

        y -= 42;
        drawSectionTitle(content, "Dados bancários e formas de pagamento", margin, y);
        y -= 24;

        if (paymentQrEnabled) {
            drawPaymentQrBox(document, content, charge, margin + 346, y + 10);
        }

        drawInfoRowWide(content, "Coordenada", safe(accountHolder), margin, y, 232);
        y -= 20;
        drawInfoRowWide(content, "Banco", safe(bankName), margin, y, 232);
        y -= 20;
        drawInfoRowWide(content, "IBAN", safe(iban), margin, y, 232);
        y -= 20;
        drawInfoRowWide(content, "Nº Conta AKZ", safe(accountNumber), margin, y, 232);
        y -= 20;
        drawInfoRowWide(content, "Multicaixa", safe(multicaixaReference), margin, y, 232);
        y -= 20;
        drawInfoRowWide(content, "Carteiras", safe(mobileMoneyInfo), margin, y, 232);

        y -= 42;
        drawSectionTitle(content, "Instruções", margin, y);
        y -= 24;
        drawWrappedText(content, "1. Após pagar, envie o comprovativo pelo WhatsApp institucional.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));
        y -= 15;
        drawWrappedText(content, "2. Transferências por IBAN podem depender de compensação bancária. A tesouraria confirma a entrada antes da emissão do recibo.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));
        y -= 27;
        drawWrappedText(content, "3. Depósitos e comprovativos manuais podem ser conferidos em até 24 horas úteis, conforme regra da instituição.", PDType1Font.HELVETICA, 9, margin, y, 500, 12, new Color(15, 23, 42));

        y -= 58;
        drawValidationBox(document, content, charge, margin, y);

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

        drawCenteredText(content, resolveInstitutionDisplayName(institution), PDType1Font.HELVETICA_BOLD, 10, pageWidth / 2, y - 82, new Color(15, 23, 42));
        drawCenteredText(content, "A Marca da Educação", PDType1Font.HELVETICA_OBLIQUE, 10, pageWidth / 2, y - 99, new Color(30, 64, 175));
    }

    private void drawAlertBox(PDPageContentStream content, float x, float y, String text) throws Exception {
        float boxWidth = 500;
        float boxHeight = 48;

        content.setNonStrokingColor(new Color(255, 251, 235));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.fill();

        content.setStrokingColor(new Color(212, 175, 55));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.stroke();

        drawWrappedText(content, text, PDType1Font.HELVETICA_BOLD, 8.5f, x + 12, y - 17, boxWidth - 24, 11, new Color(120, 53, 15));
    }

    private void drawPaymentQrBox(PDDocument document, PDPageContentStream content, Charge charge, float x, float y) throws Exception {
        float boxWidth = 146;
        float boxHeight = 118;

        content.setNonStrokingColor(new Color(240, 253, 244));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.fill();

        content.setStrokingColor(new Color(22, 163, 74));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.stroke();

        drawCenteredText(content, safe(paymentQrLabel), PDType1Font.HELVETICA_BOLD, 8.5f, x + (boxWidth / 2), y - 14, new Color(20, 83, 45));

        BufferedImage qrImage = createQrCode(buildPaymentQrPayload(charge));
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + 34, y - 88, 78, 78);

        drawCenteredWrappedText(content, safe(paymentQrInstructions), PDType1Font.HELVETICA, 6.5f, x + 8, y - 100, boxWidth - 16, 8, new Color(21, 128, 61));
    }

    private void drawValidationBox(PDDocument document, PDPageContentStream content, Charge charge, float x, float y) throws Exception {
        float boxWidth = 500;
        float boxHeight = 112;
        String validationUrl = publicGuideUrl(charge.getChargeCode());

        content.setNonStrokingColor(new Color(248, 250, 252));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.fill();

        content.setStrokingColor(new Color(203, 213, 225));
        content.addRect(x, y - boxHeight, boxWidth, boxHeight);
        content.stroke();

        drawText(content, "Consulta digital da guia", PDType1Font.HELVETICA_BOLD, 12, x + 16, y - 24, new Color(15, 23, 42));
        drawText(content, "Use o QR Code ou o link público para baixar esta guia.", PDType1Font.HELVETICA, 9, x + 16, y - 42, new Color(71, 85, 105));
        drawText(content, "Código: " + safe(charge.getChargeCode()), PDType1Font.HELVETICA_BOLD, 10, x + 16, y - 64, new Color(15, 23, 42));
        drawWrappedText(content, validationUrl, PDType1Font.HELVETICA, 8, x + 16, y - 82, 330, 10, new Color(30, 64, 175));

        BufferedImage qrImage = createQrCode(validationUrl);
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + 392, y - 98, 82, 82);
    }

    private BufferedImage createQrCode(String value) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(value == null || value.isBlank() ? "-" : value, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void drawSectionTitle(PDPageContentStream content, String title, float x, float y) throws Exception {
        content.setNonStrokingColor(new Color(15, 23, 42));
        content.addRect(x, y - 4, 500, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 10, x + 10, y + 2, Color.WHITE);
    }

    private void drawInfoRow(PDPageContentStream content, String label, Object value, float x, float y) throws Exception {
        drawInfoRowWide(content, label, value, x, y, 150);
    }

    private void drawInfoRowWide(PDPageContentStream content, String label, Object value, float x, float y, float maxValueWidth) throws Exception {
        drawText(content, label + ":", PDType1Font.HELVETICA_BOLD, 8.5f, x, y, new Color(51, 65, 85));
        drawWrappedText(content, safe(value), PDType1Font.HELVETICA, 8.5f, x + 100, y, maxValueWidth, 10, new Color(15, 23, 42));
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

    private void drawCenteredWrappedText(PDPageContentStream content, String text, PDType1Font font, float size, float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String safeText = toPdfSafeText(text);
        String[] words = safeText.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * size;
            if (width > maxWidth && line.length() > 0) {
                drawCenteredText(content, line.toString(), font, size, x + (maxWidth / 2), currentY, color);
                currentY -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (line.length() > 0) {
            drawCenteredText(content, line.toString(), font, size, x + (maxWidth / 2), currentY, color);
        }
    }

    private String resolveInstitutionDisplayName(Institution institution) {
        if (institution == null) {
            return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
        }

        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) {
            return institution.getLegalName();
        }

        if (institution.getName() != null && !institution.getName().isBlank()) {
            return institution.getName();
        }

        return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
    }

    private String publicGuideUrl(String chargeCode) {
        return API_BASE_URL + "/api/v1/public/payment-guides/" + chargeCode + "/pdf";
    }

    private String buildPaymentQrPayload(Charge charge) {
        String template = paymentQrPayloadTemplate == null ? "" : paymentQrPayloadTemplate.trim();

        if (!template.isBlank()) {
            return template
                    .replace("{chargeCode}", safe(charge.getChargeCode()))
                    .replace("{amount}", amountPlain(charge.getTotalAmount()))
                    .replace("{currency}", safe(charge.getCurrency()))
                    .replace("{dueDate}", charge.getDueDate() == null ? "" : charge.getDueDate().toString())
                    .replace("{iban}", safe(iban))
                    .replace("{accountNumber}", safe(accountNumber))
                    .replace("{accountHolder}", safe(accountHolder))
                    .replace("{studentNumber}", charge.getStudent() == null ? "" : safe(charge.getStudent().getStudentNumber()))
                    .replace("{studentName}", charge.getStudent() == null ? "" : safe(charge.getStudent().getFullName()));
        }

        return "SECRETARIAPAY|TYPE=PAYMENT|CHARGE=" + safe(charge.getChargeCode())
                + "|AMOUNT=" + amountPlain(charge.getTotalAmount())
                + "|CURRENCY=" + safe(charge.getCurrency())
                + "|BANK=" + safe(bankName)
                + "|HOLDER=" + safe(accountHolder)
                + "|IBAN=" + safe(iban)
                + "|ACCOUNT_AKZ=" + safe(accountNumber);
    }

    private String amountPlain(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate value) {
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

        String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;

        if ("AOA".equalsIgnoreCase(safeCurrency)) {
            return formatter.format(value) + " Kz";
        }

        return formatter.format(value) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }


    private String defaultIfBlankOrPlaceholder(String value, String fallback) {
        String clean = value == null ? "" : value.trim();

        if (clean.isBlank()) {
            return fallback;
        }

        String normalized = clean
                .toLowerCase(Locale.ROOT)
                .replace("ç", "c")
                .replace("ã", "a")
                .replace("á", "a")
                .replace("â", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");

        boolean placeholder = normalized.contains("a definir")
                || normalized.contains("banco da instituicao")
                || normalized.contains("banco da instituicao")
                || normalized.contains("conta indicada")
                || normalized.contains("tesouraria");

        if (placeholder && !clean.equalsIgnoreCase("Multicaixa Express / transferência bancária para a conta AKZ indicada")) {
            return fallback;
        }

        return clean;
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String toPdfSafeText(String value) {
        if (value == null) {
            return "-";
        }

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
