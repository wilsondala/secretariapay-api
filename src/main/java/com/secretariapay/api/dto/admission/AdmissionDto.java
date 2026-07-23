package com.secretariapay.api.dto.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionPaymentProofStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdmissionDto {

    private AdmissionDto() {}

    public record LeadRequest(
            @NotNull UUID institutionId,
            UUID desiredCourseId,
            @NotBlank String fullName,
            String phone,
            String whatsapp,
            @Email String email,
            String documentNumber,
            String desiredShift,
            String province,
            String municipality,
            String leadSource,
            Boolean consentGiven,
            String notes
    ) {}

    public record LeadResponse(
            UUID id,
            UUID institutionId,
            String institutionName,
            UUID desiredCourseId,
            String desiredCourseName,
            String fullName,
            String phone,
            String whatsapp,
            String email,
            String documentNumber,
            String desiredShift,
            String province,
            String municipality,
            String leadSource,
            Boolean consentGiven,
            AdmissionLeadStatus status,
            LocalDateTime lastContactAt,
            LocalDateTime convertedAt,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApplicationRequest(
            @NotNull UUID institutionId,
            UUID leadId,
            @NotNull UUID desiredCourseId,
            @NotBlank String desiredShift,
            @NotBlank String academicYear,
            @NotBlank String fullName,
            String documentType,
            @NotBlank String documentNumber,
            LocalDate birthDate,
            String phone,
            String whatsapp,
            @Email String email,
            String previousSchool,
            String province,
            String municipality,
            Boolean documentsComplete,
            Boolean termsAccepted,
            String notes
    ) {}

    public record InvoiceRequest(
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate dueDate,
            String paymentReference,
            String provider
    ) {}

    public record PaymentProofRequest(
            @NotBlank String fileUrl,
            String fileName,
            String mimeType
    ) {}

    public record PublicApplicationAccessRequest(
            @NotBlank String documentNumber
    ) {}

    public record PublicPaymentProofRequest(
            @NotBlank String documentNumber,
            @NotBlank String fileUrl,
            String fileName,
            String mimeType
    ) {}

    public record PublicPaymentInstructionsResponse(
            boolean enabled,
            boolean provisional,
            String environmentLabel,
            String bankName,
            String accountHolder,
            String iban,
            String accountNumber,
            String multicaixaReference,
            String mobileMoneyInfo,
            String supportWhatsapp,
            String supportEmail,
            String notice
    ) {}

    public record PublicPaymentResponse(
            String applicationCode,
            String fullName,
            String desiredCourseName,
            String desiredShift,
            String academicYear,
            AdmissionApplicationStatus applicationStatus,
            InvoiceResponse invoice,
            PaymentProofResponse latestPaymentProof,
            PublicPaymentInstructionsResponse paymentInstructions
    ) {}

    public record ReviewPaymentProofRequest(
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
            AdmissionInvoiceStatus status,
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
            AdmissionPaymentProofStatus status,
            String reviewedBy,
            String reviewNote,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApplicationResponse(
            UUID id,
            String applicationCode,
            UUID institutionId,
            String institutionName,
            UUID campaignId,
            String campaignCode,
            UUID leadId,
            UUID desiredCourseId,
            String desiredCourseName,
            String desiredShift,
            String academicYear,
            AdmissionSourceChannel sourceChannel,
            String fullName,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phone,
            String whatsapp,
            String email,
            String previousSchool,
            String province,
            String municipality,
            Boolean documentsComplete,
            Boolean termsAccepted,
            AdmissionApplicationStatus status,
            String notes,
            LocalDateTime submittedAt,
            LocalDateTime confirmedAt,
            InvoiceResponse invoice,
            PaymentProofResponse latestPaymentProof,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ApplicationStatusRequest(
            @NotNull AdmissionApplicationStatus status,
            String notes
    ) {}

    public record ApplicationDocumentsRequest(
            @NotNull Boolean documentsComplete,
            @NotBlank String reviewedBy,
            String notes
    ) {}

    public record EnrollmentDocumentChecklistRequest(
            @NotNull Boolean twoPassportPhotos,
            @NotNull Boolean authenticatedCertificateCopy,
            @NotNull Boolean identityDocumentCopy,
            @NotNull Boolean studiedAbroad,
            @NotNull Boolean educationEquivalenceCopy,
            @NotNull Boolean secondaryEducationCompleted,
            @NotBlank String reviewedBy,
            String notes
    ) {}

    public record EnrollmentDocumentChecklistResponse(
            UUID id,
            UUID applicationId,
            String applicationCode,
            Boolean twoPassportPhotos,
            Boolean authenticatedCertificateCopy,
            Boolean identityDocumentCopy,
            Boolean studiedAbroad,
            Boolean educationEquivalenceCopy,
            Boolean secondaryEducationCompleted,
            Boolean ageEligible,
            Boolean documentsComplete,
            String reviewedBy,
            String notes,
            LocalDateTime reviewedAt,
            UUID enrollmentRequestId,
            String enrollmentRequestCode,
            EnrollmentRequestStatus enrollmentStatus,
            BigDecimal enrollmentAmount,
            String enrollmentCurrency,
            LocalDate enrollmentDueDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record ReportRow(
            String applicationCode,
            String fullName,
            String documentNumber,
            String phone,
            String desiredCourseName,
            String desiredShift,
            AdmissionApplicationStatus applicationStatus,
            AdmissionInvoiceStatus paymentStatus,
            LocalDateTime submittedAt,
            LocalDateTime confirmedAt
    ) {}

    public record DashboardResponse(
            long totalLeads,
            long contactedLeads,
            long submittedApplications,
            long awaitingPayment,
            long paymentUnderReview,
            long confirmedApplications,
            BigDecimal totalInvoiced,
            BigDecimal totalPaid,
            List<ReportRow> applications
    ) {}

    public record CatalogShiftResponse(
            String code,
            String label
    ) {}

    public record CatalogCourseResponse(
            UUID courseId,
            String courseCode,
            String courseName,
            String decreeReference,
            List<CatalogShiftResponse> shifts
    ) {}

    public record CatalogDepartmentResponse(
            String departmentCode,
            List<CatalogCourseResponse> courses
    ) {}

    public record CatalogResponse(
            UUID institutionId,
            String institutionName,
            UUID campaignId,
            String campaignCode,
            String academicYear,
            LocalDate registrationStart,
            LocalDate registrationEnd,
            boolean registrationOpen,
            BigDecimal registrationFee,
            BigDecimal enrollmentFee,
            BigDecimal reenrollmentFee,
            String currency,
            boolean publicFormEnabled,
            boolean whatsappEnabled,
            List<CatalogDepartmentResponse> departments
    ) {}
}
