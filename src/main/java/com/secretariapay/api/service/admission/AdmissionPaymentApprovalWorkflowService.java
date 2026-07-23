package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdmissionPaymentApprovalWorkflowService {

    private final AdmissionService admissionService;
    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionOperationalNotificationService notificationService;

    public AdmissionPaymentApprovalWorkflowService(
            AdmissionService admissionService,
            AdmissionApplicationRepository applicationRepository,
            AdmissionOperationalNotificationService notificationService
    ) {
        this.admissionService = admissionService;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AdmissionDto.ApplicationResponse approvePaymentProof(
            UUID proofId,
            AdmissionDto.ReviewPaymentProofRequest request
    ) {
        AdmissionDto.ApplicationResponse response = admissionService.approvePaymentProof(proofId, request);
        enqueueEnrollmentDocumentsIfReleased(response);
        return response;
    }

    @Transactional
    public AdmissionDto.ApplicationResponse confirmInvoicePayment(
            UUID invoiceId,
            AdmissionDto.ReviewPaymentProofRequest request
    ) {
        AdmissionDto.ApplicationResponse response = admissionService.confirmInvoicePayment(invoiceId, request);
        enqueueEnrollmentDocumentsIfReleased(response);
        return response;
    }

    private void enqueueEnrollmentDocumentsIfReleased(AdmissionDto.ApplicationResponse response) {
        if (response == null
                || response.id() == null
                || response.status() != AdmissionApplicationStatus.DOCUMENTATION_PENDING) {
            return;
        }

        AdmissionApplication application = applicationRepository.findById(response.id())
                .orElseThrow(() -> new NotFoundException(
                        "O pagamento foi confirmado, mas a candidatura não pôde ser preparada para a matrícula."
                ));
        notificationService.enqueueEnrollmentDocumentsRequested(application);
    }
}
