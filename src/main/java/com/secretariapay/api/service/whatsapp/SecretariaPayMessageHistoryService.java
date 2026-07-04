package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.SecretariaPayMessagePreviewResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageStatusRequest;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SecretariaPayMessageHistoryService {

    private final SecretariaPayMessageTemplateService templateService;
    private final SecretariaPayMessageRepository repository;

    public SecretariaPayMessageHistoryService(
            SecretariaPayMessageTemplateService templateService,
            SecretariaPayMessageRepository repository
    ) {
        this.templateService = templateService;
        this.repository = repository;
    }

    @Transactional
    public SecretariaPayMessageResponse generateBeforeDue(UUID chargeId, Integer daysBefore) {
        return save(templateService.beforeDue(chargeId, daysBefore));
    }

    @Transactional
    public SecretariaPayMessageResponse generateDueToday(UUID chargeId) {
        return save(templateService.dueToday(chargeId));
    }

    @Transactional
    public SecretariaPayMessageResponse generateOverdue(UUID chargeId, Integer daysLate) {
        return save(templateService.overdue(chargeId, daysLate));
    }

    @Transactional
    public SecretariaPayMessageResponse generateProofReceived(UUID paymentProofId) {
        return save(templateService.proofReceived(paymentProofId));
    }

    @Transactional
    public SecretariaPayMessageResponse generateProofApproved(UUID paymentProofId) {
        return save(templateService.proofApproved(paymentProofId));
    }

    @Transactional
    public SecretariaPayMessageResponse generateReceiptIssued(UUID receiptId) {
        return save(templateService.receiptIssued(receiptId));
    }

    @Transactional
    public SecretariaPayMessageResponse generateRegularized(UUID studentId) {
        return save(templateService.regularized(studentId));
    }

    @Transactional(readOnly = true)
    public List<SecretariaPayMessageResponse> findByInstitution(UUID institutionId) {
        return repository.findByInstitutionIdOrderByCreatedAtDesc(institutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SecretariaPayMessageResponse> findByStudent(UUID studentId) {
        return repository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SecretariaPayMessageResponse> findByCharge(UUID chargeId) {
        return repository.findByChargeIdOrderByCreatedAtDesc(chargeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SecretariaPayMessageResponse markQueued(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId)
                .setStatus(SecretariaPayMessageStatus.QUEUED);

        return toResponse(repository.save(message));
    }

    @Transactional
    public SecretariaPayMessageResponse markSent(UUID messageId, SecretariaPayMessageStatusRequest request) {
        SecretariaPayMessage message = findMessage(messageId)
                .setStatus(SecretariaPayMessageStatus.SENT)
                .setSentAt(LocalDateTime.now())
                .setProviderMessageId(request != null ? request.getProviderMessageId() : null)
                .setFailureReason(null);

        return toResponse(repository.save(message));
    }

    @Transactional
    public SecretariaPayMessageResponse markFailed(UUID messageId, SecretariaPayMessageStatusRequest request) {
        SecretariaPayMessage message = findMessage(messageId)
                .setStatus(SecretariaPayMessageStatus.FAILED)
                .setFailureReason(request != null ? request.getFailureReason() : null);

        return toResponse(repository.save(message));
    }

    @Transactional
    public SecretariaPayMessageResponse markRead(UUID messageId) {
        SecretariaPayMessage message = findMessage(messageId)
                .setStatus(SecretariaPayMessageStatus.READ)
                .setReadAt(LocalDateTime.now());

        return toResponse(repository.save(message));
    }

    private SecretariaPayMessageResponse save(SecretariaPayMessagePreviewResponse preview) {
        String recipientPhone = WhatsappRecipientOverrideContext.current()
                .orElse(preview.getStudentWhatsapp());

        SecretariaPayMessage message = new SecretariaPayMessage()
                .setInstitutionId(preview.getInstitutionId())
                .setInstitutionName(preview.getInstitutionName())
                .setStudentId(preview.getStudentId())
                .setStudentNumber(preview.getStudentNumber())
                .setStudentName(preview.getStudentName())
                .setChargeId(preview.getChargeId())
                .setChargeCode(preview.getChargeCode())
                .setPaymentProofId(preview.getPaymentProofId())
                .setReceiptId(preview.getReceiptId())
                .setReceiptCode(preview.getReceiptCode())
                .setType(preview.getType())
                .setChannel(preview.getChannel())
                .setLanguage(preview.getLanguage())
                .setRecipientPhone(recipientPhone)
                .setMessage(preview.getMessage())
                .setStatus(SecretariaPayMessageStatus.GENERATED);

        return toResponse(repository.save(message));
    }

    private SecretariaPayMessage findMessage(UUID messageId) {
        return repository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem do SecretáriaPay não encontrada."));
    }

    private SecretariaPayMessageResponse toResponse(SecretariaPayMessage message) {
        return new SecretariaPayMessageResponse()
                .setId(message.getId())
                .setInstitutionId(message.getInstitutionId())
                .setInstitutionName(message.getInstitutionName())
                .setStudentId(message.getStudentId())
                .setStudentNumber(message.getStudentNumber())
                .setStudentName(message.getStudentName())
                .setChargeId(message.getChargeId())
                .setChargeCode(message.getChargeCode())
                .setPaymentProofId(message.getPaymentProofId())
                .setReceiptId(message.getReceiptId())
                .setReceiptCode(message.getReceiptCode())
                .setType(message.getType())
                .setChannel(message.getChannel())
                .setLanguage(message.getLanguage())
                .setRecipientPhone(message.getRecipientPhone())
                .setMessage(message.getMessage())
                .setStatus(message.getStatus())
                .setProviderMessageId(message.getProviderMessageId())
                .setFailureReason(message.getFailureReason())
                .setSentAt(message.getSentAt())
                .setReadAt(message.getReadAt())
                .setCreatedAt(message.getCreatedAt())
                .setUpdatedAt(message.getUpdatedAt());
    }
}
