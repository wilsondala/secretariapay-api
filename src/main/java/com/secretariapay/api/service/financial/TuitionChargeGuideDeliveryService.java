package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryItem;
import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryRequest;
import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.notification.NotificationDispatchResult;
import com.secretariapay.api.service.notification.SecretariaPayEmailNotificationService;
import com.secretariapay.api.service.notification.SecretariaPaySmsNotificationService;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageDispatchService;
import com.secretariapay.api.service.whatsapp.SecretariaPayPaymentGuideMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TuitionChargeGuideDeliveryService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private final ChargeRepository chargeRepository;
    private final SecretariaPayPaymentGuideMessageService paymentGuideMessageService;
    private final SecretariaPayMessageDispatchService messageDispatchService;
    private final SecretariaPayEmailNotificationService emailNotificationService;
    private final SecretariaPaySmsNotificationService smsNotificationService;

    public TuitionChargeGuideDeliveryService(
            ChargeRepository chargeRepository,
            SecretariaPayPaymentGuideMessageService paymentGuideMessageService,
            SecretariaPayMessageDispatchService messageDispatchService,
            SecretariaPayEmailNotificationService emailNotificationService,
            SecretariaPaySmsNotificationService smsNotificationService
    ) {
        this.chargeRepository = chargeRepository;
        this.paymentGuideMessageService = paymentGuideMessageService;
        this.messageDispatchService = messageDispatchService;
        this.emailNotificationService = emailNotificationService;
        this.smsNotificationService = smsNotificationService;
    }

    @Transactional
    public TuitionChargeGuideDeliveryResponse sendGuides(TuitionChargeGuideDeliveryRequest request) {
        List<Charge> selectedCharges = selectCharges(request);
        TuitionChargeGuideDeliveryResponse response = new TuitionChargeGuideDeliveryResponse()
                .setInstitutionId(request.getInstitutionId())
                .setReferenceMonth(clean(request.getReferenceMonth()))
                .setChargeCodePrefix(defaultIfBlank(request.getChargeCodePrefix(), "IMT-PROPINA-"))
                .setSelectedCharges(selectedCharges.size())
                .setProcessedAt(LocalDateTime.now());

        int processed = 0;
        int sentWhatsapp = 0;
        int failedWhatsapp = 0;
        int sentEmail = 0;
        int failedEmail = 0;
        int sentSms = 0;
        int failedSms = 0;
        int skippedNoContact = 0;
        int skippedNotPending = 0;

        for (Charge charge : selectedCharges) {
            TuitionChargeGuideDeliveryItem item = baseItem(charge);
            response.getItems().add(item);

            if (Boolean.TRUE.equals(request.getOnlyPending()) && charge.getStatus() != ChargeStatus.PENDING) {
                skippedNotPending++;
                item.setFinalStatus("SKIPPED_NOT_PENDING")
                        .setAction("Cobrança fora do estado PENDING.");
                continue;
            }

            processed++;
            Student student = charge.getStudent();
            boolean hasWhatsapp = student != null && !isBlank(student.getWhatsapp());
            boolean hasEmail = student != null && (!isBlank(student.getEmail()) || !isBlank(student.getGuardianEmail()));
            boolean hasPhone = student != null && (!isBlank(student.getPhone()) || !isBlank(student.getGuardianPhone()));
            boolean delivered = false;

            if (Boolean.TRUE.equals(request.getSendWhatsapp()) && hasWhatsapp) {
                try {
                    SecretariaPayMessageResponse message = paymentGuideMessageService.generatePaymentGuideMessage(charge.getId());
                    SecretariaPayMessageDispatchResult dispatchResult = messageDispatchService.dispatch(message.getId());
                    item.setWhatsappMessageId(dispatchResult.getMessageId())
                            .setWhatsappStatus(dispatchResult.getStatus())
                            .setWhatsappFailureReason(dispatchResult.getFailureReason());

                    if ("SENT".equalsIgnoreCase(dispatchResult.getStatus())) {
                        sentWhatsapp++;
                        delivered = true;
                    } else {
                        failedWhatsapp++;
                    }
                } catch (Exception exception) {
                    failedWhatsapp++;
                    item.setWhatsappStatus("FAILED")
                            .setWhatsappFailureReason(exception.getMessage());
                }
            }

            if (Boolean.TRUE.equals(request.getSendEmail()) && hasEmail) {
                NotificationDispatchResult emailResult = emailNotificationService.sendPaymentGuideEmail(charge, item.getGuideUrl());
                item.setEmailMessageId(emailResult.getMessageId())
                        .setEmailStatus(emailResult.getStatus())
                        .setEmailFailureReason(emailResult.getFailureReason());

                if ("SENT".equalsIgnoreCase(emailResult.getStatus())) {
                    sentEmail++;
                    delivered = true;
                } else {
                    failedEmail++;
                }
            }

            if (Boolean.TRUE.equals(request.getSendSms()) && !hasWhatsapp && hasPhone) {
                NotificationDispatchResult smsResult = smsNotificationService.sendPaymentGuideSms(charge, item.getGuideUrl());
                item.setSmsMessageId(smsResult.getMessageId())
                        .setSmsStatus(smsResult.getStatus())
                        .setSmsFailureReason(smsResult.getFailureReason());

                if ("SENT".equalsIgnoreCase(smsResult.getStatus())) {
                    sentSms++;
                    delivered = true;
                } else {
                    failedSms++;
                }
            }

            if (!hasWhatsapp && !hasEmail && !hasPhone) {
                skippedNoContact++;
                item.setFinalStatus("SKIPPED_NO_CONTACT")
                        .setAction("Estudante sem WhatsApp, e-mail ou telefone cadastrado.");
            } else if (delivered) {
                item.setFinalStatus("DELIVERED")
                        .setAction("Guia enviada em pelo menos um canal.");
            } else {
                item.setFinalStatus("FAILED")
                        .setAction("Nenhum canal conseguiu entregar a guia.");
            }
        }

        return response
                .setProcessedCharges(processed)
                .setSentWhatsapp(sentWhatsapp)
                .setFailedWhatsapp(failedWhatsapp)
                .setSentEmail(sentEmail)
                .setFailedEmail(failedEmail)
                .setSentSms(sentSms)
                .setFailedSms(failedSms)
                .setSkippedNoContact(skippedNoContact)
                .setSkippedNotPending(skippedNotPending)
                .setStatus("COMPLETED")
                .setMessage("Envio multicanal de guias concluído. WhatsApp usa PDF; e-mail e SMS registram link público da guia.");
    }

    private List<Charge> selectCharges(TuitionChargeGuideDeliveryRequest request) {
        Set<UUID> requestedChargeIds = request.getChargeIds() == null
                ? Set.of()
                : new HashSet<>(request.getChargeIds());

        int limit = request.getMaxItems() == null || request.getMaxItems() < 1 ? 50 : Math.min(request.getMaxItems(), 500);
        String prefix = defaultIfBlank(request.getChargeCodePrefix(), "IMT-PROPINA-");

        return chargeRepository.findAll()
                .stream()
                .filter(charge -> requestedChargeIds.isEmpty() || requestedChargeIds.contains(charge.getId()))
                .filter(charge -> belongsToInstitution(charge, request.getInstitutionId()))
                .filter(charge -> isBlank(request.getReferenceMonth()) || request.getReferenceMonth().trim().equalsIgnoreCase(safe(charge.getReferenceMonth())))
                .filter(charge -> isBlank(prefix) || safe(charge.getChargeCode()).startsWith(prefix))
                .filter(charge -> isBlank(request.getStatus()) || safeStatus(charge).equalsIgnoreCase(request.getStatus().trim()))
                .limit(limit)
                .toList();
    }

    private TuitionChargeGuideDeliveryItem baseItem(Charge charge) {
        Student student = charge.getStudent();
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;

        return new TuitionChargeGuideDeliveryItem()
                .setChargeId(charge.getId())
                .setChargeCode(charge.getChargeCode())
                .setReferenceMonth(charge.getReferenceMonth())
                .setChargeStatus(safeStatus(charge))
                .setStudentId(student != null ? student.getId() : null)
                .setStudentNumber(student != null ? student.getStudentNumber() : null)
                .setStudentName(student != null ? student.getFullName() : null)
                .setWhatsapp(student != null ? student.getWhatsapp() : null)
                .setPhone(resolvePhone(student))
                .setEmail(resolveEmail(student))
                .setGuideUrl(paymentGuideUrl(charge));
    }

    private boolean belongsToInstitution(Charge charge, UUID institutionId) {
        if (institutionId == null) {
            return true;
        }

        Student student = charge != null ? charge.getStudent() : null;
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        return institution != null && institutionId.equals(institution.getId());
    }

    private String paymentGuideUrl(Charge charge) {
        return API_BASE_URL + "/api/v1/public/payment-guides/" + charge.getChargeCode() + "/pdf";
    }

    private String resolvePhone(Student student) {
        if (student == null) return null;
        if (!isBlank(student.getPhone())) return student.getPhone();
        if (!isBlank(student.getGuardianPhone())) return student.getGuardianPhone();
        return null;
    }

    private String resolveEmail(Student student) {
        if (student == null) return null;
        if (!isBlank(student.getEmail())) return student.getEmail();
        if (!isBlank(student.getGuardianEmail())) return student.getGuardianEmail();
        return null;
    }

    private String safeStatus(Charge charge) {
        return charge != null && charge.getStatus() != null ? charge.getStatus().name() : "";
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String safe(String value) { return value == null ? "" : value; }
    private boolean isBlank(String value) { return value == null || value.trim().isBlank(); }
}
