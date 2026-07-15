package com.secretariapay.api.service.branding;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialDocumentBrandingAspectTest {

    private final OfficialDocumentBrandingAspect branding = new OfficialDocumentBrandingAspect();

    @Test
    void shouldApplyClientBrandingToPaymentGuide() throws Exception {
        byte[] original = blankPdf(PDRectangle.A4);

        byte[] branded = branding.applyBranding(
                original,
                OfficialDocumentBrandingAspect.DocumentType.PAYMENT_GUIDE
        );

        assertValidBrandedPdf(original, branded, PDRectangle.A4);
    }

    @Test
    void shouldApplyClientBrandingToAcademicDocument() throws Exception {
        byte[] original = blankPdf(PDRectangle.A4);

        byte[] branded = branding.applyBranding(
                original,
                OfficialDocumentBrandingAspect.DocumentType.ACADEMIC_DOCUMENT
        );

        assertValidBrandedPdf(original, branded, PDRectangle.A4);
    }

    @Test
    void shouldApplyClientBrandingToLandscapeFinancialStatement() throws Exception {
        PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        byte[] original = blankPdf(landscapeA4);

        byte[] branded = branding.applyBranding(
                original,
                OfficialDocumentBrandingAspect.DocumentType.FINANCIAL_STATEMENT
        );

        assertValidBrandedPdf(original, branded, landscapeA4);
    }

    @Test
    void shouldRenderBothApprovedMarksWithVisiblePixels() {
        BufferedImage imetro = branding.renderImetroLogo(720, 430);
        BufferedImage secretariaPay = branding.renderSecretariaPayMetroLogo(600, 900);

        assertTrue(countVisiblePixels(imetro) > 20_000, "A marca UM deve possuir conteúdo visível.");
        assertTrue(countVisiblePixels(secretariaPay) > 20_000, "A marca SecretariaPay METRO deve possuir conteúdo visível.");
    }

    private byte[] blankPdf(PDRectangle format) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(format);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setNonStrokingColor(255, 255, 255);
                content.addRect(0, 0, format.getWidth(), format.getHeight());
                content.fill();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void assertValidBrandedPdf(byte[] original, byte[] branded, PDRectangle expectedFormat) throws Exception {
        assertTrue(branded.length > 1_000, "O PDF final deve conter as imagens institucionais.");
        assertNotEquals(original.length, branded.length, "A identidade visual deve modificar o PDF.");

        try (PDDocument document = PDDocument.load(branded)) {
            assertEquals(1, document.getNumberOfPages());
            assertEquals(expectedFormat.getWidth(), document.getPage(0).getMediaBox().getWidth(), 0.1f);
            assertEquals(expectedFormat.getHeight(), document.getPage(0).getMediaBox().getHeight(), 0.1f);
        }
    }

    private long countVisiblePixels(BufferedImage image) {
        long visible = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 15) {
                    visible++;
                }
            }
        }
        return visible;
    }
}
