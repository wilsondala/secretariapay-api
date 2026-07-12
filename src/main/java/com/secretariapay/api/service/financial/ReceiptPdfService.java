package com.secretariapay.api.service.financial;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReceiptPdfService {
    private static final Color NAVY = new Color(4, 42, 104);
    private static final Color GREEN = new Color(22, 145, 58);
    private static final Color GOLD = new Color(216, 164, 37);
    private static final Color LIGHT = new Color(247, 249, 252);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ReceiptRepository receiptRepository;
    private final ReceiptAuthenticityService authenticityService;

    public ReceiptPdfService(ReceiptRepository receiptRepository, ReceiptAuthenticityService authenticityService) {
        this.receiptRepository = receiptRepository;
        this.authenticityService = authenticityService;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(UUID receiptId) {
        Receipt anchor = receiptRepository.findById(receiptId).orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));
        Student student = anchor.getCharge().getStudent();
        List<Receipt> receipts = receiptRepository.findByChargeStudentIdAndStatusOrderByChargePaidAtAsc(student.getId(), ReceiptStatus.VALID);
        if (receipts.isEmpty()) receipts = List.of(anchor);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            try (PDPageContentStream c = new PDPageContentStream(document, page)) { draw(document, page, c, anchor, receipts, student); }
            document.save(output);
            return output.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Não foi possível gerar o comprovativo oficial.", e); }
    }

    private void draw(PDDocument doc, PDPage page, PDPageContentStream c, Receipt anchor, List<Receipt> receipts, Student student) throws Exception {
        float w = page.getMediaBox().getWidth(), h = page.getMediaBox().getHeight(), m = 18;
        stroke(c, NAVY, m, m, w - 2*m, h - 2*m, .7f);
        image(doc, c, "static/assets/imetro.png", 28, h-91, 125, 66);
        image(doc, c, "static/branding/secretariapay-logo.png", w-205, h-87, 174, 58);
        center(c, "COMPROVATIVO DE PAGAMENTOS", 20, w/2, h-54, NAVY, true);
        line(c, GOLD, w/2-102, h-66, w/2+102, h-66, 1);
        center(c, "Emitido em: " + DATE_TIME.format(anchor.getIssuedAt()) + "  |  Comprovativo Nº " + anchor.getReceiptCode(), 9, w/2, h-83, NAVY, false);

        float infoY=h-108; fill(c,LIGHT,m+1,infoY-69,w-2*m-2,69); stroke(c,NAVY,m+1,infoY-69,w-2*m-2,69,.7f);
        AcademicClass ac=student.getAcademicClass(); Course course=ac==null?null:ac.getCourse();
        pair(c,"NOME",student.getFullName(),30,infoY-18); pair(c,"MATRÍCULA",student.getStudentNumber(),30,infoY-37); pair(c,"CURSO",course==null?"-":course.getName(),30,infoY-56);
        pair(c,"ANO ACADÉMICO",ac==null?"-":ac.getAcademicYear(),320,infoY-18); pair(c,"TURMA",ac==null?"-":ac.getName(),320,infoY-37); pair(c,"TELEFONE",mask(student.getPhone()),320,infoY-56);
        pair(c,"DOCUMENTO",mask(student.getDocumentNumber()),575,infoY-27); pair(c,"E-MAIL",student.getEmail(),575,infoY-50);

        float titleY=infoY-82; fill(c,NAVY,m+1,titleY-22,w-2*m-2,22); center(c,"DETALHAMENTO DOS PAGAMENTOS",11,w/2,titleY-15,Color.WHITE,true);
        float headerY=titleY-26; fill(c,NAVY,m+1,headerY-25,w-2*m-2,25);
        String[] heads={"Nº","DESCRIÇÃO DA COBRANÇA","REF. PERÍODO","REFERÊNCIA / GUIA","DATA DO PAGAMENTO","FORMA DE PAGAMENTO","VALOR BRUTO","DESCONTOS","JUROS / MULTA","VALOR LÍQUIDO"};
        float[] xs={25,52,178,246,340,438,548,616,676,742};
        for(int i=0;i<heads.length;i++) text(c,heads[i],6.4f,xs[i],headerY-16,Color.WHITE,true);
        float y=headerY-40; int n=1; BigDecimal gross=BigDecimal.ZERO,disc=BigDecimal.ZERO,fees=BigDecimal.ZERO,total=BigDecimal.ZERO;
        for(Receipt receipt:receipts){ Charge ch=receipt.getCharge();
            text(c,String.valueOf(n++),8,28,y,NAVY,false);
            fittedText(c, safe(ch.getDescription()), 8, 6.4f, 52, y, 120, NAVY, false);
            fittedText(c, safe(ch.getReferenceMonth()), 8, 6.2f, 178, y, 62, NAVY, false);
            fittedText(c, safe(ch.getChargeCode()), 7.4f, 5.8f, 246, y, 88, NAVY, false);
            fittedText(c, ch.getPaidAt()==null?"-":DATE_TIME.format(ch.getPaidAt()), 7.3f, 6.1f, 340, y, 91, NAVY, false);
            fittedText(c, "PAGAMENTO CONFIRMADO", 6.5f, 5.6f, 438, y, 104, GREEN, true);
            text(c,money(ch.getAmount()),8,552,y,NAVY,false);
            text(c,money(ch.getDiscountAmount()),8,620,y,NAVY,false);
            BigDecimal fee=nz(ch.getFineAmount()).add(nz(ch.getInterestAmount())); text(c,money(fee),8,688,y,NAVY,false); text(c,money(ch.getTotalAmount()),8,756,y,GREEN,true);
            line(c,new Color(210,218,232),m+2,y-12,w-m-2,y-12,.4f); y-=37; gross=gross.add(nz(ch.getAmount())); disc=disc.add(nz(ch.getDiscountAmount())); fees=fees.add(fee); total=total.add(nz(ch.getTotalAmount()));
        }
        line(c,NAVY,m+1,y+14,w-m-1,y+14,.8f); text(c,"Nº TOTAL DE TÍTULOS:  " + receipts.size(),8,28,y-2,NAVY,true);
        text(c,"SUBTOTAL:",8,314,y-2,NAVY,true); text(c,money(gross),8,464,y-2,NAVY,false); text(c,"ACRÉSCIMOS:",8,314,y-17,NAVY,true); text(c,money(fees),8,464,y-17,new Color(20,85,180),false);
        text(c,"DESCONTOS:",8,314,y-32,NAVY,true); text(c,money(disc),8,464,y-32,Color.RED,false); fill(c,NAVY,302,y-56,210,20); text(c,"TOTAL LÍQUIDO:",8,314,y-50,Color.WHITE,true); text(c,money(total)+" KZ",10,432,y-50,Color.WHITE,true);

        float boxY=43, boxH=105; stroke(c,NAVY,m+1,boxY,w-2*m-2,boxH,.7f);
        String hash=authenticityService.hash(anchor); String url=anchor.getValidationUrl(); if(url==null||!url.contains("hash=")) url="https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/"+anchor.getReceiptCode()+"/authentic?hash="+hash;
        BufferedImage qr=createQr(url); c.drawImage(LosslessFactory.createFromImage(doc,qr),31,54,82,82);
        text(c,"V",25,132,111,GREEN,true); text(c,"COMPROVATIVO VÁLIDO",9,165,117,GREEN,true); text(c,"Este documento comprova os pagamentos recebidos",8,165,101,NAVY,false); text(c,"e registados pela tesouraria do IMETRO.",8,165,87,NAVY,false);
        text(c,"Validação pública por QR Code",8,165,71,GREEN,true); text(c,"Código de verificação: "+authenticityService.shortHash(anchor)+"-IMETRO",7.5f,165,57,NAVY,false);
        line(c,new Color(170,180,195),414,53,414,136,.6f); center(c,"RESPONSÁVEL / TESOURARIA",8,535,118,NAVY,true); center(c,"Assinado digitalmente",11,535,91,new Color(39,54,205),false); line(c,NAVY,462,78,608,78,.5f); center(c,"Tesouraria IMETRO",8,535,62,NAVY,false);
        center(c,"SELO DIGITAL",8,700,112,new Color(130,165,225),true); stroke(c,new Color(130,165,225),654,56,92,72,1.2f); center(c,"IMETRO",16,700,84,new Color(130,165,225),true); center(c,"LIQUIDADO",7,700,68,GREEN,true);
        line(c,NAVY,m+1,35,w-m-1,35,.7f); center(c,"Documento emitido eletronicamente pelo SecretáriaPay Académico - IMETRO",8,w/2,24,NAVY,false); center(c,"Este documento não substitui o recibo individual do estudante.",7.5f,w/2,14,NAVY,false);
    }

    private BufferedImage createQr(String value)throws Exception{Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.H);hints.put(EncodeHintType.MARGIN,1);BitMatrix matrix=new QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,500,500,hints);return MatrixToImageWriter.toBufferedImage(matrix);}
    private void image(PDDocument d,PDPageContentStream c,String path,float x,float y,float w,float h){try{ClassPathResource r=new ClassPathResource(path);if(r.exists()){var i=org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(d,r.getInputStream().readAllBytes(),path);c.drawImage(i,x,y,w,h);}}catch(Exception ignored){}}
    private void pair(PDPageContentStream c,String l,Object v,float x,float y)throws Exception{text(c,l+":",8,x,y,NAVY,true);text(c,clip(safe(v),36),8,x+72,y,Color.BLACK,false);}
    private void fittedText(PDPageContentStream c,String value,float maxSize,float minSize,float x,float y,float maxWidth,Color color,boolean bold)throws Exception{String p=pdf(value);PDType1Font font=bold?PDType1Font.HELVETICA_BOLD:PDType1Font.HELVETICA;float size=maxSize;while(size>minSize&&font.getStringWidth(p)/1000*size>maxWidth)size-=.2f;if(font.getStringWidth(p)/1000*size>maxWidth){while(p.length()>4&&font.getStringWidth(p+"...")/1000*size>maxWidth)p=p.substring(0,p.length()-1);p=p+"...";}text(c,p,size,x,y,color,bold);}
    private void text(PDPageContentStream c,String s,float z,float x,float y,Color color,boolean bold)throws Exception{c.beginText();c.setNonStrokingColor(color);c.setFont(bold?PDType1Font.HELVETICA_BOLD:PDType1Font.HELVETICA,z);c.newLineAtOffset(x,y);c.showText(pdf(s));c.endText();}
    private void center(PDPageContentStream c,String s,float z,float x,float y,Color color,boolean bold)throws Exception{String p=pdf(s);PDType1Font f=bold?PDType1Font.HELVETICA_BOLD:PDType1Font.HELVETICA;text(c,p,z,x-(f.getStringWidth(p)/1000*z)/2,y,color,bold);}
    private void fill(PDPageContentStream c,Color color,float x,float y,float w,float h)throws Exception{c.setNonStrokingColor(color);c.addRect(x,y,w,h);c.fill();}
    private void stroke(PDPageContentStream c,Color color,float x,float y,float w,float h,float z)throws Exception{c.setStrokingColor(color);c.setLineWidth(z);c.addRect(x,y,w,h);c.stroke();}
    private void line(PDPageContentStream c,Color color,float x1,float y1,float x2,float y2,float z)throws Exception{c.setStrokingColor(color);c.setLineWidth(z);c.moveTo(x1,y1);c.lineTo(x2,y2);c.stroke();}
    private String money(BigDecimal v){DecimalFormatSymbols s=new DecimalFormatSymbols(new Locale("pt","AO"));s.setGroupingSeparator('.');s.setDecimalSeparator(',');return new DecimalFormat("#,##0.00",s).format(nz(v));}
    private BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;} private String safe(Object v){return v==null?"-":String.valueOf(v);} private String clip(String v,int n){return v==null?"-":v.length()>n?v.substring(0,n-3)+"...":v;}
    private String mask(String v){if(v==null||v.isBlank())return "-";if(v.length()<7)return "***";return v.substring(0,Math.min(4,v.length()))+" *** *** "+v.substring(v.length()-3);}
    private String pdf(String v){return safe(v).replace("–","-").replace("—","-").replace("“","\"").replace("”","\"").replace("’","'").replace("…","...").replace("✓","V");}
}
