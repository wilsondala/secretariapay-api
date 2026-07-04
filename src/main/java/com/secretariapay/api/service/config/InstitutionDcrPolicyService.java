package com.secretariapay.api.service.config;

import com.secretariapay.api.dto.config.DcrChargeEvaluationRequest;
import com.secretariapay.api.dto.config.DcrChargeEvaluationResponse;
import com.secretariapay.api.dto.config.InstitutionDcrPolicyResponse;
import com.secretariapay.api.entity.config.InstitutionDcrPolicy;
import com.secretariapay.api.entity.enums.config.DcrChargeStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.config.InstitutionDcrPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InstitutionDcrPolicyService {

    private final InstitutionDcrPolicyRepository policyRepository;

    public InstitutionDcrPolicyService(InstitutionDcrPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public InstitutionDcrPolicyResponse findActiveByInstitution(UUID institutionId) {
        InstitutionDcrPolicy policy = findPolicy(institutionId);
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public DcrChargeEvaluationResponse evaluate(UUID institutionId, DcrChargeEvaluationRequest request) {
        InstitutionDcrPolicy policy = findPolicy(institutionId);

        BigDecimal baseAmount = request == null || request.getBaseAmount() == null
                ? BigDecimal.ZERO
                : request.getBaseAmount();

        LocalDate dueDate = request == null || request.getDueDate() == null
                ? LocalDate.now()
                : request.getDueDate();

        LocalDate referenceDate = request == null || request.getReferenceDate() == null
                ? LocalDate.now()
                : request.getReferenceDate();

        long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueDate, referenceDate));
        int referenceDay = referenceDate.getDayOfMonth();

        DcrChargeStatus status = resolveStatus(policy, dueDate, referenceDate, daysLate, referenceDay);
        BigDecimal penaltyPercent = resolvePenaltyPercent(policy, status, referenceDay);
        BigDecimal penaltyAmount = baseAmount
                .multiply(penaltyPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(penaltyAmount).setScale(2, RoundingMode.HALF_UP);

        return new DcrChargeEvaluationResponse()
                .setInstitutionId(policy.getInstitution().getId())
                .setPolicyCode(policy.getPolicyCode())
                .setCurrency(policy.getCurrency())
                .setBaseAmount(baseAmount.setScale(2, RoundingMode.HALF_UP))
                .setPenaltyPercent(penaltyPercent.setScale(2, RoundingMode.HALF_UP))
                .setPenaltyAmount(penaltyAmount)
                .setTotalAmount(totalAmount)
                .setDueDate(dueDate)
                .setReferenceDate(referenceDate)
                .setDaysLate(daysLate)
                .setStatus(status.name())
                .setDebt(status == DcrChargeStatus.DEBT || status == DcrChargeStatus.DELINQUENT)
                .setDelinquent(status == DcrChargeStatus.DELINQUENT)
                .setCanSendWhatsappNow(isWhatsappAllowedNow(policy))
                .setProvisionalAutomaticConfirmation(policy.getProvisionalAutomaticConfirmation())
                .setManualDcrConfirmationRequired(policy.getManualDcrConfirmationRequired())
                .setDcrApprovalRole(policy.getDcrApprovalRole())
                .setMessage(resolveMessage(status, penaltyPercent));
    }

    private InstitutionDcrPolicy findPolicy(UUID institutionId) {
        return policyRepository.findByInstitutionIdAndActiveTrue(institutionId)
                .orElseThrow(() -> new NotFoundException("Política DCR ativa não encontrada para a instituição."));
    }

    private DcrChargeStatus resolveStatus(
            InstitutionDcrPolicy policy,
            LocalDate dueDate,
            LocalDate referenceDate,
            long daysLate,
            int referenceDay
    ) {
        if (daysLate >= policy.getDelinquencyAfterDays()) {
            return DcrChargeStatus.DELINQUENT;
        }

        if (daysLate >= policy.getDebtAfterDays()) {
            return DcrChargeStatus.DEBT;
        }

        if (referenceDate.isBefore(dueDate)) {
            return DcrChargeStatus.UPCOMING;
        }

        if (referenceDay >= policy.getSecondPenaltyStartDay()) {
            return DcrChargeStatus.LATE_SECOND_PENALTY;
        }

        if (referenceDay >= policy.getFirstPenaltyStartDay()) {
            return DcrChargeStatus.LATE_FIRST_PENALTY;
        }

        return DcrChargeStatus.PAYMENT_WINDOW;
    }

    private BigDecimal resolvePenaltyPercent(InstitutionDcrPolicy policy, DcrChargeStatus status, int referenceDay) {
        if (status == DcrChargeStatus.DELINQUENT || status == DcrChargeStatus.DEBT) {
            return policy.getSecondPenaltyPercent();
        }

        if (status == DcrChargeStatus.LATE_SECOND_PENALTY || referenceDay >= policy.getSecondPenaltyStartDay()) {
            return policy.getSecondPenaltyPercent();
        }

        if (status == DcrChargeStatus.LATE_FIRST_PENALTY || referenceDay >= policy.getFirstPenaltyStartDay()) {
            return policy.getFirstPenaltyPercent();
        }

        return BigDecimal.ZERO;
    }

    private boolean isWhatsappAllowedNow(InstitutionDcrPolicy policy) {
        LocalTime now = LocalTime.now();
        LocalTime start = policy.getWhatsappAllowedStart();
        LocalTime end = policy.getWhatsappAllowedEnd();

        if (start == null || end == null) {
            return true;
        }

        return !now.isBefore(start) && !now.isAfter(end);
    }

    private String resolveMessage(DcrChargeStatus status, BigDecimal penaltyPercent) {
        return switch (status) {
            case UPCOMING -> "Cobrança futura. Pode receber lembrete preventivo conforme calendário DCR.";
            case PAYMENT_WINDOW -> "Propina em período de pagamento sem multa.";
            case LATE_FIRST_PENALTY -> "Atraso de pagamento com multa DCR de " + penaltyPercent.setScale(2, RoundingMode.HALF_UP) + "% após o dia 10.";
            case LATE_SECOND_PENALTY -> "Atraso agravado com multa DCR de " + penaltyPercent.setScale(2, RoundingMode.HALF_UP) + "% após o dia 15.";
            case DEBT -> "Situação convertida em dívida após 30 dias. Requer acompanhamento da DCR.";
            case DELINQUENT -> "Inadimplência crítica após 90 dias. Requer ação institucional e relatório executivo.";
        };
    }

    private InstitutionDcrPolicyResponse toResponse(InstitutionDcrPolicy policy) {
        return new InstitutionDcrPolicyResponse()
                .setId(policy.getId())
                .setInstitutionId(policy.getInstitution().getId())
                .setPolicyCode(policy.getPolicyCode())
                .setPolicyName(policy.getPolicyName())
                .setCurrency(policy.getCurrency())
                .setPaymentWindowStartDay(policy.getPaymentWindowStartDay())
                .setNoPenaltyUntilDay(policy.getNoPenaltyUntilDay())
                .setFirstPenaltyStartDay(policy.getFirstPenaltyStartDay())
                .setFirstPenaltyPercent(policy.getFirstPenaltyPercent())
                .setSecondPenaltyStartDay(policy.getSecondPenaltyStartDay())
                .setSecondPenaltyPercent(policy.getSecondPenaltyPercent())
                .setDailyInterestEnabled(policy.getDailyInterestEnabled())
                .setDailyInterestPercent(policy.getDailyInterestPercent())
                .setDebtAfterDays(policy.getDebtAfterDays())
                .setDelinquencyAfterDays(policy.getDelinquencyAfterDays())
                .setPreDueReminderDays(policy.getPreDueReminderDays())
                .setCompulsoryReminderStartDay(policy.getCompulsoryReminderStartDay())
                .setWhatsappAllowedStart(policy.getWhatsappAllowedStart())
                .setWhatsappAllowedEnd(policy.getWhatsappAllowedEnd())
                .setProvisionalAutomaticConfirmation(policy.getProvisionalAutomaticConfirmation())
                .setManualDcrConfirmationRequired(policy.getManualDcrConfirmationRequired())
                .setDcrApprovalRole(policy.getDcrApprovalRole())
                .setOfficialEmail(policy.getOfficialEmail())
                .setCcEmail(policy.getCcEmail())
                .setActive(policy.getActive())
                .setNotes(policy.getNotes());
    }
}
