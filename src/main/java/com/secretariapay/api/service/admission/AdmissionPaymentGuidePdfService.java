package com.secretariapay.api.service.admission;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdmissionPaymentGuidePdfService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final String DOCUMENT_VERSION = "3.1";
    private static final String INSTITUTIONAL_SITE = "www.imetroangola.com";
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

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository invoiceRepository;
    private final boolean pilotEnabled;
    private final String bankName;
    private final String accountHolder;
    private final String iban;
    private final String accountNumber;
    private final String whatsapp;
    private final String financialEmail;

    public AdmissionPaymentGuidePdfService(
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository invoiceRepository,
            @Value("${secretariapay.admissions.public-payment-pilot-enabled:false}") boolean pilotEnabled,
            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:06014467710001}") String accountNumber,
            @Value("${secretariapay.institution.whatsapp:+244 991 640 259}") String whatsapp,
            @Value("${secretariapay.institution.financial-email:secretaria.financeira@imetroangola.com}") String financialEmail
    ) {
        this.applicationRepository = applicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.pilotEnabled = pilotEnabled;
        this.bankName = clean(bankName, "Banco Angolano de Investimento");
        this.accountHolder = clean(accountHolder, "OMNEN INTELENGENDA");
        this.iban = clean(iban, "AO06 0040 0000 6014 4677 1017 1");
        this.accountNumber = clean(accountNumber, "06014467710001");
        this.whatsapp = clean(whatsapp, "+244 991 640 259");
        this.financialEmail = clean(financialEmail, "secretaria.financeira@imetroangola.com");
    }

    @Transactional(readOnly = true)
    public byte[] generate(
            String applicationCode,
            AdmissionDto.PublicApplicationAccessRequest request
    ) {
        if (!pilotEnabled) {
            throw new IllegalArgumentException("A emissão pública da guia está desativada neste ambiente.");
        }

        AdmissionApplication application = findAuthorized(applicationCode, request.documentNumber());
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new IllegalArgumentException("A cobrança da inscrição ainda não foi emitida."));

        if (invoice.getStatus() == AdmissionInvoiceStatus.EXPIRED
                || invoice.getStatus() == AdmissionInvoiceStatus.CANCELLED
                || (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now(LUANDA_ZONE)))) {
            throw new IllegalArgumentException(
                    "O prazo desta guia terminou. A candidatura foi marcada como desistência por falta de pagamento."
            );
        }

        return createPdf(application, invoice);
    }

    private AdmissionApplication findAuthorized(String applicationCode, String documentNumber) {
        String code = clean(applicationCode, null);
        String document = clean(documentNumber, null);
        if (code == null || document == null) {
            throw new IllegalArgumentException("Informe o código da candidatura e o número do documento.");
        }

        AdmissionApplication application = applicationRepository.findByApplicationCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Candidatura não encontrada ou documento inválido."));
        if (application.getDocumentNumber() == null
                || !application.getDocumentNumber().trim().equalsIgnoreCase(document)) {
            throw new IllegalArgumentException("Candidatura não encontrada ou documento inválido.");
        }
        return application;
    }

    private byte[] createPdf(AdmissionApplication application, AdmissionInvoice invoice) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(document, page, content, application, invoice);
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a Guia de Pagamento Académico.", exception);
        }
    }

    private void draw(
            PDDocument document,
            PDPage page,
            PDPageContentStream content,
            AdmissionApplication application,
            AdmissionInvoice invoice
    ) throws Exception {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 34;
        float width = pageWidth - margin * 2;
        LocalDateTime issuedAt = LocalDateTime.now(LUANDA_ZONE);
        String hash = documentHash(application, invoice, issuedAt);

        paintBackground(content, pageWidth, pageHeight);
        drawHeader(document, content, application.getInstitution(), margin, 790, width);
        drawTitle(content, pageWidth, 708);
        drawIdentity(content, invoice, margin, 655, width);
        drawCandidate(content, application, margin, 546, width);
        drawCharge(content, application, invoice, margin, 445, width);
        drawPayment(content, document, application, invoice, margin, 332, width);
        drawInstructions(content, margin, 208, width);
        drawValidation(content, document, application, invoice, issuedAt, hash, margin, 115, width);
        drawFooter(content, pageWidth, issuedAt);
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

    private void drawHeader(
            PDDocument document,
            PDPageContentStream content,
            Institution institution,
            float x,
            float y,
            float width
    ) throws Exception {
        box(content, x, y - 62, width, 58, Color.WHITE, BORDER);
        drawImetroLogo(document, content, x + 12, y - 54, 58, 44);
        drawText(content, institutionName(institution), PDType1Font.HELVETICA_BOLD, 9.8f, x + 78, y - 20, NAVY);
        drawText(content, "Secretaria Financeira | Gestão Académica e Financeira", PDType1Font.HELVETICA_BOLD, 7.6f, x + 78, y - 36, NAVY_2);
        drawText(content, "Documento emitido eletronicamente pelo SecretáriaPay", PDType1Font.HELVETICA, 6.8f, x + 78, y - 50, MUTED);
        drawSecretariaPayLogo(document, content, x + width - 84, y - 56, 66, 50);
    }

    private void drawTitle(PDPageContentStream content, float pageWidth, float y) throws Exception {
        drawCenteredText(content, "GUIA DE PAGAMENTO ACADÉMICO", PDType1Font.HELVETICA_BOLD, 18, pageWidth / 2, y, NAVY);
        drawCenteredText(content, "Taxa de inscrição académica", PDType1Font.HELVETICA, 8.3f, pageWidth / 2, y - 16, MUTED);
        pill(content, pageWidth / 2 - 47, y - 38, 94, 16, GOLD_LIGHT, GOLD, "PAGAMENTO ACADÉMICO", NAVY);
    }

    private void drawIdentity(
            PDPageContentStream content,
            AdmissionInvoice invoice,
            float x,
            float y,
            float width
    ) throws Exception {
        float gap = 8;
        float cardWidth = (width - gap) / 2;
        identityCard(content, "GUIA Nº", safe(invoice.getInvoiceCode()), x, y - 48, cardWidth, NAVY_2);
        identityCard(content, "EMISSÃO", LocalDate.now(LUANDA_ZONE).format(DATE_FORMATTER), x + cardWidth + gap, y - 48, cardWidth, BLUE);
        identityCard(content, "VENCIMENTO", formatDate(invoice.getDueDate()), x, y - 101, cardWidth, NAVY_2);
        identityCard(content, "SITUAÇÃO", invoiceStatus(invoice.getStatus()), x + cardWidth + gap, y - 101, cardWidth, GOLD);
    }

    private void drawCandidate(
            PDPageContentStream content,
            AdmissionApplication application,
            float x,
            float y,
            float width
    ) throws Exception {
        sectionHeader(content, "DADOS DO CANDIDATO", x, y, width);
        box(content, x, y - 91, width, 72, Color.WHITE, BORDER);
        float firstColumn = width * .52f;
        float secondColumn = width * .24f;

        infoPair(content, "NOME COMPLETO", application.getFullName(), x + 14, y - 37, firstColumn - 18);
        infoPair(content, "CANDIDATURA", application.getApplicationCode(), x + firstColumn + 6, y - 37, secondColumn - 12);
        infoPair(content, "DOCUMENTO", safe(application.getDocumentType()) + " " + safe(application.getDocumentNumber()), x + firstColumn + secondColumn + 2, y - 37, width - firstColumn - secondColumn - 16);
        infoPair(content, "CURSO", application.getDesiredCourse() == null ? "-" : application.getDesiredCourse().getName(), x + 14, y - 68, firstColumn - 18);
        infoPair(content, "TURNO", shiftLabel(application.getDesiredShift()), x + firstColumn + 6, y - 68, secondColumn - 12);
        infoPair(content, "ANO ACADÉMICO", application.getAcademicYear(), x + firstColumn + secondColumn + 2, y - 68, width - firstColumn - secondColumn - 16);
    }

    private void drawCharge(
            PDPageContentStream content,
            AdmissionApplication application,
            AdmissionInvoice invoice,
            float x,
            float y,
            float width
    ) throws Exception {
        sectionHeader(content, "DETALHES DA COBRANÇA", x, y, width);
        box(content, x, y - 103, width, 84, Color.WHITE, BORDER);

        String academicYear = safe(application.getAcademicYear());
        BigDecimal amount = invoice.getAmount() == null ? BigDecimal.ZERO : invoice.getAmount();
        infoPair(content, "DESCRIÇÃO", "Taxa de inscrição académica " + academicYear, x + 14, y - 38, width * .50f);
        infoPair(content, "REFERÊNCIA", "Inscrição " + academicYear, x + width * .58f, y - 38, width * .20f);
        infoPair(content, "VENCIMENTO", formatDate(invoice.getDueDate()), x + width * .82f, y - 38, width * .15f);
        infoPair(content, "VALOR BASE", formatMoney(amount, invoice.getCurrency()), x + 14, y - 76, width * .28f);
        infoPair(content, "ACRÉSCIMOS/AJUSTES", formatMoney(BigDecimal.ZERO, invoice.getCurrency()), x + width * .36f, y - 76, width * .28f);

        box(content, x + width * .69f, y - 93, width * .29f, 42, GOLD_LIGHT, GOLD);
        drawText(content, "TOTAL A PAGAR", PDType1Font.HELVETICA_BOLD, 7.2f, x + width * .71f, y - 67, NAVY);
        drawFittedText(content, formatMoney(amount, invoice.getCurrency()), PDType1Font.HELVETICA_BOLD, 17, 12, x + width * .71f, y - 87, width * .25f, GOLD);
    }

    private void drawPayment(
            PDPageContentStream content,
            PDDocument document,
            AdmissionApplication application,
            AdmissionInvoice invoice,
            float x,
            float y,
            float width
    ) throws Exception {
        sectionHeader(content, "DADOS PARA PAGAMENTO", x, y, width);
        box(content, x, y - 103, width, 84, LIGHT, BORDER);
        float qrArea = 112;
        float textWidth = width - qrArea - 20;

        infoPair(content, "BENEFICIÁRIO", accountHolder, x + 14, y - 37, textWidth * .45f);
        infoPair(content, "IBAN", iban, x + textWidth * .48f, y - 37, textWidth * .49f);
        infoPair(content, "BANCO", bankName, x + 14, y - 67, textWidth * .45f);
        infoPair(content, "Nº DA CONTA (AKZ)", accountNumber, x + textWidth * .48f, y - 67, textWidth * .49f);
        drawWrappedText(
                content,
                "Referência de pagamento: " + paymentReference(invoice)
                        + " | Em transferência BAI, informe esta referência na descrição da operação.",
                PDType1Font.HELVETICA_BOLD,
                7.1f,
                x + 14,
                y - 88,
                textWidth - 8,
                9,
                NAVY
        );

        BufferedImage qrImage = createQr(buildPaymentPayload(application, invoice));
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
        content.drawImage(qr, x + width - 94, y - 95, 78, 78);
        drawCenteredText(content, "QR PARA PAGAMENTO", PDType1Font.HELVETICA_BOLD, 6.3f, x + width - 55, y - 13, NAVY);
    }

    private void drawInstructions(PDPageContentStream content, float x, float y, float width) throws Exception {
        sectionHeader(content, "INSTRUÇÕES IMPORTANTES", x, y, width);
        box(content, x, y - 75, width, 56, NAVY, NAVY);
        drawWrappedText(
                content,
                "1. Efetue o pagamento até à data de vencimento.  2. Na transferência BAI, informe a referência de pagamento na descrição.  3. Após o pagamento, envie o comprovativo pelo portal ou WhatsApp oficial.  4. Sem pagamento ou comprovativo dentro do prazo, a candidatura será marcada como desistência.",
                PDType1Font.HELVETICA_BOLD,
                7.4f,
                x + 14,
                y - 36,
                width - 28,
                10.5f,
                Color.WHITE
        );
    }

    private void drawValidation(
            PDPageContentStream content,
            PDDocument document,
            AdmissionApplication application,
            AdmissionInvoice invoice,
            LocalDateTime issuedAt,
            String hash,
            float x,
            float y,
            float width
    ) throws Exception {
        float gap = 10;
        float leftWidth = width * .62f;
        float rightWidth = width - leftWidth - gap;

        sectionHeader(content, "VALIDAÇÃO DIGITAL", x, y, leftWidth);
        box(content, x, y - 75, leftWidth, 56, Color.WHITE, BORDER);
        pill(content, x + 14, y - 47, 98, 17, new Color(233, 248, 242), GREEN, "EMITIDO DIGITALMENTE", GREEN);
        drawFittedText(content, "Cobrança: " + invoice.getInvoiceCode(), PDType1Font.HELVETICA_BOLD, 7.4f, 5.9f, x + 14, y - 64, leftWidth - 90, NAVY);
        drawText(content, "Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER), PDType1Font.HELVETICA, 6.4f, x + 14, y - 73, MUTED);

        BufferedImage validationQrImage = createQr(validationPayload(application, invoice, hash));
        PDImageXObject validationQr = LosslessFactory.createFromImage(document, validationQrImage);
        content.drawImage(validationQr, x + leftWidth - 67, y - 70, 52, 52);

        sectionHeader(content, "ATENDIMENTO FINANCEIRO", x + leftWidth + gap, y, rightWidth);
        box(content, x + leftWidth + gap, y - 75, rightWidth, 56, GOLD_LIGHT, new Color(236, 207, 126));
        drawFittedText(content, "WhatsApp: " + whatsapp, PDType1Font.HELVETICA_BOLD, 7.2f, 5.8f, x + leftWidth + gap + 12, y - 39, rightWidth - 24, NAVY);
        drawFittedText(content, financialEmail, PDType1Font.HELVETICA_BOLD, 6.5f, 5.2f, x + leftWidth + gap + 12, y - 52, rightWidth - 24, NAVY);
        drawText(content, "Hash: " + hash.substring(0, Math.min(18, hash.length())), PDType1Font.HELVETICA, 5.8f, x + leftWidth + gap + 12, y - 68, MUTED);
    }

    private void drawFooter(PDPageContentStream content, float pageWidth, LocalDateTime issuedAt) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(0, 0, pageWidth, 38);
        content.fill();
        drawText(content, "IMETRO - Secretaria Financeira", PDType1Font.HELVETICA_BOLD, 6.7f, 34, 24, Color.WHITE);
        drawCenteredText(content, INSTITUTIONAL_SITE + " | " + financialEmail, PDType1Font.HELVETICA, 6.2f, pageWidth / 2, 24, Color.WHITE);
        drawText(content, "Powered by SecretáriaPay | TRIA Company", PDType1Font.HELVETICA_BOLD, 6.4f, pageWidth - 190, 24, Color.WHITE);
        drawCenteredText(content, "Documento financeiro | Versão " + DOCUMENT_VERSION + " | " + issuedAt.format(DATE_TIME_FORMATTER), PDType1Font.HELVETICA, 5.6f, pageWidth / 2, 10, new Color(220, 228, 240));
    }

    private void drawImetroLogo(
            PDDocument document,
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height
    ) throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("static/assets/imetro.png");
            if (resource.exists()) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(
                        document,
                        resource.getInputStream().readAllBytes(),
                        "imetro-logo"
                );
                content.drawImage(logo, x, y, width, height);
                return;
            }
        } catch (Exception ignored) {
        }
        drawText(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 14, x, y + 16, GOLD);
    }

    private void drawSecretariaPayLogo(
            PDDocument document,
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height
    ) throws Exception {
        String[] candidates = {
                "static/assets/secretariapay-imetro.png",
                "static/assets/secretariapay-logo.png",
                "static/assets/logo-secretariapay.png",
                "static/assets/secretariapay.png"
        };

        for (String path : candidates) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    PDImageXObject logo = PDImageXObject.createFromByteArray(
                            document,
                            resource.getInputStream().readAllBytes(),
                            path
                    );
                    content.drawImage(logo, x, y, width, height);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        drawSecretariaPayBrand(content, x - 50, y + 10);
    }

    private void drawSecretariaPayBrand(PDPageContentStream content, float x, float y) throws Exception {
        box(content, x, y, 28, 28, NAVY, GOLD);
        drawCenteredText(content, "SP", PDType1Font.HELVETICA_BOLD, 8.5f, x + 14, y + 9, Color.WHITE);
        drawText(content, "Secretária", PDType1Font.HELVETICA_BOLD, 11.5f, x + 36, y + 14, NAVY);
        drawText(content, "Pay", PDType1Font.HELVETICA_BOLD, 11.5f, x + 92, y + 14, GOLD);
        drawText(content, "GESTÃO FINANCEIRA ACADÉMICA", PDType1Font.HELVETICA_BOLD, 4.8f, x + 36, y + 3, MUTED);
    }

    private void sectionHeader(
            PDPageContentStream content,
            String title,
            float x,
            float y,
            float width
    ) throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, y - 18, width, 18);
        content.fill();
        content.setNonStrokingColor(GOLD);
        content.addRect(x, y - 18, 5, 18);
        content.fill();
        drawText(content, title, PDType1Font.HELVETICA_BOLD, 8.2f, x + 13, y - 13, Color.WHITE);
    }

    private void identityCard(
            PDPageContentStream content,
            String label,
            String value,
            float x,
            float y,
            float width,
            Color accent
    ) throws Exception {
        box(content, x, y, width, 44, Color.WHITE, BORDER);
        content.setNonStrokingColor(accent);
        content.addRect(x, y, 5, 44);
        content.fill();
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 7, x + 13, y + 29, MUTED);
        drawFittedText(content, value, PDType1Font.HELVETICA_BOLD, 10.5f, 7, x + 13, y + 11, width - 23, NAVY);
    }

    private void infoPair(
            PDPageContentStream content,
            String label,
            Object value,
            float x,
            float y,
            float maxWidth
    ) throws Exception {
        drawText(content, label, PDType1Font.HELVETICA_BOLD, 6.7f, x, y + 9, MUTED);
        drawFittedText(content, safe(value), PDType1Font.HELVETICA_BOLD, 8.3f, 6.3f, x, y - 2, maxWidth, NAVY);
    }

    private void box(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            Color fill,
            Color stroke
    ) throws Exception {
        content.setNonStrokingColor(fill);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(stroke);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void pill(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            Color fill,
            Color stroke,
            String label,
            Color textColor
    ) throws Exception {
        box(content, x, y, width, height, fill, stroke);
        drawCenteredText(content, label, PDType1Font.HELVETICA_BOLD, 6.4f, x + width / 2, y + 5.5f, textColor);
    }

    private BufferedImage createQr(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(
                value == null || value.isBlank() ? "-" : value,
                BarcodeFormat.QR_CODE,
                420,
                420
        );
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String buildPaymentPayload(AdmissionApplication application, AdmissionInvoice invoice) {
        return "SECRETARIAPAY|TYPE=ADMISSION_PAYMENT"
                + "|APPLICATION=" + safe(application.getApplicationCode())
                + "|INVOICE=" + safe(invoice.getInvoiceCode())
                + "|REFERENCE=" + paymentReference(invoice)
                + "|AMOUNT=" + amountPlain(invoice.getAmount())
                + "|CURRENCY=" + safe(invoice.getCurrency())
                + "|DUE_DATE=" + safe(invoice.getDueDate())
                + "|IBAN=" + iban.replace(" ", "")
                + "|ACCOUNT=" + accountNumber;
    }

    private String validationPayload(
            AdmissionApplication application,
            AdmissionInvoice invoice,
            String hash
    ) {
        return "SECRETARIAPAY|TYPE=ADMISSION_GUIDE_VALIDATION"
                + "|APPLICATION=" + safe(application.getApplicationCode())
                + "|INVOICE=" + safe(invoice.getInvoiceCode())
                + "|VERSION=" + DOCUMENT_VERSION
                + "|HASH=" + hash;
    }

    private String documentHash(
            AdmissionApplication application,
            AdmissionInvoice invoice,
            LocalDateTime issuedAt
    ) throws Exception {
        String raw = safe(application.getApplicationCode())
                + "|" + safe(application.getDocumentNumber())
                + "|" + safe(invoice.getInvoiceCode())
                + "|" + amountPlain(invoice.getAmount())
                + "|" + safe(invoice.getDueDate())
                + "|" + issuedAt;
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)))
                .toUpperCase(Locale.ROOT);
    }

    private String paymentReference(AdmissionInvoice invoice) {
        if (invoice.getPaymentReference() != null && !invoice.getPaymentReference().isBlank()) {
            return invoice.getPaymentReference().trim();
        }
        String code = safe(invoice.getInvoiceCode()).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (code.length() > 18) code = code.substring(code.length() - 18);
        return "SPAY-BAI-" + code;
    }

    private String institutionName(Institution institution) {
        if (institution != null && institution.getName() != null && !institution.getName().isBlank()) {
            return institution.getName();
        }
        return "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";
    }

    private String invoiceStatus(AdmissionInvoiceStatus status) {
        if (status == null) return "PENDENTE";
        return switch (status) {
            case PAID -> "PAGO";
            case UNDER_REVIEW -> "EM ANÁLISE";
            case EXPIRED -> "EXPIRADO";
            case CANCELLED -> "CANCELADO";
            default -> "PENDENTE";
        };
    }

    private String shiftLabel(String shift) {
        if (shift == null || shift.isBlank()) return "-";
        return switch (shift.trim().toUpperCase(Locale.ROOT)) {
            case "MANHA", "MANHÃ" -> "Manhã";
            case "TARDE" -> "Tarde";
            case "NOITE" -> "Noite";
            default -> shift;
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal value, String currency) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        String suffix = "AOA".equalsIgnoreCase(safe(currency)) ? " Kz" : " " + safe(currency);
        return formatter.format(value == null ? BigDecimal.ZERO : value) + suffix;
    }

    private String amountPlain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private String safe(Object value) {
        if (value == null) return "-";
        String text = value.toString().trim();
        return text.isBlank() ? "-" : text;
    }

    private void drawText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            float size,
            float x,
            float y,
            Color color
    ) throws Exception {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(toPdfText(text));
        content.endText();
    }

    private void drawCenteredText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            float size,
            float centerX,
            float y,
            Color color
    ) throws Exception {
        String normalized = toPdfText(text);
        float width = font.getStringWidth(normalized) / 1000 * size;
        drawText(content, normalized, font, size, centerX - width / 2, y, color);
    }

    private void drawFittedText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            float preferredSize,
            float minimumSize,
            float x,
            float y,
            float maxWidth,
            Color color
    ) throws Exception {
        String normalized = toPdfText(text);
        float size = preferredSize;
        while (size > minimumSize && font.getStringWidth(normalized) / 1000 * size > maxWidth) {
            size -= .3f;
        }
        if (font.getStringWidth(normalized) / 1000 * size > maxWidth) {
            normalized = ellipsize(normalized, font, size, maxWidth);
        }
        drawText(content, normalized, font, size, x, y, color);
    }

    private void drawWrappedText(
            PDPageContentStream content,
            String text,
            PDType1Font font,
            float size,
            float x,
            float y,
            float maxWidth,
            float lineHeight,
            Color color
    ) throws Exception {
        String[] words = toPdfText(text).split("\\s+");
        StringBuilder line = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * size > maxWidth && !line.isEmpty()) {
                drawText(content, line.toString(), font, size, x, currentY, color);
                currentY -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            drawText(content, line.toString(), font, size, x, currentY, color);
        }
    }

    private String ellipsize(String text, PDType1Font font, float size, float maxWidth) throws Exception {
        String suffix = "...";
        String candidate = text;
        while (!candidate.isEmpty()
                && font.getStringWidth(candidate + suffix) / 1000 * size > maxWidth) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate + suffix;
    }

    private String toPdfText(String value) {
        if (value == null) return "-";
        return value
                .replace('–', '-')
                .replace('—', '-')
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"');
    }
}
