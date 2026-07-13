package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TuitionChargeSettlementService {

    private final ChargeRepository chargeRepository;

    public TuitionChargeSettlementService(ChargeRepository chargeRepository) {
        this.chargeRepository = chargeRepository;
    }

    /**
     * Liquida a propina no mesmo lançamento que originou a guia.
     *
     * <p>Quando já existe uma cobrança aberta para o estudante e período, ela é atualizada para
     * PAID em vez de ser criada uma segunda cobrança. Se a mesma referência já estiver paga, o
     * lançamento pago é reutilizado, tornando a confirmação idempotente.</p>
     */
    @Transactional
    public Charge settleTuitionPayment(
            Student student,
            String referenceMonth,
            String description,
            LocalDate dueDate,
            BigDecimal amount,
            BigDecimal fineAmount,
            BigDecimal interestAmount,
            String currency,
            LocalDateTime paidAt
    ) {
        if (student == null || student.getId() == null) {
            throw new IllegalArgumentException("O estudante é obrigatório para liquidar a propina.");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória para liquidar a propina.");
        }

        LocalDate periodStart = dueDate.withDayOfMonth(1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        List<Charge> candidates = chargeRepository.findActiveTuitionByStudentAndPeriodForUpdate(
                student.getId(),
                periodStart,
                periodEnd
        );

        Charge canonical = candidates.stream()
                .filter(charge -> ChargeStatus.PAID.equals(charge.getStatus()))
                .findFirst()
                .orElseGet(() -> candidates.stream().findFirst().orElse(null));

        if (canonical == null) {
            canonical = new Charge()
                    .setStudent(student)
                    .setChargeCode(generateChargeCode(dueDate))
                    .setDescription(limit(defaultIfBlank(description, "Propina " + safeReference(referenceMonth)), 120))
                    .setReferenceMonth(limit(safeReference(referenceMonth), 20))
                    .setDueDate(dueDate)
                    .setAmount(money(amount))
                    .setFineAmount(money(fineAmount))
                    .setInterestAmount(money(interestAmount))
                    .setDiscountAmount(BigDecimal.ZERO)
                    .setCurrency(normalizeCurrency(currency))
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        } else if (!ChargeStatus.PAID.equals(canonical.getStatus())) {
            if (isBlank(canonical.getReferenceMonth())) {
                canonical.setReferenceMonth(limit(safeReference(referenceMonth), 20));
            }
            if (isBlank(canonical.getDescription())) {
                canonical.setDescription(limit(defaultIfBlank(description, "Propina " + safeReference(referenceMonth)), 120));
            }

            canonical
                    .setAmount(money(amount))
                    .setFineAmount(money(fineAmount))
                    .setInterestAmount(money(interestAmount))
                    .setDiscountAmount(BigDecimal.ZERO)
                    .setCurrency(normalizeCurrency(currency))
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt)
                    .setCancelledAt(null);
        }

        Charge savedCanonical = chargeRepository.save(canonical);
        cancelOpenDuplicates(candidates, savedCanonical);
        return savedCanonical;
    }

    private void cancelOpenDuplicates(List<Charge> candidates, Charge canonical) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        LocalDateTime cancelledAt = LocalDateTime.now();
        List<Charge> duplicates = new ArrayList<>();

        for (Charge candidate : candidates) {
            if (candidate == null || candidate.getId() == null || candidate.getId().equals(canonical.getId())) {
                continue;
            }
            if (!isOpen(candidate.getStatus())) {
                continue;
            }

            candidate
                    .setStatus(ChargeStatus.CANCELLED)
                    .setCancelledAt(cancelledAt);
            duplicates.add(candidate);
        }

        if (!duplicates.isEmpty()) {
            chargeRepository.saveAll(duplicates);
        }
    }

    private boolean isOpen(ChargeStatus status) {
        return ChargeStatus.PENDING.equals(status)
                || ChargeStatus.OVERDUE.equals(status)
                || ChargeStatus.PARTIALLY_PAID.equals(status);
    }

    private String generateChargeCode(LocalDate dueDate) {
        String period = dueDate.getYear() + String.format("%02d", dueDate.getMonthValue());
        String code;
        do {
            code = "IP-PROPINA-" + period + "-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase(Locale.ROOT);
        } while (chargeRepository.existsByChargeCode(code));
        return code;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String value) {
        return limit(defaultIfBlank(value, "AOA").toUpperCase(Locale.ROOT), 10);
    }

    private String safeReference(String value) {
        return defaultIfBlank(value, "Pagamento académico");
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String limit(String value, int maxLength) {
        String safe = value == null ? "" : value.trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
