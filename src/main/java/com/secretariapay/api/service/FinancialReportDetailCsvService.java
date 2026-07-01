package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.FinancialBookingReportItemResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FinancialReportDetailCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateBookingsCsv(List<FinancialBookingReportItemResponse> bookings) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');

        csv.append("Reserva ID;Código da reserva;Status da reserva;");
        csv.append("Bilhete ID;Código do bilhete;Status do bilhete;");
        csv.append("Empresa ID;Empresa;Nome comercial;");
        csv.append("Passageiro;Documento;WhatsApp;");
        csv.append("Origem;UF origem;Terminal origem;");
        csv.append("Destino;UF destino;Terminal destino;");
        csv.append("Partida;Chegada;Assento;");
        csv.append("Valor;Moeda;Expira em;Pago em;Cancelado em;Criado em;Atualizado em\r\n");

        for (FinancialBookingReportItemResponse booking : bookings) {
            appendRow(
                    csv,
                    value(booking.getBookingId()),
                    booking.getBookingCode(),
                    value(booking.getBookingStatus()),

                    value(booking.getTicketId()),
                    booking.getTicketCode(),
                    value(booking.getTicketStatus()),

                    value(booking.getCompanyId()),
                    booking.getCompanyName(),
                    booking.getCompanyTradeName(),

                    booking.getPassengerName(),
                    booking.getPassengerDocument(),
                    booking.getPassengerWhatsapp(),

                    booking.getOriginCity(),
                    booking.getOriginState(),
                    booking.getOriginTerminal(),

                    booking.getDestinationCity(),
                    booking.getDestinationState(),
                    booking.getDestinationTerminal(),

                    formatDate(booking.getDepartureAt()),
                    formatDate(booking.getArrivalAt()),
                    value(booking.getSeatNumber()),

                    value(booking.getAmount()),
                    booking.getCurrency(),
                    formatDate(booking.getExpiresAt()),
                    formatDate(booking.getPaidAt()),
                    formatDate(booking.getCancelledAt()),
                    formatDate(booking.getCreatedAt()),
                    formatDate(booking.getUpdatedAt())
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
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
