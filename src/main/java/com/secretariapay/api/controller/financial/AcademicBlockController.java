package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.AcademicBlockReleaseRequest;
import com.secretariapay.api.dto.financial.AcademicBlockRequest;
import com.secretariapay.api.dto.financial.AcademicBlockResponse;
import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;
import com.secretariapay.api.service.financial.AcademicBlockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-blocks")
public class AcademicBlockController {

    private final AcademicBlockService service;

    public AcademicBlockController(AcademicBlockService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public AcademicBlockResponse create(@Valid @RequestBody AcademicBlockRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<AcademicBlockResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public AcademicBlockResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<AcademicBlockResponse> findByStudent(@PathVariable UUID studentId) {
        return service.findByStudent(studentId);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public List<AcademicBlockResponse> findByStatus(@PathVariable AcademicBlockStatus status) {
        return service.findByStatus(status);
    }

    @PatchMapping("/{id}/release")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'DIRECAO', 'ROLE_DIRECAO', 'FINANCEIRO', 'ROLE_FINANCEIRO', 'SECRETARIA', 'ROLE_SECRETARIA')")
    public AcademicBlockResponse release(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicBlockReleaseRequest request
    ) {
        return service.release(id, request);
    }
}
