package com.secretariapay.api.controller.imports;

import com.secretariapay.api.dto.imports.AcademicStudentImportSyncResponse;
import com.secretariapay.api.dto.imports.AcademicStudentImportValidationResponse;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.service.imports.AcademicStudentCsvImportService;
import com.secretariapay.api.service.imports.AcademicStudentImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-student-imports")
public class AcademicStudentImportController {

    private static final String IMPORT_AUTHORITIES = "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')";

    private final AcademicStudentImportService service;
    private final AcademicStudentCsvImportService csvImportService;

    public AcademicStudentImportController(
            AcademicStudentImportService service,
            AcademicStudentCsvImportService csvImportService
    ) {
        this.service = service;
        this.csvImportService = csvImportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportBatch createBatch(@RequestBody AcademicStudentImportBatch request) {
        return service.createBatch(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportBatch uploadCsv(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID institutionId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String sourceName,
            Principal principal
    ) {
        return csvImportService.upload(
                file,
                institutionId,
                academicYear,
                semester,
                sourceName,
                principal == null ? null : principal.getName()
        );
    }

    @GetMapping
    @PreAuthorize(IMPORT_AUTHORITIES)
    public List<AcademicStudentImportBatch> findAll() {
        return service.findAll();
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public List<AcademicStudentImportBatch> findByInstitution(@PathVariable UUID institutionId) {
        return service.findByInstitution(institutionId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportBatch findBatch(@PathVariable UUID id) {
        return service.findBatch(id);
    }

    @PostMapping("/{id}/rows")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportRow addRow(@PathVariable UUID id, @RequestBody AcademicStudentImportRow request) {
        return service.addRow(id, request);
    }

    @GetMapping("/{id}/rows")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public List<AcademicStudentImportRow> findRows(@PathVariable UUID id) {
        return service.findRows(id);
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportValidationResponse validateBatch(@PathVariable UUID id) {
        return service.validateBatch(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportValidationResponse completeBatch(@PathVariable UUID id) {
        return service.completeBatch(id);
    }

    @PatchMapping("/{id}/sync")
    @PreAuthorize(IMPORT_AUTHORITIES)
    public AcademicStudentImportSyncResponse syncBatch(@PathVariable UUID id) {
        return service.syncBatch(id);
    }
}