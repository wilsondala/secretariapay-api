
package com.vairapido.api.service;

import com.vairapido.api.dto.report.FinancialReportResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FinancialReportCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateCsv(FinancialReportResponse report) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');
        csv.append("Campo;Valor\r\n");

        appendRow(csv, "Escopo", report.getScope());
        appendRow(csv, "Empresa ID", report.getCompanyId() != null ? report.getCompanyId().toString() : "");
        appendRow(csv, "Moeda", report.getCurrency());
        appendRow(csv, "Início do período", formatDate(report.getPeriodStartAt()));
        appendRow(csv, "Fim do período", formatDate(report.getPeriodEndAt()));

        appendRow(csv, "Reservas pagas", String.valueOf(report.getPaidBookings()));
        appendRow(csv, "Reservas pendentes", String.valueOf(report.getPendingBookings()));
        appendRow(csv, "Reservas canceladas", String.valueOf(report.getCancelledBookings()));
        appendRow(csv, "Bilhetes emitidos", String.valueOf(report.getIssuedTickets()));

        appendRow(csv, "Total vendido", value(report.getTotalRevenue()));
        appendRow(csv, "Ticket médio", value(report.getAverageTicketAmount()));
        appendRow(csv, "Gerado em", formatDate(report.getGeneratedAt()));

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder csv, String field, String value) {
        csv.append(escape(field))
                .append(";")
                .append(escape(value))
                .append("\r\n");
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