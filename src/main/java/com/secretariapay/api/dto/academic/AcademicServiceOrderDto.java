package com.secretariapay.api.dto.academic;

import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AcademicServiceOrderDto {

    private AcademicServiceOrderDto() {
    }

    public record CreateRequest(
            UUID studentId,
            UUID serviceId,
            String purpose,
            String notes
    ) {
    }

    public record RequestPaymentRequest(
            LocalDate dueDate
    ) {
    }

    public record ActionRequest(
            String physicalLocation,
            String recipientName,
            String recipientDocumentNumber,
            String notes
    ) {
    }

    public record Response(
            UUID id,
            String orderCode,
            AcademicServiceOrderStatus status,
            UUID studentId,
            String studentNumber,
            String studentName,
            String studentWhatsapp,
            String courseName,
            UUID serviceId,
            String serviceCode,
            String serviceName,
            String serviceCategory,
            BigDecimal amount,
            String currency,
            UUID chargeId,
            String chargeCode,
            ChargeStatus chargeStatus,
            UUID documentRequestId,
            String documentCode,
            String documentType,
            String purpose,
            String notes,
            String physicalLocation,
            String requestedBy,
            String printedBy,
            String signedBy,
            String whatsappSentBy,
            String deliveredBy,
            String recipientName,
            String recipientDocumentNumber,
            String deliveryNotes,
            LocalDateTime requestedAt,
            LocalDateTime paymentRequestedAt,
            LocalDateTime paymentConfirmedAt,
            LocalDateTime documentGeneratedAt,
            LocalDateTime readyForPrintAt,
            LocalDateTime printedAt,
            LocalDateTime waitingSignatureAt,
            LocalDateTime signedAt,
            LocalDateTime readyForPickupAt,
            LocalDateTime whatsappSentAt,
            LocalDateTime deliveredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}