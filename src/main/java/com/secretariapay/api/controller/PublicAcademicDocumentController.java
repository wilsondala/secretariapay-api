package com.secretariapay.api.controller;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.service.academic.AcademicDocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/public/academic-documents")
public class PublicAcademicDocumentController {

    private final AcademicDocumentService service;

    public PublicAcademicDocumentController(AcademicDocumentService service) {
        this.service = service;
    }

    @GetMapping("/{documentCode}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String documentCode) {
        byte[] bytes = service.generatePublicPdf(documentCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("Declaracao_IMETRO_" + safeFilename(documentCode) + ".pdf", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/{documentCode}/validate")
    public AcademicDocumentDto.ValidationResponse validate(@PathVariable String documentCode) {
        return service.validatePublic(documentCode);
    }

    private String safeFilename(String value) {
        String safe = value == null ? "documento" : value.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() ? "documento" : safe;
    }
}
