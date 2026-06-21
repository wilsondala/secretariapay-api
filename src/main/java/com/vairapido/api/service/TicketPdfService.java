package com.vairapido.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.PassengerDocumentType;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TicketRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TicketRepository ticketRepository;
    private final DocumentValidatorService documentValidatorService;

    public TicketPdfService(
            TicketRepository ticketRepository,
            DocumentValidatorService documentValidatorService
    ) {
        this.ticketRepository = ticketRepository;
        this.documentValidatorService = documentValidatorService;
    }

    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            Booking booking = ticket.getBooking();
            Trip trip = booking.getTrip();
            Passenger passenger = booking.getPassenger();
            TransportCompany company = trip.getTransportCompany();
            TravelRoute route = trip.getRoute();

            PDImageXObject qrImage = createQrImage(document, ticket.getValidationUrl());

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(content);
                drawTicketBox(content);
                drawTitle(content);
                drawTicketInfo(content, ticket, booking, trip, passenger, company, route);
                content.drawImage(qrImage, 395, 555, 130, 130);
                drawFooter(content, ticket);
            }

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("Erro ao gerar PDF do bilhete.", exception);
        }
    }

    private PDImageXObject createQrImage(PDDocument document, String validationUrl)
            throws WriterException, IOException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
                validationUrl,
                BarcodeFormat.QR_CODE,
                300,
                300
        );

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return LosslessFactory.createFromImage(document, bufferedImage);
    }

    private void drawHeader(PDPageContentStream content) throws IOException {
        content.setNonStrokingColor(13, 27, 42);
        content.addRect(0, 760, 595, 82);
        content.fill();

        content.setNonStrokingColor(255, 193, 7);
        content.addRect(0, 752, 595, 8);
        content.fill();

        content.beginText();
        content.setNonStrokingColor(255, 255, 255);
        content.setFont(PDType1Font.HELVETICA_BOLD, 26);
        content.newLineAtOffset(50, 805);
        content.showText("VaiRápido");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(255, 255, 255);
        content.setFont(PDType1Font.HELVETICA, 11);
        content.newLineAtOffset(50, 785);
        content.showText("Bilhete digital de passagem");
        content.endText();
    }

    private void drawTicketBox(PDPageContentStream content) throws IOException {
        content.setNonStrokingColor(248, 250, 252);
        content.addRect(40, 105, 515, 610);
        content.fill();

        content.setStrokingColor(220, 220, 220);
        content.addRect(40, 105, 515, 610);
        content.stroke();
    }

    private void drawTitle(PDPageContentStream content) throws IOException {
        content.beginText();
        content.setNonStrokingColor(13, 27, 42);
        content.setFont(PDType1Font.HELVETICA_BOLD, 18);
        content.newLineAtOffset(60, 685);
        content.showText("Bilhete confirmado");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(34, 197, 94);
        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        content.newLineAtOffset(60, 665);
        content.showText("Apresente este bilhete no momento do embarque.");
        content.endText();
    }

    private void drawTicketInfo(
            PDPageContentStream content,
            Ticket ticket,
            Booking booking,
            Trip trip,
            Passenger passenger,
            TransportCompany company,
            TravelRoute route
    ) throws IOException {

        float y = 625;

        PassengerDocumentType documentType = getPassengerDocumentType(passenger);
        String documentLabel = documentValidatorService.label(documentType);
        String documentNumber = documentValidatorService.normalize(
                documentType,
                passenger.getDocumentNumber()
        );

        drawLabelValue(content, "Código do bilhete", ticket.getTicketCode(), 60, y);
        y -= 34;

        drawLabelValue(content, "Código da reserva", booking.getBookingCode(), 60, y);
        y -= 34;

        drawLabelValue(content, "Passageiro", passenger.getFullName(), 60, y);
        y -= 34;

        drawLabelValue(content, documentLabel, documentNumber, 60, y);
        y -= 34;

        drawLabelValue(content, "WhatsApp", passenger.getWhatsapp(), 60, y);
        y -= 34;

        drawLabelValue(content, "Empresa", getCompanyName(company), 60, y);
        y -= 34;

        drawLabelValue(content, "Origem", formatLocation(route.getOriginCity(), route.getOriginState(), route.getOriginTerminal()), 60, y);
        y -= 34;

        drawLabelValue(content, "Destino", formatLocation(route.getDestinationCity(), route.getDestinationState(), route.getDestinationTerminal()), 60, y);
        y -= 34;

        drawLabelValue(content, "Data e hora de saída", trip.getDepartureAt().format(DATE_TIME_FORMATTER), 60, y);
        y -= 34;

        drawLabelValue(content, "Previsão de chegada", trip.getArrivalAt().format(DATE_TIME_FORMATTER), 60, y);
        y -= 34;

        drawLabelValue(content, "Poltrona", String.valueOf(booking.getSeatNumber()), 60, y);
        y -= 34;

        drawLabelValue(content, "Valor", formatMoney(booking), 60, y);
        y -= 34;

        drawLabelValue(content, "Status do bilhete", ticket.getStatus().name(), 60, y);

        drawBoardingWarning(content);
    }

    private void drawBoardingWarning(PDPageContentStream content) throws IOException {
        float boxX = 60;
        float boxY = 115;
        float boxWidth = 475;
        float boxHeight = 78;

        content.setNonStrokingColor(255, 251, 235);
        content.addRect(boxX, boxY, boxWidth, boxHeight);
        content.fill();

        content.setStrokingColor(245, 158, 11);
        content.addRect(boxX, boxY, boxWidth, boxHeight);
        content.stroke();

        content.beginText();
        content.setNonStrokingColor(146, 64, 14);
        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        content.newLineAtOffset(boxX + 12, boxY + 56);
        content.showText("Atenção no embarque");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(120, 53, 15);
        content.setFont(PDType1Font.HELVETICA, 8);
        content.newLineAtOffset(boxX + 12, boxY + 39);
        content.showText("Apresente documento oficial com o mesmo nome e número informado neste bilhete.");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(120, 53, 15);
        content.setFont(PDType1Font.HELVETICA, 8);
        content.newLineAtOffset(boxX + 12, boxY + 24);
        content.showText("Divergência de nome ou documento pode impedir o embarque.");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(120, 53, 15);
        content.setFont(PDType1Font.HELVETICA, 8);
        content.newLineAtOffset(boxX + 12, boxY + 9);
        content.showText("Dados declarados pelo comprador/passageiro no momento da emissão.");
        content.endText();
    }

    private void drawLabelValue(
            PDPageContentStream content,
            String label,
            String value,
            float x,
            float y
    ) throws IOException {

        content.beginText();
        content.setNonStrokingColor(100, 116, 139);
        content.setFont(PDType1Font.HELVETICA_BOLD, 9);
        content.newLineAtOffset(x, y);
        content.showText(safeText(label).toUpperCase());
        content.endText();

        content.beginText();
        content.setNonStrokingColor(15, 23, 42);
        content.setFont(PDType1Font.HELVETICA, 12);
        content.newLineAtOffset(x, y - 15);
        content.showText(limitText(safeText(value), 72));
        content.endText();
    }

    private void drawFooter(PDPageContentStream content, Ticket ticket) throws IOException {
        content.beginText();
        content.setNonStrokingColor(13, 27, 42);
        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        content.newLineAtOffset(395, 535);
        content.showText("QR Code de validação");
        content.endText();

        content.beginText();
        content.setNonStrokingColor(71, 85, 105);
        content.setFont(PDType1Font.HELVETICA, 8);
        content.newLineAtOffset(60, 75);
        content.showText("Validação: " + limitText(ticket.getValidationUrl(), 95));
        content.endText();

        content.beginText();
        content.setNonStrokingColor(100, 116, 139);
        content.setFont(PDType1Font.HELVETICA, 8);
        content.newLineAtOffset(60, 55);
        content.showText("VaiRápido - sua passagem em poucos minutos.");
        content.endText();
    }

    private PassengerDocumentType getPassengerDocumentType(Passenger passenger) {
        if (passenger == null || passenger.getDocumentType() == null) {
            return documentValidatorService.defaultDocumentType();
        }

        return passenger.getDocumentType();
    }

    private String getCompanyName(TransportCompany company) {
        if (company.getTradeName() != null && !company.getTradeName().isBlank()) {
            return company.getTradeName();
        }

        return company.getName();
    }

    private String formatLocation(String city, String state, String terminal) {
        StringBuilder builder = new StringBuilder();

        builder.append(safeText(city));

        if (state != null && !state.isBlank()) {
            builder.append(" - ").append(state);
        }

        if (terminal != null && !terminal.isBlank()) {
            builder.append(" | ").append(terminal);
        }

        return builder.toString();
    }

    private String formatMoney(Booking booking) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);

        return booking.getCurrency() + " " + numberFormat.format(booking.getAmount());
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return "-";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }
}