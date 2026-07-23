package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AdmissionPaymentGuidePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color NAVY = new Color(5, 31, 72);
    private static final Color BLUE = new Color(17, 148, 221);
    private static final Color GOLD = new Color(224, 170, 29);
    private static final Color LIGHT = new Color(244, 248, 252);
    private static final Color BORDER = new Color(198, 211, 226);
    private static final Color MUTED = new Color(79, 97, 119);
    private static final Color GREEN = new Color(20, 130, 87);

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
                || (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now()))) {
            throw new IllegalArgumentException("O prazo desta guia terminou. A candidatura foi marcada como desistência por falta de pagamento.");
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
            throw new IllegalStateException("Não foi possível gerar a Guia de Pagamento da Inscrição.", exception);
        }
    }

    private void draw(
            PDDocument document,
            PDPage page,
            PDPageContentStream content,
            AdmissionApplication application,
            AdmissionInvoice invoice
    ) throws Exception {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float margin = 38;
        float bodyWidth = width - margin * 2;

        fill(content, 0, 0, width, height, Color.WHITE);
        fill(content, 0, height - 18, width, 18, NAVY);
        fill(content, 0, height - 23, width, 5, GOLD);

        drawLogo(document, content, margin, 747, 62, 48);
        text(content, institutionName(application), PDType1Font.HELVETICA_BOLD, 11, margin + 78, 780, NAVY);
        text(content, "Secretaria Financeira | Admissoes 2026/2027", PDType1Font.HELVETICA_BOLD, 8, margin + 78, 763, BLUE);
        text(content, "Documento emitido eletronicamente pelo SecretariaPay", PDType1Font.HELVETICA, 7, margin + 78, 748, MUTED);
        text(content, "GUIA DE PAGAMENTO DA INSCRICAO", PDType1Font.HELVETICA_BOLD, 19, margin, 705, NAVY);
        text(content, "Taxa oficial da candidatura academica", PDType1Font.HELVETICA, 9, margin, 687, MUTED);

        box(content, margin, 624, bodyWidth, 46, LIGHT, BORDER);
        info(content, "GUIA", invoice.getInvoiceCode(), margin + 14, 650, bodyWidth * .45f);
        info(content, "VENCIMENTO", date(invoice.getDueDate()), margin + bodyWidth * .54f, 650, bodyWidth * .20f);
        info(content, "ESTADO", status(invoice.getStatus()), margin + bodyWidth * .78f, 650, bodyWidth * .18f);

        section(content, "DADOS DO CANDIDATO", margin, 590, bodyWidth);
        box(content, margin, 505, bodyWidth, 70, Color.WHITE, BORDER);
        info(content, "NOME COMPLETO", application.getFullName(), margin + 14, 550, bodyWidth * .50f);
        info(content, "DOCUMENTO", safe(application.getDocumentType()) + " " + safe(application.getDocumentNumber()), margin + bodyWidth * .55f, 550, bodyWidth * .40f);
        info(content, "CURSO", application.getDesiredCourse() == null ? "-" : application.getDesiredCourse().getName(), margin + 14, 518, bodyWidth * .50f);
        info(content, "TURNO / ANO ACADEMICO", safe(application.getDesiredShift()) + " | " + safe(application.getAcademicYear()), margin + bodyWidth * .55f, 518, bodyWidth * .40f);

        section(content, "DETALHES DA COBRANCA", margin, 470, bodyWidth);
        box(content, margin, 390, bodyWidth, 64, LIGHT, BORDER);
        info(content, "DESCRICAO", "Taxa de inscricao 2026/2027", margin + 14, 430, bodyWidth * .42f);
        info(content, "REFERENCIA", invoice.getPaymentReference() == null ? invoice.getInvoiceCode() : invoice.getPaymentReference(), margin + bodyWidth * .48f, 430, bodyWidth * .28f);
        info(content, "VALOR", money(invoice.getAmount(), invoice.getCurrency()), margin + bodyWidth * .80f, 430, bodyWidth * .17f);
        text(content, "Prazo: efetue o pagamento e envie o comprovativo ate " + date(invoice.getDueDate()) + ".", PDType1Font.HELVETICA_BOLD, 8.5f, margin + 14, 402, NAVY);

        section(content, "DADOS PARA PAGAMENTO", margin, 355, bodyWidth);
        box(content, margin, 250, bodyWidth, 88, NAVY, NAVY);
        infoLight(content, "BANCO", bankName, margin + 14, 312, bodyWidth * .42f);
        infoLight(content, "BENEFICIARIO", accountHolder, margin + bodyWidth * .50f, 312, bodyWidth * .45f);
        infoLight(content, "CONTA AKZ", accountNumber, margin + 14, 278, bodyWidth * .42f);
        infoLight(content, "IBAN", iban, margin + bodyWidth * .50f, 278, bodyWidth * .45f);

        box(content, margin, 148, bodyWidth, 78, new Color(255, 248, 220), new Color(226, 185, 55));
        text(content, "INSTRUCOES IMPORTANTES", PDType1Font.HELVETICA_BOLD, 8, margin + 14, 207, NAVY);
        wrapped(content,
                "1. Pague ate a data de vencimento. 2. Informe a referencia da guia na transferencia. 3. Depois do pagamento, envie o comprovativo no portal ou pelo WhatsApp oficial. 4. Sem pagamento ou comprovativo dentro do prazo, a candidatura sera marcada como desistente.",
                PDType1Font.HELVETICA_BOLD, 8.2f, margin + 14, 188, bodyWidth - 28, 12, NAVY);

        box(content, margin, 78, bodyWidth, 52, Color.WHITE, BORDER);
        text(content, "ATENDIMENTO OFICIAL", PDType1Font.HELVETICA_BOLD, 7.5f, margin + 14, 112, MUTED);
        text(content, "WhatsApp: " + whatsapp + " | " + financialEmail, PDType1Font.HELVETICA_BOLD, 8.2f, margin + 14, 94, NAVY);
        text(content, "Candidatura: " + application.getApplicationCode(), PDType1Font.HELVETICA, 7, margin + bodyWidth * .64f, 94, MUTED);

        fill(content, 0, 0, width, 38, NAVY);
        text(content, "IMETRO - Secretaria Financeira", PDType1Font.HELVETICA_BOLD, 6.8f, margin, 23, Color.WHITE);
        text(content, "Powered by SecretariaPay | TRIA Company", PDType1Font.HELVETICA_BOLD, 6.8f, width - 220, 23, Color.WHITE);
    }

    private void drawLogo(PDDocument document, PDPageContentStream content, float x, float y, float w, float h) throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource("static/assets/imetro.png");
            if (resource.exists()) {
                PDImageXObject image = PDImageXObject.createFromByteArray(document, resource.getInputStream().readAllBytes(), "imetro");
                content.drawImage(image, x, y, w, h);
                return;
            }
        } catch (Exception ignored) {
        }
        text(content, "IMETRO", PDType1Font.HELVETICA_BOLD, 15, x, y + 18, GOLD);
    }

    private void section(PDPageContentStream content, String title, float x, float y, float width) throws Exception {
        fill(content, x, y - 18, width, 18, NAVY);
        fill(content, x, y - 18, 5, 18, GOLD);
        text(content, title, PDType1Font.HELVETICA_BOLD, 8, x + 13, y - 13, Color.WHITE);
    }

    private void info(PDPageContentStream content, String label, Object value, float x, float y, float maxWidth) throws Exception {
        text(content, label, PDType1Font.HELVETICA_BOLD, 6.7f, x, y, MUTED);
        fitted(content, safe(value), PDType1Font.HELVETICA_BOLD, 9.2f, 6.5f, x, y - 15, maxWidth, NAVY);
    }

    private void infoLight(PDPageContentStream content, String label, Object value, float x, float y, float maxWidth) throws Exception {
        text(content, label, PDType1Font.HELVETICA_BOLD, 6.7f, x, y, new Color(170, 191, 215));
        fitted(content, safe(value), PDType1Font.HELVETICA_BOLD, 9.2f, 6.5f, x, y - 15, maxWidth, Color.WHITE);
    }

    private void box(PDPageContentStream content, float x, float y, float width, float height, Color fill, Color stroke) throws Exception {
        fill(content, x, y, width, height, fill);
        content.setStrokingColor(stroke);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void fill(PDPageContentStream content, float x, float y, float width, float height, Color color) throws Exception {
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void text(PDPageContentStream content, String value, PDType1Font font, float size, float x, float y, Color color) throws Exception {
        content.beginText();
        content.setNonStrokingColor(color);
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(pdfSafe(value));
        content.endText();
    }

    private void fitted(PDPageContentStream content, String value, PDType1Font font, float start, float min, float x, float y, float maxWidth, Color color) throws Exception {
        String safe = pdfSafe(value);
        float size = start;
        while (size > min && font.getStringWidth(safe) / 1000f * size > maxWidth) size -= .3f;
        text(content, safe, font, size, x, y, color);
    }

    private void wrapped(PDPageContentStream content, String value, PDType1Font font, float size, float x, float y, float maxWidth, float lineHeight, Color color) throws Exception {
        String[] words = pdfSafe(value).split("\\s+");
        StringBuilder line = new StringBuilder();
        float cursor = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000f * size > maxWidth && line.length() > 0) {
                text(content, line.toString(), font, size, x, cursor, color);
                cursor -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) text(content, line.toString(), font, size, x, cursor, color);
    }

    private String institutionName(AdmissionApplication application) {
        if (application.getInstitution() != null && application.getInstitution().getLegalName() != null
                && !application.getInstitution().getLegalName().isBlank()) {
            return application.getInstitution().getLegalName();
        }
        return "Instituto Superior Politecnico Metropolitano de Angola";
    }

    private String status(AdmissionInvoiceStatus value) {
        if (value == null) return "PENDENTE";
        return switch (value) {
            case PAID -> "PAGO";
            case UNDER_REVIEW -> "EM ANALISE";
            case EXPIRED -> "VENCIDO";
            case CANCELLED -> "CANCELADO";
            default -> "PENDENTE";
        };
    }

    private String money(BigDecimal value, String currency) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value == null ? BigDecimal.ZERO : value) + " " + ("AOA".equalsIgnoreCase(currency) ? "Kz" : safe(currency));
    }

    private String date(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMAT);
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value).trim();
    }

    private String pdfSafe(String value) {
        String source = safe(value);
        StringBuilder result = new StringBuilder(source.length());
        for (char character : source.toCharArray()) {
            result.append(character >= 32 && character <= 255 ? character : '?');
        }
        return result.toString();
    }
}
