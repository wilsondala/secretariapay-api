package com.secretariapay.api.controller.admission;

import com.secretariapay.api.service.admission.AdmissionPaymentProofFileStorageService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/admissions/payment-proof-files")
public class AdmissionPaymentProofFileController {

    private static final String ACCESS = "hasAnyAuthority(" +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMISSOES','ROLE_ADMISSOES'," +
            "'SECRETARIA','ROLE_SECRETARIA','DCR_COORDENACAO','ROLE_DCR_COORDENACAO'," +
            "'DCR_OPERADOR','ROLE_DCR_OPERADOR','FINANCEIRO','ROLE_FINANCEIRO'," +
            "'TESOURARIA','ROLE_TESOURARIA','AUDITORIA','ROLE_AUDITORIA')";

    private final AdmissionPaymentProofFileStorageService storageService;

    public AdmissionPaymentProofFileController(
            AdmissionPaymentProofFileStorageService storageService
    ) {
        this.storageService = storageService;
    }

    @GetMapping("/{storedName:.+}")
    @PreAuthorize(ACCESS)
    public ResponseEntity<?> download(@PathVariable String storedName) {
        AdmissionPaymentProofFileStorageService.StoredPaymentProofFile stored =
                storageService.load(storedName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(stored.mimeType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(stored.storedName(), StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(stored.resource());
    }
}
