package com.secretariapay.api.service.dashboard;

import com.secretariapay.api.dto.dashboard.FinancialDashboardResponse;
import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicBlockRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinancialDashboardService {

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final AcademicBlockRepository academicBlockRepository;

    public FinancialDashboardService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            PaymentProofRepository paymentProofRepository,
            AcademicBlockRepository academicBlockRepository
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.paymentProofRepository = paymentProofRepository;
        this.academicBlockRepository = academicBlockRepository;
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getSummary() {
        List<Charge> charges = chargeRepository.findAll();
        LocalDate today = LocalDate.now();

        BigDecimal expectedRevenue = sumCharges(charges);
        BigDecimal receivedRevenue = sumChargesByStatus(charges, ChargeStatus.PAID);
        BigDecimal pendingRevenue = sumChargesByStatus(charges, ChargeStatus.PENDING);
        BigDecimal overdueRevenue = sumOverdueCharges(charges, today);

        long pendingCharges = countByStatus(charges, ChargeStatus.PENDING);
        long paidCharges = countByStatus(charges, ChargeStatus.PAID);
        long overdueCharges = charges.stream()
                .filter(charge -> ChargeStatus.OVERDUE.equals(charge.getStatus())
                        || (ChargeStatus.PENDING.equals(charge.getStatus())
                        && charge.getDueDate() != null
                        && charge.getDueDate().isBefore(today)))
                .count();

        return new FinancialDashboardResponse()
                .setTotalStudents(studentRepository.count())
                .setBlockedStudents((long) studentRepository.findByFinanciallyBlockedTrueOrderByFullNameAsc().size())
                .setPendingCharges(pendingCharges)
                .setPaidCharges(paidCharges)
                .setOverdueCharges(overdueCharges)
                .setPendingPaymentProofs((long) paymentProofRepository.findByStatusOrderBySubmittedAtAsc(PaymentProofStatus.PENDING_REVIEW).size())
                .setActiveAcademicBlocks((long) academicBlockRepository.findByStatusOrderByBlockedAtDesc(AcademicBlockStatus.ACTIVE).size())
                .setExpectedRevenue(expectedRevenue)
                .setReceivedRevenue(receivedRevenue)
                .setPendingRevenue(pendingRevenue)
                .setOverdueRevenue(overdueRevenue)
                .setCurrency(resolveCurrency(charges))
                .setGeneratedAt(LocalDateTime.now());
    }

    private long countByStatus(List<Charge> charges, ChargeStatus status) {
        return charges.stream()
                .filter(charge -> status.equals(charge.getStatus()))
                .count();
    }

    private BigDecimal sumCharges(List<Charge> charges) {
        return charges.stream()
                .map(this::safeTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumChargesByStatus(List<Charge> charges, ChargeStatus status) {
        return charges.stream()
                .filter(charge -> status.equals(charge.getStatus()))
                .map(this::safeTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumOverdueCharges(List<Charge> charges, LocalDate today) {
        return charges.stream()
                .filter(charge -> ChargeStatus.OVERDUE.equals(charge.getStatus())
                        || (ChargeStatus.PENDING.equals(charge.getStatus())
                        && charge.getDueDate() != null
                        && charge.getDueDate().isBefore(today)))
                .map(this::safeTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safeTotalAmount(Charge charge) {
        if (charge.getTotalAmount() != null) {
            return charge.getTotalAmount();
        }

        if (charge.getAmount() != null) {
            return charge.getAmount();
        }

        return BigDecimal.ZERO;
    }

    private String resolveCurrency(List<Charge> charges) {
        return charges.stream()
                .map(Charge::getCurrency)
                .filter(currency -> currency != null && !currency.isBlank())
                .findFirst()
                .orElse("AOA");
    }
}
