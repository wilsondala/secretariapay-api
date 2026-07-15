package com.secretariapay.api.service.branding;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Aplica as artes originais aprovadas pelo cliente aos PDFs oficiais sem
 * alterar dados, cálculos, QR Codes, hashes, permissões ou rotas existentes.
 *
 * A sobreposição é executada depois que cada serviço termina de gerar o PDF.
 * Se a aplicação da marca falhar, o documento original continua sendo entregue.
 */
@Aspect
@Component
public class OfficialDocumentBrandingAspect {

    private static final Logger log = LoggerFactory.getLogger(OfficialDocumentBrandingAspect.class);

    private static final String IMETRO_ORIGINAL =
            "static/assets/branding/imetro-um-original.png";
    private static final String SECRETARIAPAY_ORIGINAL =
            "static/assets/branding/secretariapay-metro-original.png";

    /**
     * Recursos anteriores usados apenas como fallback seguro enquanto uma
     * instalação ainda não recebeu os PNGs originais aprovados.
     */
    private static final String IMETRO_FALLBACK = "static/assets/imetro.png";
    private static final String SECRETARIAPAY_FALLBACK = "static/branding/secretariapay-logo.png";

    @Around("execution(byte[] com.secretariapay.api.service.financial.PaymentGuidePdfService.generateByChargeId(..)) || " +
            "execution(byte[] com.secretariapay.api.service.financial.PaymentGuidePdfService.generateByChargeCode(..))")
    public Object brandPaymentGuide(ProceedingJoinPoint joinPoint) throws Throwable {
        return brand(joinPoint.proceed(), DocumentType.PAYMENT_GUIDE);
    }

    @Around("execution(byte[] com.secretariapay.api.service.financial.ReceiptPdfService.generateReceiptPdf(..))")
    public Object brandFinancialDocument(ProceedingJoinPoint joinPoint) throws Throwable {
        return brand(joinPoint.proceed(), DocumentType.FINANCIAL_STATEMENT);
    }

    @Around("execution(byte[] com.secretariapay.api.service.academic.AcademicDocumentPdfService.generate(..))")
    public Object brandAcademicDocument(ProceedingJoinPoint joinPoint) throws Throwable {
        return brand(joinPoint.proceed(), DocumentType.ACADEMIC_DOCUMENT);
    }

    private Object brand(Object generated, DocumentType type) {
        if (!(generated instanceof byte[] pdf) || pdf.length == 0) {
            return generated;
        }

        try {
            return applyBranding(pdf, type);
        } catch (Exception exception) {
            log.warn(
                    "Não foi possível aplicar as marcas originais ao documento {}. O PDF original será mantido.",
                    type,
                    exception
            );
            return generated;
        }
    }

