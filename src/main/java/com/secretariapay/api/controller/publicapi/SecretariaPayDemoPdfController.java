package com.secretariapay.api.controller.publicapi;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/demo")
public class SecretariaPayDemoPdfController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @GetMapping("/payment-guides/{demoCode}/pdf")
    public ResponseEntity<byte[]> paymentGuide(
            @PathVariable String demoCode,
            @RequestParam(defaultValue = "Estudante demonstrativo") String student,
            @RequestParam(defaultValue = "mes atual") String month,
            @RequestParam(defaultValue = "Multicaixa Express") String method
    ) throws IOException {
        byte[] pdf = buildPdf(
                "GUIA DE PAGAMENTO DEMONSTRATIVA",
                demoCode,
                student,
                month,
                method,
                "45.000,00 Kz",
                "Pendente de confirmacao demonstrativa",
                "Esta guia foi emitida em ambiente de demonstracao do SecretariaPay Academico."
        );
        return pdfResponse(pdf, "guia-pagamento-demo-" + safeFile(demoCode) + ".pdf");
    }

    @GetMapping("/receipts/{receiptCode}/pdf")
    public ResponseEntity<byte[]> receipt(
            @PathVariable String receiptCode,
            @RequestParam(defaultValue = "Estudante demonstrativo") String student,
            @RequestParam(defaultValue = "mes atual") String month,
            @RequestParam(defaultValue = "Multicaixa Express") String method
    ) throws IOException {
        byte[] pdf = buildPdf(
                "RECIBO DE PAGAMENTO DEMONSTRATIVO",
                receiptCode,
                student,
                month,
                method,
                "45.000,00 Kz",
                "Pagamento confirmado na demonstracao",
                "Este recibo comprova a simulacao de pagamento no SecretariaPay Academico."
        );
        return pdfResponse(pdf, "recibo-demo-" + safeFile(receiptCode) + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName).build().toString())
                .body(pdf);
    }

    private byte[] buildPdf(
            String title,
            String code,
            String student,
            String month,
            String method,
            String amount,
            String status,
            String footer
    ) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(55, y);
                content.showText("SecretariaPay Academico / IMETRO");
                content.endText();

                y -= 38;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 15);
                content.newLineAtOffset(55, y);
                content.showText(clean(title));
                content.endText();

                y -= 38;
                List<String> lines = new ArrayList<>();
                lines.add("Codigo: " + clean(code));
                lines.add("Estudante/Cadastro: " + clean(student));
                lines.add("Mes de referencia: " + clean(month));
                lines.add("Forma de pagamento: " + clean(method));
                lines.add("Valor: " + clean(amount));
                lines.add("Estado: " + clean(status));
                lines.add("Emitido em: " + LocalDateTime.now().format(FORMATTER));

                for (String line : lines) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(55, y);
                    content.showText(line);
                    content.endText();
                    y -= 24;
                }

                y -= 18;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_OBLIQUE, 11);
                content.newLineAtOffset(55, y);
                content.showText(clean(footer));
                content.endText();

                y -= 50;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(55, y);
                content.showText("Documento gerado automaticamente para demonstracao institucional.");
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replaceAll("[^A-Za-z0-9 ./,:;_@()+-]", " ").replaceAll("\\s+", " ").trim();
    }

    private String safeFile(String value) {
        if (value == null || value.isBlank()) return "documento";
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
