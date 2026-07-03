package com.secretariapay.api.service.secretariapay;

import com.secretariapay.api.dto.financial.PaymentProofResponse;
import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.secretariapay.SecretariaPayFinancialFlowResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.repository.whatsapp.SecretariaPayMessageRepository;
import com.secretariapay.api.service.financial.PaymentProofService;
import com.secretariapay.api.service.financial.ReceiptService;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageDispatchService;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageHistoryService;
import com.secretariapay.api.service.whatsapp.SecretariaPayPaymentGuideMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SecretariaPayFinancialFlowService {

    private final SecretariaPayPaymentGuideMessageService paymentGuideMessageService;
    private final SecretariaPayMessageDispatchService messageDispatchService;
    private final PaymentProofService paymentProofService;
    private final PaymentProofRepository paymentProofRepository;
    private final ReceiptService receiptService;
    private final ReceiptRepository receiptRepository;
    private final SecretariaPayMessageHistoryService messageHistoryService;
    private final SecretariaPayMessageRepository messageRepository;

    public SecretariaPayFinancialFlowService(
            SecretariaPayPaymentGuideMessageService paymentGuideMessageService,
            SecretariaPayMessageDispatchService messageDispatchService,
            PaymentProofService paymentProofService,
            PaymentProofRepository paymentProofRepository,
            ReceiptService receiptService,
            ReceiptRepository receiptRepository,
            SecretariaPayMessageHistoryService messageHistoryService,
            SecretariaPayMessageRepository messageRepository
    ) {
        this.paymentGuideMessageService = paymentGuideMessageService;
        this.messageDispatchService = messageDispatchService;
        this.paymentProofService = paymentProofService;
        this.paymentProofRepository = paymentProofRepository;
        this.receiptService = receiptService;
        this.receiptRepository = receiptRepository;
        this.messageHistoryService = messageHistoryService;
        this.messageRepository = messageRepository;
    }

    public SecretariaPayFinancialFlowResponse sendPaymentGuide(UUID chargeId) {
        SecretariaPayMessageResponse guideMessage = paymentGuideMessageService.generatePaymentGuideMessage(chargeId);
        SecretariaPayMessageDispatchResult dispatch = messageDispatchService.dispatch(guideMessage.getId());

        return new SecretariaPayFinancialFlowResponse()
                .setFlow("PAYMENT_GUIDE_SEND")
                .setStatus(dispatch.getStatus())
                .setMessage("Guia de pagamento gerada e enviada para o estudante.")
                .setChargeId(guideMessage.getChargeId())
                .setChargeCode(guideMessage.getChargeCode())
                .setGuideMessageId(guideMessage.getId())
                .setGuideDispatchStatus(dispatch.getStatus())
                .setGuideProviderMessageId(dispatch.getProviderMessageId())
                .setFailureReason(dispatch.getFailureReason())
                .setProcessedAt(LocalDateTime.now());
    }

    public SecretariaPayFinancialFlowResponse approveProofIssueReceiptAndNotify(
            UUID paymentProofId,
            PaymentProofReviewRequest request
    ) {
        PaymentProofResponse proof = paymentProofService.approve(paymentProofId, request);

        if (proof.getChargeId() == null) {
            throw new NotFoundException("Cobrança do comprovativo não encontrada.");
        }

        ReceiptResponse receipt = findExistingReceiptForCharge(proof.getChargeId());
        boolean receiptAlreadyExisted = receipt != null;

        if (receipt == null) {
            receipt = receiptService.issueForCharge(proof.getChargeId());
        }

        SecretariaPayMessageResponse receiptMessage = messageHistoryService.generateReceiptIssued(receipt.getId());
        SecretariaPayMessageDispatchResult dispatch = messageDispatchService.dispatch(receiptMessage.getId());

        return new SecretariaPayFinancialFlowResponse()
                .setFlow("APPROVE_PROOF_ISSUE_RECEIPT_NOTIFY")
                .setStatus(dispatch.getStatus())
                .setMessage(receiptAlreadyExisted
                        ? "Comprovativo aprovado. Recibo existente reenviado ao estudante."
                        : "Comprovativo aprovado. Cobrança paga, recibo emitido e enviado ao estudante.")
                .setChargeId(proof.getChargeId())
                .setChargeCode(proof.getChargeCode())
                .setPaymentProofId(proof.getId())
                .setReceiptId(receipt.getId())
                .setReceiptCode(receipt.getReceiptCode())
                .setReceiptMessageId(receiptMessage.getId())
                .setReceiptDispatchStatus(dispatch.getStatus())
                .setReceiptProviderMessageId(dispatch.getProviderMessageId())
                .setPdfUrl(receipt.getPdfUrl())
                .setValidationUrl(receipt.getValidationUrl())
                .setFailureReason(dispatch.getFailureReason())
                .setProcessedAt(LocalDateTime.now());
    }

    public SecretariaPayFinancialFlowResponse rejectProofAndNotifyStudent(
            UUID paymentProofId,
            PaymentProofReviewRequest request
    ) {
        PaymentProofResponse proofResponse = paymentProofService.reject(paymentProofId, request);
        PaymentProof proof = paymentProofRepository.findById(paymentProofId)
                .orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));

        SecretariaPayMessage rejectionMessage = messageRepository.save(buildRejectionMessage(proof));
        SecretariaPayMessageDispatchResult dispatch = messageDispatchService.dispatch(rejectionMessage.getId());

        return new SecretariaPayFinancialFlowResponse()
                .setFlow("REJECT_PAYMENT_PROOF_NOTIFY")
                .setStatus(dispatch.getStatus())
                .setMessage("Comprovativo rejeitado. O aluno foi notificado para reenviar o comprovativo correto.")
                .setChargeId(proofResponse.getChargeId())
                .setChargeCode(proofResponse.getChargeCode())
                .setPaymentProofId(proofResponse.getId())
                .setStudentNotificationMessageId(rejectionMessage.getId())
                .setStudentNotificationDispatchStatus(dispatch.getStatus())
                .setStudentNotificationProviderMessageId(dispatch.getProviderMessageId())
                .setFailureReason(dispatch.getFailureReason() != null ? dispatch.getFailureReason() : proofResponse.getReviewNote())
                .setProcessedAt(LocalDateTime.now());
    }

    private SecretariaPayMessage buildRejectionMessage(PaymentProof proof) {
        Charge charge = proof.getCharge();
        Student student = charge != null ? charge.getStudent() : null;
        Institution institution = institution(student);

        String messageText = """
                Comprovativo não aprovado.

                Cobrança: %s
                Motivo: %s

                Por favor, confira os dados do pagamento e envie novamente uma imagem ou PDF legível do comprovativo.

                O recibo digital só será emitido após a confirmação do pagamento pela tesouraria.

                %s
                SecretáriaPay Académico
                """.formatted(
                charge != null ? safe(charge.getChargeCode()) : "não identificada",
                safe(proof.getReviewNote()).isBlank() ? "Comprovativo pendente de correção." : proof.getReviewNote(),
                displayInstitutionName(institution)
        ).trim();

        return new SecretariaPayMessage()
                .setInstitutionId(institution != null ? institution.getId() : null)
                .setInstitutionName(displayInstitutionName(institution))
                .setStudentId(student != null ? student.getId() : null)
                .setStudentNumber(student != null ? student.getStudentNumber() : null)
                .setStudentName(student != null ? student.getFullName() : null)
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setPaymentProofId(proof.getId())
                .setType("PAYMENT_PROOF_REJECTED")
                .setChannel("WHATSAPP")
                .setLanguage("pt-AO")
                .setRecipientPhone(resolveRecipientPhone(student, proof))
                .setMessage(messageText)
                .setStatus(SecretariaPayMessageStatus.GENERATED);
    }

    private String resolveRecipientPhone(Student student, PaymentProof proof) {
        if (student != null && student.getWhatsapp() != null && !student.getWhatsapp().isBlank()) {
            return student.getWhatsapp();
        }

        if (proof.getSubmittedByPhone() != null && !proof.getSubmittedByPhone().isBlank()) {
            return proof.getSubmittedByPhone();
        }

        return student != null ? student.getPhone() : null;
    }

    private ReceiptResponse findExistingReceiptForCharge(UUID chargeId) {
        return receiptRepository.findByChargeId(chargeId)
                .map(receiptService::toResponse)
                .orElse(null);
    }

    private Institution institution(Student student) {
        if (student == null || student.getAcademicClass() == null) {
            return null;
        }

        AcademicClass academicClass = student.getAcademicClass();
        if (academicClass.getCourse() == null) {
            return null;
        }

        Course course = academicClass.getCourse();
        return course.getInstitution();
    }

    private String displayInstitutionName(Institution institution) {
        if (institution == null) {
            return "SecretáriaPay Académico";
        }

        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) {
            return institution.getLegalName();
        }

        if (institution.getName() != null && !institution.getName().isBlank()) {
            return institution.getName();
        }

        return "SecretáriaPay Académico";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