    /**
     * Visível para testes unitários e futuras rotinas institucionais de reemissão.
     */
    public byte[] applyBranding(byte[] sourcePdf, DocumentType type) throws Exception {
        try (PDDocument document = PDDocument.load(sourcePdf);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            if (document.getNumberOfPages() == 0) {
                return sourcePdf;
            }

            PDPage page = document.getPage(0);
            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    page,
                    AppendMode.APPEND,
                    true,
                    true
            )) {
                switch (type) {
                    case PAYMENT_GUIDE -> overlayPortraitHeader(document, content, page, false);
                    case ACADEMIC_DOCUMENT -> overlayPortraitHeader(document, content, page, true);
                    case FINANCIAL_STATEMENT -> overlayLandscapeHeader(document, content, page);
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private void overlayPortraitHeader(
            PDDocument document,
            PDPageContentStream content,
            PDPage page,
            boolean academicDocument
    ) throws Exception {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float headerBottom = pageHeight - 114;

        /*
         * Apaga exclusivamente as marcas antigas. A máscara esquerda termina
         * antes do início do nome da instituição, evitando o corte de letras.
         */
        float leftLogoX = academicDocument ? 52 : 44;
        fillWhite(content, leftLogoX - 2, headerBottom + 7, 66, 50);

        float oldRightBrandX = academicDocument ? pageWidth - 196 : pageWidth - 187;
        fillWhite(content, oldRightBrandX, headerBottom + 9, 151, 36);

        PDImageXObject imetro = LosslessFactory.createFromImage(
                document,
                renderImetroLogo(900, 600)
        );
        PDImageXObject secretariaPay = LosslessFactory.createFromImage(
                document,
                renderSecretariaPayMetroLogo(600, 900)
        );

        // Proporção original UM: 3:2.
        content.drawImage(imetro, leftLogoX, headerBottom + 9, 63, 42);

        // Proporção original SecretariaPay IMETRO: 2:3, sem distorção.
        float secretariaWidth = academicDocument ? 48 : 50;
        float secretariaHeight = secretariaWidth * 1.5f;
        content.drawImage(
                secretariaPay,
                pageWidth - secretariaWidth - 48,
                headerBottom - 12,
                secretariaWidth,
                secretariaHeight
        );
    }

    private void overlayLandscapeHeader(
            PDDocument document,
            PDPageContentStream content,
            PDPage page
    ) throws Exception {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        // Áreas exatas das marcas antigas do borderô A4 horizontal.
        fillWhite(content, 26, pageHeight - 93, 130, 70);
        fillWhite(content, pageWidth - 207, pageHeight - 89, 178, 62);

        PDImageXObject imetro = LosslessFactory.createFromImage(
                document,
                renderImetroLogo(900, 600)
        );
        PDImageXObject secretariaPay = LosslessFactory.createFromImage(
                document,
                renderSecretariaPayMetroLogo(600, 900)
        );

        content.drawImage(imetro, 38, pageHeight - 91, 105, 70);
        content.drawImage(secretariaPay, pageWidth - 78, pageHeight - 101, 52, 78);
    }

    private void fillWhite(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height
    ) throws Exception {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(x, y, width, height);
        content.fill();
    }

    /**
     * Carrega a arte UM original e apenas a redimensiona, sem redesenho,
     * alteração de cores ou deformação de proporção.
     */
    BufferedImage renderImetroLogo(int width, int height) {
        return renderResourcePreservingAspect(
                width,
                height,
                IMETRO_ORIGINAL,
                IMETRO_FALLBACK
        );
    }

    /**
     * Carrega a arte SecretariaPay IMETRO original e apenas a redimensiona,
     * sem substituir símbolo, tipografia, relevo ou cores.
     */
    BufferedImage renderSecretariaPayMetroLogo(int width, int height) {
        return renderResourcePreservingAspect(
                width,
                height,
                SECRETARIAPAY_ORIGINAL,
                SECRETARIAPAY_FALLBACK
        );
    }

    private BufferedImage renderResourcePreservingAspect(
            int targetWidth,
            int targetHeight,
            String preferredResource,
            String fallbackResource
    ) {
        BufferedImage source = readFirstAvailable(preferredResource, fallbackResource);
        BufferedImage output = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        double scale = Math.min(
                targetWidth / (double) source.getWidth(),
                targetHeight / (double) source.getHeight()
        );
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int drawX = (targetWidth - drawWidth) / 2;
        int drawY = (targetHeight - drawHeight) / 2;

        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.drawImage(source, drawX, drawY, drawWidth, drawHeight, null);
        } finally {
            graphics.dispose();
        }

        return output;
    }

    private BufferedImage readFirstAvailable(String... resourcePaths) {
        for (String resourcePath : resourcePaths) {
            try {
                ClassPathResource resource = new ClassPathResource(resourcePath);
                if (!resource.exists()) {
                    continue;
                }

                try (InputStream input = resource.getInputStream()) {
                    BufferedImage image = ImageIO.read(input);
                    if (image != null) {
                        return image;
                    }
                }
            } catch (Exception exception) {
                log.debug("Não foi possível carregar o recurso de marca {}.", resourcePath, exception);
            }
        }

        throw new IllegalStateException("Nenhuma arte institucional válida foi encontrada no classpath.");
    }

    public enum DocumentType {
        PAYMENT_GUIDE,
        FINANCIAL_STATEMENT,
        ACADEMIC_DOCUMENT
    }
}
