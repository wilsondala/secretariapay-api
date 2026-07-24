package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionPublicApplicationWorkflowService {

    private final AdmissionService admissionService;
    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionOperationalNotificationService notificationService;

    public AdmissionPublicApplicationWorkflowService(
            AdmissionService admissionService,
            AdmissionApplicationRepository applicationRepository,
            AdmissionOperationalNotificationService notificationService
    ) {
        this.admissionService = admissionService;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AdmissionDto.ApplicationResponse createApplication(AdmissionDto.ApplicationRequest request) {
        AdmissionDto.ApplicationResponse response = admissionService.createApplication(
                request,
                AdmissionSourceChannel.FORM
        );

        if (response.status() == AdmissionApplicationStatus.SUBMITTED) {
            AdmissionApplication application = applicationRepository.findById(response.id())
                    .orElseThrow(() -> new NotFoundException(
                            "A candidatura foi criada, mas não pôde ser preparada para acompanhamento."
                    ));
            notificationService.enqueueApplicationSubmitted(application);
        }

        return response;
    }
}
