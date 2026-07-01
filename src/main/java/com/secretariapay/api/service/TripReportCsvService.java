package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.TripReportResponse;
import com.secretariapay.api.dto.report.TripTicketReportItemResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TripReportCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateTripReportCsv(
            TripReportResponse report,
            List<TripTicketReportItemResponse> tickets
    ) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');

        csv.append("Resumo da viagem\r\n");
        csv.append("Campo;Valor\r\n");

        appendFieldRow(csv, "Viagem ID", value(report.getTripId()));
        appendFieldRow(csv, "Empresa ID", value(report.getCompanyId()));
        appendFieldRow(csv, "Empresa", report.getCompanyName());
        appendFieldRow(csv, "Nome comercial", report.getCompanyTradeName());

        appendFieldRow(csv, "Origem", report.getOriginCity());
        appendFieldRow(csv, "UF origem", report.getOriginState());
        appendFieldRow(csv, "Terminal origem", report.getOriginTerminal());

        appendFieldRow(csv, "Destino", report.getDestinationCity());
        appendFieldRow(csv, "UF destino", report.getDestinationState());
        appendFieldRow(csv, "Terminal destino", report.getDestinationTerminal());

        appendFieldRow(csv, "Partida", formatDate(report.getDepartureAt()));
        appendFieldRow(csv, "Chegada", formatDate(report.getArrivalAt()));

        appendFieldRow(csv, "Total de assentos", value(report.getTotalSeats()));
        appendFieldRow(csv, "Assentos disponíveis", value(report.getAvailableSeats()));
        appendFieldRow(csv, "Assentos ocupados", value(report.getOccupiedSeats()));
        appendFieldRow(csv, "Taxa de ocupação (%)", value(report.getOccupancyRatePercentage()));

        appendFieldRow(csv, "Moeda", report.getCurrency());

        appendFieldRow(csv, "Total de reservas", value(report.getTotalBookings()));
        appendFieldRow(csv, "Reservas pagas", value(report.getPaidBookings()));
        appendFieldRow(csv, "Reservas pendentes", value(report.getPendingBookings()));
        appendFieldRow(csv, "Reservas canceladas", value(report.getCancelledBookings()));
        appendFieldRow(csv, "Reservas expiradas", value(report.getExpiredBookings()));

        appendFieldRow(csv, "Bilhetes emitidos", value(report.getIssuedTickets()));
        appendFieldRow(csv, "Bilhetes válidos", value(report.getValidTickets()));
        appendFieldRow(csv, "Bilhetes usados", value(report.getUsedTickets()));
        appendFieldRow(csv, "Bilhetes cancelados", value(report.getCancelledTickets()));
        appendFieldRow(csv, "Check-in realizado (%)", value(report.getCheckInRatePercentage()));

        appendFieldRow(csv, "Receita total", value(report.getTotalRevenue()));
        appendFieldRow(csv, "Ticket médio", value(report.getAverageTicketAmount()));
        appendFieldRow(csv, "Gerado em", formatDate(report.getGeneratedAt()));

        csv.append("\r\n");
        csv.append("Bilhetes da viagem\r\n");

        csv.append("Bilhete ID;Código do bilhete;Status do bilhete;");
        csv.append("Reserva ID;Código da reserva;Status da reserva;");
        csv.append("Passageiro;Documento;WhatsApp;Assento;Valor;Moeda;");
        csv.append("Emitido em;Usado em;Cancelado em\r\n");

        for (TripTicketReportItemResponse ticket : tickets) {
            appendRow(
                    csv,
                    value(ticket.getTicketId()),
                    ticket.getTicketCode(),
                    value(ticket.getTicketStatus()),
                    value(ticket.getBookingId()),
                    ticket.getBookingCode(),
                    value(ticket.getBookingStatus()),
                    ticket.getPassengerName(),
                    ticket.getPassengerDocument(),
                    ticket.getPassengerWhatsapp(),
                    value(ticket.getSeatNumber()),
                    value(ticket.getAmount()),
                    ticket.getCurrency(),
                    formatDate(ticket.getIssuedAt()),
                    formatDate(ticket.getUsedAt()),
                    formatDate(ticket.getCancelledAt())
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendFieldRow(StringBuilder csv, String field, String value) {
        appendRow(csv, field, value);
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(";");
            }

            csv.append(escape(values[index]));
        }

        csv.append("\r\n");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(";")
                || escaped.contains("\"")
                || escaped.contains("\n")
                || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(DATE_TIME_FORMATTER);
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }
}
