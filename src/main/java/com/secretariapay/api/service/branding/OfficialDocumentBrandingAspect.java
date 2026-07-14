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
import org.springframework.stereotype.Component;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Aplica a identidade visual aprovada pelo cliente aos PDFs oficiais sem
 * alterar dados, cálculos, QR Codes, hashes, permissões ou rotas existentes.
 *
 * A sobreposição é executada depois que cada serviço termina de gerar o PDF.
 * Se a aplicação da marca falhar, o documento original continua sendo entregue.
 */
@Aspect
@Component
public class OfficialDocumentBrandingAspect {

    private static final Logger log = LoggerFactory.getLogger(OfficialDocumentBrandingAspect.class);

    private static final Color NAVY = new Color(5, 31, 72);
    private static final Color NAVY_SOFT = new Color(18, 52, 91);
    private static final Color GOLD = new Color(218, 165, 32);
    private static final Color GOLD_LIGHT = new Color(247, 224, 151);
    private static final Color GOLD_DARK = new Color(126, 83, 12);
    private static final Color SHADOW = new Color(15, 23, 42, 55);

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
            log.warn("Não foi possível aplicar a nova identidade visual ao documento {}. O PDF original será mantido.",
                    type, exception);
            return generated;
        }
    }

    /**
     * Visível para testes unitários e para futuras rotinas institucionais de reemissão.
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

        // Limpa somente as áreas das marcas antigas. Textos institucionais permanecem intactos.
        fillWhite(content, 39, headerBottom + 5, 82, 64);
        fillWhite(content, pageWidth - 202, headerBottom + 5, 170, 64);

        PDImageXObject imetro = LosslessFactory.createFromImage(document, renderImetroLogo(720, 430));
        PDImageXObject secretariaPay = LosslessFactory.createFromImage(document, renderSecretariaPayMetroLogo(1080, 300));

        float imetroWidth = academicDocument ? 68 : 64;
        float imetroHeight = academicDocument ? 48 : 45;
        content.drawImage(imetro, 46, headerBottom + 14, imetroWidth, imetroHeight);

        float brandWidth = academicDocument ? 145 : 151;
        float brandHeight = academicDocument ? 40 : 42;
        content.drawImage(secretariaPay, pageWidth - brandWidth - 39, headerBottom + 17, brandWidth, brandHeight);
    }

    private void overlayLandscapeHeader(
            PDDocument document,
            PDPageContentStream content,
            PDPage page
    ) throws Exception {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        fillWhite(content, 20, pageHeight - 100, 148, 82);
        fillWhite(content, pageWidth - 218, pageHeight - 98, 200, 78);

        PDImageXObject imetro = LosslessFactory.createFromImage(document, renderImetroLogo(720, 430));
        PDImageXObject secretariaPay = LosslessFactory.createFromImage(document, renderSecretariaPayMetroLogo(1080, 300));

        content.drawImage(imetro, 28, pageHeight - 92, 126, 70);
        content.drawImage(secretariaPay, pageWidth - 207, pageHeight - 82, 182, 51);
    }

    private void fillWhite(PDPageContentStream content, float x, float y, float width, float height) throws Exception {
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(x, y, width, height);
        content.fill();
    }

    /**
     * Versão vetorial institucional inspirada na marca UM com louros aprovada pelo cliente.
     * O desenho é renderizado em memória para manter o JAR autocontido e previsível.
     */
    BufferedImage renderImetroLogo(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);

        float scaleX = width / 720f;
        float scaleY = height / 430f;
        graphics.scale(scaleX, scaleY);

        drawLaurel(graphics, 360, 220, false);
        drawLaurel(graphics, 360, 220, true);

        Font monogramFont = new Font(Font.SERIF, Font.BOLD, 198);
        drawOutlinedText(graphics, "M", monogramFont, 360, 260, GOLD, GOLD_DARK, 5.5f, true);
        Font uFont = new Font(Font.SERIF, Font.BOLD, 205);
        drawOutlinedText(graphics, "U", uFont, 360, 245, GOLD_LIGHT, GOLD_DARK, 5f, true);

        graphics.dispose();
        return image;
    }

    private void drawLaurel(Graphics2D graphics, float centerX, float centerY, boolean mirror) {
        AffineTransform previous = graphics.getTransform();
        if (mirror) {
            graphics.translate(centerX * 2, 0);
            graphics.scale(-1, 1);
        }

        Path2D branch = new Path2D.Float();
        branch.moveTo(342, 370);
        branch.curveTo(170, 350, 78, 270, 72, 126);
        graphics.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setPaint(new GradientPaint(70, 120, GOLD_LIGHT, 340, 370, GOLD_DARK));
        graphics.draw(branch);

        for (int index = 0; index < 11; index++) {
            double t = index / 10.0;
            double x = bezier(342, 170, 78, 72, t);
            double y = bezier(370, 350, 270, 126, t);
            double angle = -0.18 - t * 1.12;
            double leafWidth = 44 - t * 10;
            double leafHeight = 17 - t * 3;
            drawLeaf(graphics, x, y, leafWidth, leafHeight, angle - 0.52);
            if (index > 0 && index < 10) {
                drawLeaf(graphics, x + 7, y - 3, leafWidth * .92, leafHeight * .92, angle + 0.62);
            }
        }

        graphics.setTransform(previous);
    }

    private double bezier(double p0, double p1, double p2, double p3, double t) {
        double inverse = 1 - t;
        return inverse * inverse * inverse * p0
                + 3 * inverse * inverse * t * p1
                + 3 * inverse * t * t * p2
                + t * t * t * p3;
    }

    private void drawLeaf(Graphics2D graphics, double centerX, double centerY, double width, double height, double angle) {
        AffineTransform previous = graphics.getTransform();
        graphics.translate(centerX, centerY);
        graphics.rotate(angle);
        Shape leaf = new Ellipse2D.Double(-width / 2, -height / 2, width, height);
        graphics.setPaint(new GradientPaint((float) -width / 2, 0, GOLD_DARK, (float) width / 2, 0, GOLD_LIGHT));
        graphics.fill(leaf);
        graphics.setColor(GOLD_DARK);
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(leaf);
        graphics.setTransform(previous);
    }

    /**
     * Versão horizontal da marca SecretariaPay METRO aprovada pelo cliente.
     */
    BufferedImage renderSecretariaPayMetroLogo(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);

        float scaleX = width / 1080f;
        float scaleY = height / 300f;
        graphics.scale(scaleX, scaleY);

        drawAcademicFinanceSymbol(graphics, 28, 26, 220, 230);

        Font wordmarkFont = new Font(Font.SERIF, Font.BOLD, 104);
        drawOutlinedText(graphics, "SecretariaPay", wordmarkFont, 650, 140, NAVY, GOLD_DARK, 2.8f, false);

        Font metroFont = new Font(Font.SANS_SERIF, Font.BOLD, 49);
        drawLetterSpacedText(graphics, "METRO", metroFont, 650, 222, 14, GOLD, GOLD_DARK);

        graphics.dispose();
        return image;
    }

    private void drawAcademicFinanceSymbol(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setComposite(AlphaComposite.SrcOver.derive(.22f));
        graphics.setColor(SHADOW);
        graphics.fillRoundRect(x + 13, y + 16, width - 12, height - 10, 24, 24);
        graphics.setComposite(AlphaComposite.SrcOver);

        GradientPaint goldPaint = new GradientPaint(x, y, GOLD_LIGHT, x + width, y + height, GOLD_DARK);
        graphics.setPaint(goldPaint);

        Polygon cap = new Polygon();
        cap.addPoint(x + 10, y + 48);
        cap.addPoint(x + width / 2, y + 4);
        cap.addPoint(x + width - 7, y + 48);
        cap.addPoint(x + width / 2, y + 90);
        graphics.fill(cap);
        graphics.setColor(GOLD_DARK);
        graphics.setStroke(new BasicStroke(3f));
        graphics.draw(cap);

        Path2D tassel = new Path2D.Float();
        tassel.moveTo(x + 34, y + 49);
        tassel.lineTo(x + 34, y + 118);
        tassel.lineTo(x + 23, y + 153);
        tassel.lineTo(x + 45, y + 153);
        tassel.closePath();
        graphics.setPaint(goldPaint);
        graphics.fill(tassel);
        graphics.setColor(GOLD_DARK);
        graphics.draw(tassel);

        RoundRectangle2D document = new RoundRectangle2D.Float(x + 68, y + 96, 106, 112, 16, 16);
        graphics.setPaint(goldPaint);
        graphics.fill(document);
        graphics.setColor(GOLD_DARK);
        graphics.draw(document);

        graphics.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x + 89, y + 129, x + 139, y + 129);
        graphics.drawLine(x + 89, y + 153, x + 139, y + 153);
        graphics.drawLine(x + 89, y + 177, x + 124, y + 177);

        Ellipse2D validation = new Ellipse2D.Float(x + 135, y + 143, 76, 76);
        graphics.setColor(new Color(255, 255, 255, 230));
        graphics.fill(validation);
        graphics.setColor(GOLD_DARK);
        graphics.setStroke(new BasicStroke(5f));
        graphics.draw(validation);
        Path2D check = new Path2D.Float();
        check.moveTo(x + 150, y + 180);
        check.lineTo(x + 168, y + 198);
        check.lineTo(x + 202, y + 160);
        graphics.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(check);
    }

    private void drawOutlinedText(
            Graphics2D graphics,
            String text,
            Font font,
            float centerX,
            float baselineY,
            Paint fill,
            Color outline,
            float outlineWidth,
            boolean shadow
    ) {
        GlyphVector glyphs = font.createGlyphVector(graphics.getFontRenderContext(), text);
        Shape shape = glyphs.getOutline();
        java.awt.geom.Rectangle2D bounds = shape.getBounds2D();
        AffineTransform transform = AffineTransform.getTranslateInstance(
                centerX - bounds.getCenterX(),
                baselineY - bounds.getMaxY()
        );
        Shape positioned = transform.createTransformedShape(shape);

        if (shadow) {
            AffineTransform shadowTransform = AffineTransform.getTranslateInstance(8, 10);
            graphics.setColor(SHADOW);
            graphics.fill(shadowTransform.createTransformedShape(positioned));
        }

        graphics.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(outline);
        graphics.draw(positioned);
        graphics.setPaint(fill);
        graphics.fill(positioned);
    }

    private void drawLetterSpacedText(
            Graphics2D graphics,
            String text,
            Font font,
            float centerX,
            float baselineY,
            float spacing,
            Color fill,
            Color outline
    ) {
        FontMetrics metrics = graphics.getFontMetrics(font);
        float totalWidth = 0;
        for (int index = 0; index < text.length(); index++) {
            totalWidth += metrics.charWidth(text.charAt(index));
            if (index < text.length() - 1) totalWidth += spacing;
        }

        float x = centerX - totalWidth / 2;
        graphics.setFont(font);
        graphics.setStroke(new BasicStroke(1.3f));
        for (int index = 0; index < text.length(); index++) {
            String character = String.valueOf(text.charAt(index));
            GlyphVector glyph = font.createGlyphVector(graphics.getFontRenderContext(), character);
            Shape shape = glyph.getOutline(x, baselineY);
            graphics.setColor(outline);
            graphics.draw(shape);
            graphics.setColor(fill);
            graphics.fill(shape);
            x += metrics.charWidth(text.charAt(index)) + spacing;
        }
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    public enum DocumentType {
        PAYMENT_GUIDE,
        FINANCIAL_STATEMENT,
        ACADEMIC_DOCUMENT
    }
}
