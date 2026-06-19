
package com.vairapido.api.service;

import com.vairapido.api.dto.report.OperationalReportResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class OperationalReportCsvService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateCsv(OperationalReportResponse report) {
        StringBuilder csv = new StringBuilder();

        // BOM para o Excel reconhecer UTF-8 corretamente.
        csv.append('\uFEFF');

        csv.append("Campo;Valor\r\n");

        appendRow(csv, "Escopo", report.getScope());
        appendRow(csv, "Empresa ID", report.getCompanyId() != null ? report.getCompanyId().toString() : "");
        appendRow(csv, "Início do período", formatDate(report.getPeriodStartAt()));
        appendRow(csv, "Fim do período", formatDate(report.getPeriodEndAt()));

        appendRow(csv, "Total de bilhetes", String.valueOf(report.getTotalTickets()));
        appendRow(csv, "Bilhetes válidos", String.valueOf(report.getValidTickets()));
        appendRow(csv, "Bilhetes usados", String.valueOf(report.getUsedTickets()));
        appendRow(csv, "Bilhetes cancelados", String.valueOf(report.getCancelledTickets()));

        appendRow(csv, "Validações públicas", String.valueOf(report.getPublicValidations()));
        appendRow(csv, "Validações públicas com sucesso", String.valueOf(report.getSuccessfulPublicValidations()));
        appendRow(csv, "Validações públicas falhadas", String.valueOf(report.getFailedPublicValidations()));

        appendRow(csv, "Tentativas de embarque", String.valueOf(report.getBoardingAttempts()));
        appendRow(csv, "Embarques com sucesso", String.valueOf(report.getSuccessfulBoardings()));
        appendRow(csv, "Embarques falhados", String.valueOf(report.getFailedBoardings()));

        appendRow(csv, "Tentativas suspeitas", String.valueOf(report.getSuspiciousAttempts()));
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

    private String formatDate(java.time.LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(DATE_TIME_FORMATTER);
    }
}