package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.OperationalTicketReportItemResponse;
import com.secretariapay.api.dto.ticketaudit.TicketAuditLogResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OperationalReportDetailCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateTicketsCsv(List<OperationalTicketReportItemResponse> tickets) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');

        csv.append("Ticket ID;Código do bilhete;Status do bilhete;Reserva ID;Código da reserva;Status da reserva;");
        csv.append("Empresa ID;Empresa;Nome comercial;Passageiro;Documento;WhatsApp;");
        csv.append("Origem;UF origem;Terminal origem;Destino;UF destino;Terminal destino;");
        csv.append("Partida;Chegada;Assento;Emitido em;Usado em;Cancelado em;Criado em;Atualizado em\r\n");

        for (OperationalTicketReportItemResponse ticket : tickets) {
            appendRow(
                    csv,
                    value(ticket.getTicketId()),
                    ticket.getTicketCode(),
                    value(ticket.getTicketStatus()),
                    value(ticket.getBookingId()),
                    ticket.getBookingCode(),
                    value(ticket.getBookingStatus()),
                    value(ticket.getCompanyId()),
                    ticket.getCompanyName(),
                    ticket.getCompanyTradeName(),
                    ticket.getPassengerName(),
                    ticket.getPassengerDocument(),
                    ticket.getPassengerWhatsapp(),
                    ticket.getOriginCity(),
                    ticket.getOriginState(),
                    ticket.getOriginTerminal(),
                    ticket.getDestinationCity(),
                    ticket.getDestinationState(),
                    ticket.getDestinationTerminal(),
                    formatDate(ticket.getDepartureAt()),
                    formatDate(ticket.getArrivalAt()),
                    value(ticket.getSeatNumber()),
                    formatDate(ticket.getIssuedAt()),
                    formatDate(ticket.getUsedAt()),
                    formatDate(ticket.getCancelledAt()),
                    formatDate(ticket.getCreatedAt()),
                    formatDate(ticket.getUpdatedAt())
            );
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateAuditLogsCsv(List<TicketAuditLogResponse> logs) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');

        csv.append("Log ID;Código do bilhete;Ação;Sucesso;Mensagem;");
        csv.append("Status do bilhete;Status da reserva;IP;User-Agent;Criado em\r\n");

        for (TicketAuditLogResponse log : logs) {
            appendRow(
                    csv,
                    value(log.getId()),
                    log.getTicketCode(),
                    value(log.getAction()),
                    value(log.getSuccess()),
                    log.getMessage(),
                    value(log.getTicketStatus()),
                    value(log.getBookingStatus()),
                    log.getIpAddress(),
                    log.getUserAgent(),
                    formatDate(log.getCreatedAt())
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
