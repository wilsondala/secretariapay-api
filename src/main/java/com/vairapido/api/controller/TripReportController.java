package com.vairapido.api.controller;

import com.vairapido.api.dto.report.TripReportResponse;
import com.vairapido.api.dto.report.TripTicketReportItemResponse;
import com.vairapido.api.service.TripReportCsvService;
import com.vairapido.api.service.TripReportService;
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
@RequestMapping("/api/v1/reports/trips")
public class TripReportController {

    private static final DateTimeFormatter FILE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TripReportService tripReportService;
    private final TripReportCsvService tripReportCsvService;

    public TripReportController(
            TripReportService tripReportService,
            TripReportCsvService tripReportCsvService
    ) {
        this.tripReportService = tripReportService;
        this.tripReportCsvService = tripReportCsvService;
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripReportResponse getTripReport(
            @PathVariable UUID tripId,

            @RequestParam(required = false, defaultValue = "BRL")
            String currency
    ) {
        return tripReportService.getTripReport(tripId, currency);
    }

    @GetMapping("/{tripId}/financial")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripReportResponse getTripFinancialReport(
            @PathVariable UUID tripId,

            @RequestParam(required = false, defaultValue = "BRL")
            String currency
    ) {
        return tripReportService.getTripFinancialReport(tripId, currency);
    }

    @GetMapping("/{tripId}/tickets")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public List<TripTicketReportItemResponse> getTripTickets(
            @PathVariable UUID tripId
    ) {
        return tripReportService.findTripTickets(tripId);
    }

    @GetMapping("/{tripId}/export/csv")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public ResponseEntity<byte[]> exportTripReportCsv(
            @PathVariable UUID tripId,

            @RequestParam(required = false, defaultValue = "BRL")
            String currency
    ) {
        TripReportResponse report =
                tripReportService.getTripReport(tripId, currency);

        List<TripTicketReportItemResponse> tickets =
                tripReportService.findTripTickets(tripId);

        byte[] csv = tripReportCsvService.generateTripReportCsv(
                report,
                tickets
        );

        return buildCsvResponse(
                csv,
                "relatorio-viagem-" + tripId + "-" + nowForFileName() + ".csv"
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