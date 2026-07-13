package com.secretariapay.api.dto.academic;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AcademicDocumentDto {

    private AcademicDocumentDto() {
    }

    public record CreateDemoRequest(
            String studentNumber,
            String purpose,
            String declarationText
    ) {
    }

    public record UpdateRequest(
            String purpose,
            String declarationText
    ) {
    }

    public record Response(
            UUID id,
            String documentCode,
            UUID studentId,
            String studentNumber,
            String studentName,
            String documentNumber,
            String courseName,
            String academicClassName,
            String academicYear,
            String serviceCode,
            String documentType,
            String status,
            String purpose,
            String declarationText,
            String signatoryName,
            String signatoryRole,
            String signatureMethod,
            String documentHash,
            int versionNumber,
            boolean demoMode,
            LocalDateTime issuedAt,
            LocalDateTime signedAt,
            LocalDateTime sentAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String pdfUrl,
            String validationUrl
    ) {
    }

    public record ValidationResponse(
            boolean valid,
            String documentCode,
            String status,
            String studentNumber,
            String studentName,
            String documentType,
            String signatoryName,
            String signatoryRole,
            String documentHash,
            int versionNumber,
            boolean demoMode,
            LocalDateTime issuedAt,
            LocalDateTime signedAt
    ) {
    }
}
