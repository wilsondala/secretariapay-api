package com.secretariapay.api.controller.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionEnrollmentDocumentType;
import com.secretariapay.api.service.admission.AdmissionEnrollmentDocumentFileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionEnrollmentDocumentFileController {

    private static final String ADMINS = "'ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'";
    private static final String READ = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','MARKETING','ROLE_MARKETING','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','DCR_OPERADOR','ROLE_DCR_OPERADOR','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC','AUDITORIA','ROLE_AUDITORIA')";
    private static final String ADMISSIONS = "hasAnyAuthority(" + ADMINS + ",'ADMISSOES','ROLE_ADMISSOES','SECRETARIA','ROLE_SECRETARIA')";

    private final AdmissionEnrollmentDocumentFileStorageService storageService;

    public AdmissionEnrollmentDocumentFileController(
            AdmissionEnrollmentDocumentFileStorageService storageService
    ) {
        this.storageService = storageService;
    }

    @GetMapping("/applications/{applicationId}/enrollment-document-files")
    @PreAuthorize(READ)
    public List<AdmissionEnrollmentDocumentFileStorageService.DocumentFileResponse> list(
            @PathVariable UUID applicationId
    ) {
        return storageService.list(applicationId);
    }

    @PostMapping(
            value = "/applications/{applicationId}/enrollment-document-files/{documentType}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(ADMISSIONS)
    public AdmissionEnrollmentDocumentFileStorageService.DocumentFileResponse upload(
            @PathVariable UUID applicationId,
            @PathVariable AdmissionEnrollmentDocumentType documentType,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String uploadedBy = authentication == null || authentication.getName() == null
                ? "Secretaria / Admissões"
                : authentication.getName();
        return storageService.store(applicationId, documentType, file, uploadedBy);
    }

    @DeleteMapping("/enrollment-document-files/{fileId}")
    @PreAuthorize(ADMISSIONS)
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        storageService.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/enrollment-document-files/{fileId}/content")
    @PreAuthorize(READ)
    public ResponseEntity<Resource> content(@PathVariable UUID fileId) {
        AdmissionEnrollmentDocumentFileStorageService.StoredDocumentFile stored =
                storageService.load(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(stored.mimeType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(stored.originalFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore());
        return ResponseEntity.ok().headers(headers).body(stored.resource());
    }
}
