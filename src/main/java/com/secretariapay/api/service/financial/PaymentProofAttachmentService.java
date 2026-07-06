package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PaymentProofAttachmentService {

    private final PaymentProofRepository paymentProofRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    public PaymentProofAttachmentService(
            PaymentProofRepository paymentProofRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient
    ) {
        this.paymentProofRepository = paymentProofRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
    }

    public ResponseEntity<byte[]> openAttachment(UUID id) {
        PaymentProof proof = paymentProofRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprovativo não encontrado."));

        String fileUrl = safe(proof.getFileUrl());
        if (fileUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprovativo sem anexo.");
        }

        if (!fileUrl.startsWith("whatsapp-cloud-media://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anexo não é mídia privada do WhatsApp.");
        }

        ResponseEntity<byte[]> mediaResponse = whatsAppCloudApiClient.downloadMediaByReference(fileUrl);
        byte[] body = mediaResponse.getBody();
        if (body == null || body.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anexo vazio.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(proof, mediaResponse));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(resolveFileName(proof), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private MediaType resolveMediaType(PaymentProof proof, ResponseEntity<byte[]> mediaResponse) {
        String mimeType = firstNonBlank(
                proof.getMimeType(),
                mediaResponse.getHeaders().getContentType() != null ? mediaResponse.getHeaders().getContentType().toString() : null
        );
        if (mimeType.isBlank()) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String resolveFileName(PaymentProof proof) {
        return firstNonBlank(proof.getFileName(), "comprovativo-" + proof.getId()).replaceAll("[\\r\\n]", "").trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
