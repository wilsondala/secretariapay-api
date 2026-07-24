package com.secretariapay.api.dto.enrollment;

import com.secretariapay.api.entity.enums.enrollment.EnrollmentInvoiceStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentPaymentProofStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class EnrollmentDto {

    private EnrollmentDto() {}

    public record EnrollmentFromAdmissionRequest(
            @NotNull LocalDate dueDate,
            Integer targetYearLevel,
            String paymentReference,
            String provider
    ) {}

    public record ReenrollmentRequest(
            @NotNull UUID studentId,
            @NotNull UUID targetCourseId,
            @NotBlank String targetShift,
            @NotBlank String academicYear,
            @NotNull @Positive Integer targetYearLevel,
            @NotNull LocalDate dueDate,
            String paymentReference,
            String provider
    ) {}

    public record PaymentProofRequest(
            @NotBlank String fileUrl,
            String fileName,
            String mimeType
    ) {}

    public record ReviewPaymentRequest(
            @NotBlank String reviewedBy,
            String reviewNote,
            String paymentMethod,
            String paymentReference,
            String provider,
            String externalTransactionId
    ) {}

    public record InvoiceResponse(
            UUID id,
            String invoiceCode,
            BigDecimal amount,
            String currency,
            LocalDate dueDate,
            EnrollmentInvoiceStatus status,
            String paymentMethod,
            String paymentReference,
            String provider,
            String externalTransactionId,
            LocalDateTime paidAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record PaymentProofResponse(
            UUID id,
            UUID invoiceId,
            String fileUrl,
            String fileName,
            String mimeType,
            EnrollmentPaymentProofStatus status,
            String reviewedBy,
            String reviewNote,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record EnrollmentResponse(
            UUID id,
            String requestCode,
            EnrollmentRequestType requestType,
            EnrollmentRequestStatus status,
            UUID institutionId,
            String institutionName,
            UUID campaignId,
            String campaignCode,
            String academicYear,
            UUID admissionApplicationId,
            String applicationCode,
            UUID studentId,
            String studentNumber,
            String fullName,
            String documentNumber,
            UUID targetCourseId,
            String targetCourseName,
            String targetShift,
            Integer targetYearLevel,
            LocalDateTime completedAt,
            InvoiceResponse invoice,
            PaymentProofResponse latestPaymentProof,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record DashboardRow(
            String requestCode,
            EnrollmentRequestType requestType,
            String fullName,
            String studentNumber,
            String documentNumber,
            String courseName,
            String shift,
            Integer yearLevel,
            EnrollmentRequestStatus status,
            EnrollmentInvoiceStatus paymentStatus,
            BigDecimal amount,
            LocalDateTime completedAt
    ) {}

    public record DashboardResponse(
            long totalEnrollments,
            long totalReenrollments,
            long completedEnrollments,
            long completedReenrollments,
            long awaitingEnrollmentPayment,
            long awaitingReenrollmentPayment,
            long enrollmentPaymentUnderReview,
            long reenrollmentPaymentUnderReview,
            BigDecimal enrollmentRevenue,
            BigDecimal reenrollmentRevenue,
            BigDecimal totalRevenue,
            List<DashboardRow> rows
    ) {}
}
