package com.secretariapay.api.entity.academic;

import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.entity.financial.Charge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_service_orders")
public class AcademicServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true, length = 80)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private AcademicServiceCatalog service;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", unique = true)
    private Charge charge;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_request_id", unique = true)
    private AcademicDocumentRequest documentRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcademicServiceOrderStatus status = AcademicServiceOrderStatus.SOLICITADO;

    @Column(length = 240)
    private String purpose;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "physical_location", length = 180)
    private String physicalLocation;

    @Column(name = "requested_by", length = 180)
    private String requestedBy;

    @Column(name = "printed_by", length = 180)
    private String printedBy;

    @Column(name = "signed_by", length = 180)
    private String signedBy;

    @Column(name = "whatsapp_sent_by", length = 180)
    private String whatsappSentBy;

    @Column(name = "delivered_by", length = 180)
    private String deliveredBy;

    @Column(name = "recipient_name", length = 180)
    private String recipientName;

    @Column(name = "recipient_document_number", length = 80)
    private String recipientDocumentNumber;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "payment_requested_at")
    private LocalDateTime paymentRequestedAt;

    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;

    @Column(name = "document_generated_at")
    private LocalDateTime documentGeneratedAt;

    @Column(name = "ready_for_print_at")
    private LocalDateTime readyForPrintAt;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "waiting_signature_at")
    private LocalDateTime waitingSignatureAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "ready_for_pickup_at")
    private LocalDateTime readyForPickupAt;

    @Column(name = "whatsapp_sent_at")
    private LocalDateTime whatsappSentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (requestedAt == null) requestedAt = now;
        if (status == null) status = AcademicServiceOrderStatus.SOLICITADO;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public AcademicServiceOrder setOrderCode(String orderCode) { this.orderCode = orderCode; return this; }
    public Student getStudent() { return student; }
    public AcademicServiceOrder setStudent(Student student) { this.student = student; return this; }
    public AcademicServiceCatalog getService() { return service; }
    public AcademicServiceOrder setService(AcademicServiceCatalog service) { this.service = service; return this; }
    public Charge getCharge() { return charge; }
    public AcademicServiceOrder setCharge(Charge charge) { this.charge = charge; return this; }
    public AcademicDocumentRequest getDocumentRequest() { return documentRequest; }
    public AcademicServiceOrder setDocumentRequest(AcademicDocumentRequest documentRequest) { this.documentRequest = documentRequest; return this; }
    public AcademicServiceOrderStatus getStatus() { return status; }
    public AcademicServiceOrder setStatus(AcademicServiceOrderStatus status) { this.status = status; return this; }
    public String getPurpose() { return purpose; }
    public AcademicServiceOrder setPurpose(String purpose) { this.purpose = purpose; return this; }
    public String getNotes() { return notes; }
    public AcademicServiceOrder setNotes(String notes) { this.notes = notes; return this; }
    public String getPhysicalLocation() { return physicalLocation; }
    public AcademicServiceOrder setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; return this; }
    public String getRequestedBy() { return requestedBy; }
    public AcademicServiceOrder setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; return this; }
    public String getPrintedBy() { return printedBy; }
    public AcademicServiceOrder setPrintedBy(String printedBy) { this.printedBy = printedBy; return this; }
    public String getSignedBy() { return signedBy; }
    public AcademicServiceOrder setSignedBy(String signedBy) { this.signedBy = signedBy; return this; }
    public String getWhatsappSentBy() { return whatsappSentBy; }
    public AcademicServiceOrder setWhatsappSentBy(String whatsappSentBy) { this.whatsappSentBy = whatsappSentBy; return this; }
    public String getDeliveredBy() { return deliveredBy; }
    public AcademicServiceOrder setDeliveredBy(String deliveredBy) { this.deliveredBy = deliveredBy; return this; }
    public String getRecipientName() { return recipientName; }
    public AcademicServiceOrder setRecipientName(String recipientName) { this.recipientName = recipientName; return this; }
    public String getRecipientDocumentNumber() { return recipientDocumentNumber; }
    public AcademicServiceOrder setRecipientDocumentNumber(String recipientDocumentNumber) { this.recipientDocumentNumber = recipientDocumentNumber; return this; }
    public String getDeliveryNotes() { return deliveryNotes; }
    public AcademicServiceOrder setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; return this; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public AcademicServiceOrder setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
    public LocalDateTime getPaymentRequestedAt() { return paymentRequestedAt; }
    public AcademicServiceOrder setPaymentRequestedAt(LocalDateTime paymentRequestedAt) { this.paymentRequestedAt = paymentRequestedAt; return this; }
    public LocalDateTime getPaymentConfirmedAt() { return paymentConfirmedAt; }
    public AcademicServiceOrder setPaymentConfirmedAt(LocalDateTime paymentConfirmedAt) { this.paymentConfirmedAt = paymentConfirmedAt; return this; }
    public LocalDateTime getDocumentGeneratedAt() { return documentGeneratedAt; }
    public AcademicServiceOrder setDocumentGeneratedAt(LocalDateTime documentGeneratedAt) { this.documentGeneratedAt = documentGeneratedAt; return this; }
    public LocalDateTime getReadyForPrintAt() { return readyForPrintAt; }
    public AcademicServiceOrder setReadyForPrintAt(LocalDateTime readyForPrintAt) { this.readyForPrintAt = readyForPrintAt; return this; }
    public LocalDateTime getPrintedAt() { return printedAt; }
    public AcademicServiceOrder setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; return this; }
    public LocalDateTime getWaitingSignatureAt() { return waitingSignatureAt; }
    public AcademicServiceOrder setWaitingSignatureAt(LocalDateTime waitingSignatureAt) { this.waitingSignatureAt = waitingSignatureAt; return this; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public AcademicServiceOrder setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; return this; }
    public LocalDateTime getReadyForPickupAt() { return readyForPickupAt; }
    public AcademicServiceOrder setReadyForPickupAt(LocalDateTime readyForPickupAt) { this.readyForPickupAt = readyForPickupAt; return this; }
    public LocalDateTime getWhatsappSentAt() { return whatsappSentAt; }
    public AcademicServiceOrder setWhatsappSentAt(LocalDateTime whatsappSentAt) { this.whatsappSentAt = whatsappSentAt; return this; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public AcademicServiceOrder setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}