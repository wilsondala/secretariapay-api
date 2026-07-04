package com.secretariapay.api.controller.imports;

import com.secretariapay.api.dto.imports.AcademicStudentImportValidationResponse;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.service.imports.AcademicStudentImportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-student-imports")
public class AcademicStudentImportController {

    private final AcademicStudentImportService service;

    public AcademicStudentImportController(AcademicStudentImportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public AcademicStudentImportBatch createBatch(@RequestBody AcademicStudentImportBatch request) {
        return service.createBatch(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public List<AcademicStudentImportBatch> findAll() {
        return service.findAll();
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public List<AcademicStudentImportBatch> findByInstitution(@PathVariable UUID institutionId) {
        return service.findByInstitution(institutionId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public AcademicStudentImportBatch findBatch(@PathVariable UUID id) {
        return service.findBatch(id);
    }

    @PostMapping("/{id}/rows")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public AcademicStudentImportRow addRow(@PathVariable UUID id, @RequestBody AcademicStudentImportRow request) {
        return service.addRow(id, request);
    }

    @GetMapping("/{id}/rows")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public List<AcademicStudentImportRow> findRows(@PathVariable UUID id) {
        return service.findRows(id);
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public AcademicStudentImportValidationResponse validateBatch(@PathVariable UUID id) {
        return service.validateBatch(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'SECRETARIA', 'ROLE_SECRETARIA', 'TIC', 'ROLE_TIC')")
    public AcademicStudentImportValidationResponse completeBatch(@PathVariable UUID id) {
        return service.completeBatch(id);
    }
}
