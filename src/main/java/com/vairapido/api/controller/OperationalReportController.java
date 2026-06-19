package com.vairapido.api.controller;

import com.vairapido.api.dto.report.OperationalReportResponse;
import com.vairapido.api.dto.report.OperationalTicketReportItemResponse;
import com.vairapido.api.dto.ticketaudit.TicketAuditLogResponse;
import com.vairapido.api.service.OperationalReportCsvService;
import com.vairapido.api.service.OperationalReportDetailCsvService;
import com.vairapido.api.service.OperationalReportDetailService;
import com.vairapido.api.service.OperationalReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/operational")
public class OperationalReportController {

    private static final DateTimeFormatter FILE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final OperationalReportService operationalReportService;
    private final OperationalReportCsvService operationalReportCsvService;
    private final OperationalReportDetailService operationalReportDetailService;
    private final OperationalReportDetailCsvService operationalReportDetailCsvService;

    public OperationalReportController(
            OperationalReportService operationalReportService,
            OperationalReportCsvService operationalReportCsvService,
            OperationalReportDetailService operationalReportDetailService,
            OperationalReportDetailCsvService operationalReportDetailCsvService
    ) {
        this.operationalReportService = operationalReportService;
        this.operationalReportCsvService = operationalReportCsvService;
        this.operationalReportDetailService = operationalReportDetailService;
        this.operationalReportDetailCsvService = operationalReportDetailCsvService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public OperationalReportResponse getGlobalReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportService.getGlobalReport(startAt, endAt);
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<OperationalTicketReportItemResponse> getGlobalTickets(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportDetailService.findGlobalTickets(startAt, endAt);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<TicketAuditLogResponse> getGlobalAuditLogs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportDetailService.findGlobalAuditLogs(startAt, endAt);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public OperationalReportResponse getCompanyReport(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportService.getCompanyReport(companyId, startAt, endAt);
    }

    @GetMapping("/company/{companyId}/tickets")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public List<OperationalTicketReportItemResponse> getCompanyTickets(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportDetailService.findCompanyTickets(companyId, startAt, endAt);
    }

    @GetMapping("/company/{companyId}/audit-logs")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public List<TicketAuditLogResponse> getCompanyAuditLogs(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return operationalReportDetailService.findCompanyAuditLogs(companyId, startAt, endAt);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportGlobalReportCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        OperationalReportResponse report =
                operationalReportService.getGlobalReport(startAt, endAt);

        byte[] csv = operationalReportCsvService.generateCsv(report);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-global-" + nowForFileName() + ".csv"
        );
    }

    @GetMapping("/company/{companyId}/export/csv")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public ResponseEntity<byte[]> exportCompanyReportCsv(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        OperationalReportResponse report =
                operationalReportService.getCompanyReport(companyId, startAt, endAt);

        byte[] csv = operationalReportCsvService.generateCsv(report);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-empresa-" + companyId + "-" + nowForFileName() + ".csv"
        );
    }

    @GetMapping("/tickets/export/csv")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportGlobalTicketsCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<OperationalTicketReportItemResponse> tickets =
                operationalReportDetailService.findGlobalTickets(startAt, endAt);

        byte[] csv = operationalReportDetailCsvService.generateTicketsCsv(tickets);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-tickets-global-" + nowForFileName() + ".csv"
        );
    }

    @GetMapping("/audit-logs/export/csv")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportGlobalAuditLogsCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<TicketAuditLogResponse> logs =
                operationalReportDetailService.findGlobalAuditLogs(startAt, endAt);

        byte[] csv = operationalReportDetailCsvService.generateAuditLogsCsv(logs);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-auditoria-global-" + nowForFileName() + ".csv"
        );
    }

    @GetMapping("/company/{companyId}/tickets/export/csv")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public ResponseEntity<byte[]> exportCompanyTicketsCsv(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<OperationalTicketReportItemResponse> tickets =
                operationalReportDetailService.findCompanyTickets(companyId, startAt, endAt);

        byte[] csv = operationalReportDetailCsvService.generateTicketsCsv(tickets);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-tickets-empresa-" + companyId + "-" + nowForFileName() + ".csv"
        );
    }

    @GetMapping("/company/{companyId}/audit-logs/export/csv")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public ResponseEntity<byte[]> exportCompanyAuditLogsCsv(
            @PathVariable UUID companyId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<TicketAuditLogResponse> logs =
                operationalReportDetailService.findCompanyAuditLogs(companyId, startAt, endAt);

        byte[] csv = operationalReportDetailCsvService.generateAuditLogsCsv(logs);

        return buildCsvResponse(
                csv,
                "relatorio-operacional-auditoria-empresa-" + companyId + "-" + nowForFileName() + ".csv"
        );
    }

    private ResponseEntity<byte[]> buildCsvResponse(byte[] csv, String fileName) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString()
                )
                .body(csv);
    }

    private String nowForFileName() {
        return LocalDateTime.now().format(FILE_DATE_FORMATTER);
    }
}