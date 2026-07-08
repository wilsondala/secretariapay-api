package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.service.GuidePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/guides")
public class PublicGuideController {

    private final GuidePdfService guidePdfService;

    public PublicGuideController(GuidePdfService guidePdfService) {
        this.guidePdfService = guidePdfService;
    }

    @GetMapping("/{guideCode}")
    public ResponseEntity<Map<String, Object>> getGuide(@PathVariable String guideCode) {
        GuideFallbackRequest guide = guidePdfService.buildDemoGuide(guideCode);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("guideCode", guide.getGuideCode());
        response.put("studentName", guide.getStudentName());
        response.put("studentNumber", guide.getStudentNumber());
        response.put("amount", guide.getAmount());
        response.put("currency", guide.getCurrency());
        response.put("dueDate", guide.getDueDate());
        response.put("status", "PENDING");
        response.put("message", guide.getMessage());
        response.put("pdfUrl", "/api/v1/public/guides/" + guide.getGuideCode() + "/pdf");
        response.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{guideCode}/pdf")
    public ResponseEntity<byte[]> getGuidePdf(@PathVariable String guideCode) {
        GuideFallbackRequest guide = guidePdfService.buildDemoGuide(guideCode);
        byte[] pdf = guidePdfService.generateGuidePdf(guide);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("guia-" + guide.getGuideCode() + ".pdf")
                        .build()
                        .toString())
                .body(pdf);
    }
}
