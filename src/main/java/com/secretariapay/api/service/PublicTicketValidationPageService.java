package com.secretariapay.api.service;

import com.secretariapay.api.dto.publicticket.TicketValidationResponse;
import com.secretariapay.api.entity.enums.TicketStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PublicTicketValidationPageService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PublicTicketValidationService publicTicketValidationService;

    public PublicTicketValidationPageService(
            PublicTicketValidationService publicTicketValidationService
    ) {
        this.publicTicketValidationService = publicTicketValidationService;
    }

    public String renderValidationPage(
            String ticketCode,
            String ipAddress,
            String userAgent
    ) {
        TicketValidationResponse response =
                publicTicketValidationService.validateByCode(
                        ticketCode,
                        ipAddress,
                        userAgent
                );

        return render(response);
    }

    private String render(TicketValidationResponse response) {
        boolean valid = Boolean.TRUE.equals(response.getValid());

        String statusClass = resolveStatusClass(response, valid);
        String statusTitle = resolveStatusTitle(response, valid);
        String statusSubtitle = valueOrDash(response.getMessage());

        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <title>Validação do Bilhete - VaiRápido</title>
                    <style>
                        :root {
                            --navy: #071B33;
                            --yellow: #FFC107;
                            --green: #16A34A;
                            --red: #DC2626;
                            --orange: #F59E0B;
                            --bg: #F4F7FB;
                            --text: #172033;
                            --muted: #64748B;
                            --white: #FFFFFF;
                            --border: #E2E8F0;
                        }

                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            font-family: Arial, Helvetica, sans-serif;
                            background: var(--bg);
                            color: var(--text);
                        }

                        .header {
                            background: var(--navy);
                            color: var(--white);
                            padding: 28px 20px 22px;
                            border-bottom: 8px solid var(--yellow);
                        }

                        .header-inner {
                            max-width: 920px;
                            margin: 0 auto;
                        }

                        .brand {
                            font-size: 36px;
                            line-height: 1;
                            font-weight: 800;
                            letter-spacing: -1px;
                        }

                        .tagline {
                            margin-top: 8px;
                            color: #DCE7F5;
                            font-size: 16px;
                        }

                        .container {
                            max-width: 920px;
                            margin: 28px auto;
                            padding: 0 16px 32px;
                        }

                        .card {
                            background: var(--white);
                            border: 1px solid var(--border);
                            border-radius: 18px;
                            box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
                            overflow: hidden;
                        }

                        .status {
                            padding: 28px;
                            border-bottom: 1px solid var(--border);
                        }

                        .status-badge {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            padding: 8px 12px;
                            border-radius: 999px;
                            font-size: 13px;
                            font-weight: 800;
                            letter-spacing: .4px;
                            text-transform: uppercase;
                        }

                        .status-badge.valid {
                            background: #DCFCE7;
                            color: #166534;
                        }

                        .status-badge.used {
                            background: #FEF3C7;
                            color: #92400E;
                        }

                        .status-badge.cancelled,
                        .status-badge.invalid {
                            background: #FEE2E2;
                            color: #991B1B;
                        }

                        .status-title {
                            margin: 18px 0 8px;
                            font-size: 30px;
                            line-height: 1.15;
                            font-weight: 800;
                        }

                        .status-subtitle {
                            color: var(--muted);
                            font-size: 17px;
                            line-height: 1.5;
                        }

                        .grid {
                            display: grid;
                            grid-template-columns: 1fr 1fr;
                            gap: 0;
                        }

                        .section {
                            padding: 24px 28px;
                            border-bottom: 1px solid var(--border);
                        }

                        .section:nth-child(odd) {
                            border-right: 1px solid var(--border);
                        }

                        .section-title {
                            font-size: 14px;
                            font-weight: 800;
                            color: var(--navy);
                            text-transform: uppercase;
                            letter-spacing: .5px;
                            margin-bottom: 16px;
                        }

                        .row {
                            margin-bottom: 14px;
                        }

                        .label {
                            font-size: 12px;
                            color: var(--muted);
                            font-weight: 700;
                            text-transform: uppercase;
                            letter-spacing: .4px;
                            margin-bottom: 4px;
                        }

                        .value {
                            font-size: 16px;
                            font-weight: 600;
                            color: var(--text);
                            word-break: break-word;
                        }

                        .footer {
                            padding: 20px 28px 26px;
                            color: var(--muted);
                            font-size: 13px;
                            line-height: 1.5;
                            background: #F8FAFC;
                        }

                        .actions {
                            margin-top: 22px;
                            display: flex;
                            gap: 12px;
                            flex-wrap: wrap;
                        }

                        .button {
                            display: inline-block;
                            text-decoration: none;
                            background: var(--navy);
                            color: var(--white);
                            border-radius: 12px;
                            padding: 12px 16px;
                            font-weight: 700;
                            font-size: 14px;
                        }

                        .button.secondary {
                            background: var(--yellow);
                            color: var(--navy);
                        }

                        @media (max-width: 720px) {
                            .brand {
                                font-size: 30px;
                            }

                            .grid {
                                grid-template-columns: 1fr;
                            }

                            .section:nth-child(odd) {
                                border-right: none;
                            }

                            .status-title {
                                font-size: 25px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <header class="header">
                        <div class="header-inner">
                            <div class="brand">VaiRápido</div>
                            <div class="tagline">Validação pública de bilhete digital</div>
                        </div>
                    </header>

                    <main class="container">
                        <section class="card">
                            <div class="status">
                                <div class="status-badge {{STATUS_CLASS}}">
                                    {{STATUS_ICON}} {{STATUS_TITLE}}
                                </div>

                                <h1 class="status-title">{{MAIN_TITLE}}</h1>
                                <div class="status-subtitle">{{STATUS_SUBTITLE}}</div>

                                <div class="actions">
                                    <a class="button" href="javascript:window.location.reload()">Atualizar validação</a>
                                    <a class="button secondary" href="{{JSON_URL}}">Ver resposta técnica</a>
                                </div>
                            </div>

                            <div class="grid">
                                <div class="section">
                                    <div class="section-title">Bilhete</div>

                                    <div class="row">
                                        <div class="label">Código do bilhete</div>
                                        <div class="value">{{TICKET_CODE}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Status do bilhete</div>
                                        <div class="value">{{TICKET_STATUS}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Código da reserva</div>
                                        <div class="value">{{BOOKING_CODE}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Status da reserva</div>
                                        <div class="value">{{BOOKING_STATUS}}</div>
                                    </div>
                                </div>

                                <div class="section">
                                    <div class="section-title">Passageiro</div>

                                    <div class="row">
                                        <div class="label">Nome</div>
                                        <div class="value">{{PASSENGER_NAME}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Documento</div>
                                        <div class="value">{{PASSENGER_DOCUMENT}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Poltrona</div>
                                        <div class="value">{{SEAT_NUMBER}}</div>
                                    </div>
                                </div>

                                <div class="section">
                                    <div class="section-title">Viagem</div>

                                    <div class="row">
                                        <div class="label">Empresa</div>
                                        <div class="value">{{COMPANY_NAME}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Origem</div>
                                        <div class="value">{{ORIGIN}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Destino</div>
                                        <div class="value">{{DESTINATION}}</div>
                                    </div>
                                </div>

                                <div class="section">
                                    <div class="section-title">Data e validação</div>

                                    <div class="row">
                                        <div class="label">Saída</div>
                                        <div class="value">{{DEPARTURE_AT}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Chegada</div>
                                        <div class="value">{{ARRIVAL_AT}}</div>
                                    </div>

                                    <div class="row">
                                        <div class="label">Validado em</div>
                                        <div class="value">{{VALIDATED_AT}}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="footer">
                                Esta página é pública e serve para conferência rápida do bilhete.
                                A validação final de embarque deve seguir as regras operacionais da empresa de transporte.
                            </div>
                        </section>
                    </main>
                </body>
                </html>
                """;

        html = html.replace("{{STATUS_CLASS}}", statusClass);
        html = html.replace("{{STATUS_ICON}}", valid ? "✅" : resolveStatusIcon(response));
        html = html.replace("{{STATUS_TITLE}}", escapeHtml(statusTitle));
        html = html.replace("{{MAIN_TITLE}}", escapeHtml(resolveMainTitle(response, valid)));
        html = html.replace("{{STATUS_SUBTITLE}}", escapeHtml(statusSubtitle));
        html = html.replace("{{JSON_URL}}", escapeHtml(buildJsonUrl(response)));

        html = html.replace("{{TICKET_CODE}}", escapeHtml(valueOrDash(response.getTicketCode())));
        html = html.replace("{{TICKET_STATUS}}", escapeHtml(valueOrDash(response.getTicketStatus())));
        html = html.replace("{{BOOKING_CODE}}", escapeHtml(valueOrDash(response.getBookingCode())));
        html = html.replace("{{BOOKING_STATUS}}", escapeHtml(valueOrDash(response.getBookingStatus())));

        html = html.replace("{{PASSENGER_NAME}}", escapeHtml(valueOrDash(response.getPassengerName())));
        html = html.replace("{{PASSENGER_DOCUMENT}}", escapeHtml(valueOrDash(response.getPassengerDocument())));
        html = html.replace("{{SEAT_NUMBER}}", escapeHtml(valueOrDash(response.getSeatNumber())));

        html = html.replace("{{COMPANY_NAME}}", escapeHtml(resolveCompanyName(response)));
        html = html.replace("{{ORIGIN}}", escapeHtml(resolveOrigin(response)));
        html = html.replace("{{DESTINATION}}", escapeHtml(resolveDestination(response)));

        html = html.replace("{{DEPARTURE_AT}}", escapeHtml(formatDate(response.getDepartureAt())));
        html = html.replace("{{ARRIVAL_AT}}", escapeHtml(formatDate(response.getArrivalAt())));
        html = html.replace("{{VALIDATED_AT}}", escapeHtml(formatDate(response.getValidatedAt())));

        return html;
    }

    private String resolveStatusClass(
            TicketValidationResponse response,
            boolean valid
    ) {
        if (valid) {
            return "valid";
        }

        if (response.getTicketStatus() == TicketStatus.USED) {
            return "used";
        }

        if (response.getTicketStatus() == TicketStatus.CANCELLED) {
            return "cancelled";
        }

        return "invalid";
    }

    private String resolveStatusTitle(
            TicketValidationResponse response,
            boolean valid
    ) {
        if (valid) {
            return "Bilhete válido";
        }

        if (response.getTicketStatus() == TicketStatus.USED) {
            return "Bilhete utilizado";
        }

        if (response.getTicketStatus() == TicketStatus.CANCELLED) {
            return "Bilhete cancelado";
        }

        return "Bilhete inválido";
    }

    private String resolveStatusIcon(TicketValidationResponse response) {
        if (response.getTicketStatus() == TicketStatus.USED) {
            return "⚠️";
        }

        if (response.getTicketStatus() == TicketStatus.CANCELLED) {
            return "❌";
        }

        return "❌";
    }

    private String resolveMainTitle(
            TicketValidationResponse response,
            boolean valid
    ) {
        if (valid) {
            return "Bilhete confirmado para embarque";
        }

        if (response.getTicketStatus() == TicketStatus.USED) {
            return "Este bilhete já foi utilizado";
        }

        if (response.getTicketStatus() == TicketStatus.CANCELLED) {
            return "Este bilhete foi cancelado";
        }

        return "Não foi possível validar este bilhete";
    }

    private String buildJsonUrl(TicketValidationResponse response) {
        String ticketCode = valueOrDash(response.getTicketCode());

        if ("-".equals(ticketCode)) {
            return "#";
        }

        return "/api/v1/public/tickets/validate/" + ticketCode;
    }

    private String resolveCompanyName(TicketValidationResponse response) {
        if (response.getCompanyTradeName() != null
                && !response.getCompanyTradeName().isBlank()) {
            return response.getCompanyTradeName();
        }

        return valueOrDash(response.getCompanyName());
    }

    private String resolveOrigin(TicketValidationResponse response) {
        return joinLocation(
                response.getOriginCity(),
                response.getOriginState(),
                response.getOriginTerminal()
        );
    }

    private String resolveDestination(TicketValidationResponse response) {
        return joinLocation(
                response.getDestinationCity(),
                response.getDestinationState(),
                response.getDestinationTerminal()
        );
    }

    private String joinLocation(
            String city,
            String state,
            String terminal
    ) {
        String cityState = valueOrEmpty(city);

        if (state != null && !state.isBlank()) {
            cityState = cityState.isBlank()
                    ? state
                    : cityState + " - " + state;
        }

        if (terminal != null && !terminal.isBlank()) {
            cityState = cityState.isBlank()
                    ? terminal
                    : cityState + " / " + terminal;
        }

        return cityState.isBlank() ? "-" : cityState;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "-";
        }

        return value.format(DATE_TIME_FORMATTER);
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value).trim();

        return text.isBlank() ? "-" : text;
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
