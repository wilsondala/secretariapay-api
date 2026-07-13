package com.secretariapay.api.controller;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.service.academic.AcademicDocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-documents")
public class AcademicDocumentController {

    private static final String READ_AUTHORITIES = "hasAnyAuthority('ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC')";
    private static final String PREPARE_AUTHORITIES = "hasAnyAuthority('ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC')";
    private static final String SIGN_AUTHORITIES = "hasAnyAuthority('ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO')";
    private static final String SEND_AUTHORITIES = "hasAnyAuthority('ADMIN','ROLE_ADMIN','COMPANY_ADMIN','ROLE_COMPANY_ADMIN','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','DCR_COORDENACAO','ROLE_DCR_COORDENACAO','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC')";

    private final AcademicDocumentService service;

    public AcademicDocumentController(AcademicDocumentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(READ_AUTHORITIES)
    public List<AcademicDocumentDto.Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_AUTHORITIES)
    public AcademicDocumentDto.Response findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping("/demo/simple-declaration")
    @PreAuthorize(PREPARE_AUTHORITIES)
    public AcademicDocumentDto.Response createDemoDeclaration(
            @RequestBody AcademicDocumentDto.CreateDemoRequest request
    ) {
        return service.createDemoDeclaration(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(PREPARE_AUTHORITIES)
    public AcademicDocumentDto.Response update(
            @PathVariable UUID id,
            @RequestBody AcademicDocumentDto.UpdateRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/ready-for-signature")
    @PreAuthorize(PREPARE_AUTHORITIES)
    public AcademicDocumentDto.Response markReadyForSignature(@PathVariable UUID id) {
        return service.markReadyForSignature(id);
    }

    @PostMapping("/{id}/sign-demo")
    @PreAuthorize(SIGN_AUTHORITIES)
    public AcademicDocumentDto.Response signDemo(@PathVariable UUID id) {
        return service.signDemo(id);
    }

    @PostMapping("/{id}/send-whatsapp")
    @PreAuthorize(SEND_AUTHORITIES)
    public AcademicDocumentDto.Response sendByWhatsapp(@PathVariable UUID id) {
        return service.sendByWhatsapp(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize(READ_AUTHORITIES)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        AcademicDocumentDto.Response document = service.findById(id);
        byte[] bytes = service.generatePdf(id);
        String filename = "Declaracao_IMETRO_" + safeFilename(document.studentNumber()) + "_" + safeFilename(document.documentCode()) + ".pdf";
        return pdfResponse(bytes, filename);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String safeFilename(String value) {
        String safe = value == null ? "documento" : value.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() ? "documento" : safe;
    }
}
