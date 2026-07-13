package com.secretariapay.api.service.operations;

import com.secretariapay.api.dto.operations.GenerateMonthlyChargesRequest;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MonthlyChargeGenerationService {

    private static final List<Integer> DEFAULT_PAYABLE_MONTHS = List.of(1, 2, 3, 4, 5, 6, 7, 9, 10, 11);
    private static final List<Integer> NON_PAYABLE_MONTHS = List.of(8, 12);

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final AuditService auditService;
    private final BigDecimal defaultMonthlyAmount;
    private final int defaultYear;
    private final int defaultDueDay;

    public MonthlyChargeGenerationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            AuditService auditService,
            @Value("${secretariapay.charges.default-monthly-amount:5000}") BigDecimal defaultMonthlyAmount,
            @Value("${secretariapay.charges.default-academic-year:2026}") int defaultYear,
            @Value("${secretariapay.charges.default-due-day:10}") int defaultDueDay
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.auditService = auditService;
        this.defaultMonthlyAmount = defaultMonthlyAmount;
        this.defaultYear = defaultYear;
        this.defaultDueDay = defaultDueDay;
    }

    @Transactional
    public Map<String, Object> generate(GenerateMonthlyChargesRequest request, String actor) {
        int year = request != null && request.getYear() != null ? request.getYear() : defaultYear;
        int dueDay = normalizeDueDay(request != null && request.getDueDay() != null ? request.getDueDay() : defaultDueDay);
        BigDecimal amount = normalizeAmount(request != null && request.getMonthlyAmount() != null ? request.getMonthlyAmount() : defaultMonthlyAmount);
        boolean dryRun = request != null && Boolean.TRUE.equals(request.getDryRun());
        List<Integer> months = normalizeMonths(request == null ? null : request.getMonths());
        List<Student> students = studentRepository.findByStatusOrderByFullNameAsc(StudentStatus.ACTIVE);

        int created = 0;
        int skippedExisting = 0;
        int skippedNonPayable = 0;
        List<Map<String, Object>> createdItems = new ArrayList<>();
        List<Map<String, Object>> skippedItems = new ArrayList<>();

        for (Student student : students) {
            for (Integer month : months) {
                if (NON_PAYABLE_MONTHS.contains(month)) {
                    skippedNonPayable++;
                    skippedItems.add(item(student, month, year, dueDay, "SKIPPED_NON_PAYABLE", null));
                    continue;
                }

                String referenceMonth = monthLabel(month) + "/" + year;
                String chargeCode = buildChargeCode(student, year, month);
                LocalDate periodStart = LocalDate.of(year, month, 1);
                LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
                boolean exists = chargeRepository.existsActiveTuitionByStudentAndPeriod(
                                student.getId(),
                                periodStart,
                                periodEnd
                        ) || chargeRepository.existsByStudentIdAndReferenceMonthIgnoreCase(student.getId(), referenceMonth)
                        || chargeRepository.existsByChargeCode(chargeCode);

                if (exists) {
                    skippedExisting++;
                    skippedItems.add(item(student, month, year, dueDay, "SKIPPED_EXISTING", chargeCode));
                    continue;
                }

                if (!dryRun) {
                    Charge charge = new Charge()
                            .setStudent(student)
                            .setChargeCode(chargeCode)
                            .setDescription("Propina " + referenceMonth)
                            .setReferenceMonth(referenceMonth)
                            .setDueDate(LocalDate.of(year, month, dueDay))
                            .setAmount(amount)
                            .setFineAmount(BigDecimal.ZERO)
                            .setInterestAmount(BigDecimal.ZERO)
                            .setDiscountAmount(BigDecimal.ZERO)
                            .setCurrency("AOA")
                            .setStatus(ChargeStatus.PENDING);
                    chargeRepository.save(charge);
                }

                created++;
                createdItems.add(item(student, month, year, dueDay, dryRun ? "PREPARED" : "CREATED", chargeCode));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("actor", clean(actor));
        response.put("year", year);
        response.put("dueDay", dueDay);
        response.put("monthlyAmount", amount);
        response.put("currency", "AOA");
        response.put("months", months);
        response.put("ignoredMonths", NON_PAYABLE_MONTHS);
        response.put("activeStudents", students.size());
        response.put("createdOrPrepared", created);
        response.put("skippedExisting", skippedExisting);
        response.put("skippedNonPayable", skippedNonPayable);
        response.put("dryRun", dryRun);
        response.put("createdItems", createdItems.stream().limit(50).toList());
        response.put("skippedItems", skippedItems.stream().limit(50).toList());

        auditService.record(clean(actor), dryRun ? "MONTHLY_CHARGES_PREPARED" : "MONTHLY_CHARGES_GENERATED", "Charge", String.valueOf(year), response.toString());
        return response;
    }

    private List<Integer> normalizeMonths(List<Integer> months) {
        if (months == null || months.isEmpty()) {
            return DEFAULT_PAYABLE_MONTHS;
        }
        return months.stream()
                .filter(month -> month != null && month >= 1 && month <= 12)
                .distinct()
                .sorted()
                .toList();
    }

    private int normalizeDueDay(int dueDay) {
        if (dueDay < 1) return 1;
        if (dueDay > 28) return 28;
        return dueDay;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildChargeCode(Student student, int year, int month) {
        String studentNumber = onlyAlphaNumeric(student.getStudentNumber());
        String base = "IMT-PROPINA-" + year + "-" + String.format("%02d", month) + "-" + studentNumber;
        if (base.length() <= 60) {
            return base;
        }
        return base.substring(0, 60);
    }

    private Map<String, Object> item(Student student, int month, int year, int dueDay, String status, String chargeCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("studentId", student.getId());
        item.put("studentNumber", student.getStudentNumber());
        item.put("studentName", student.getFullName());
        item.put("referenceMonth", monthLabel(month) + "/" + year);
        item.put("dueDate", NON_PAYABLE_MONTHS.contains(month) ? null : LocalDate.of(year, month, dueDay));
        item.put("chargeCode", chargeCode);
        item.put("status", status);
        return item;
    }

    private String monthLabel(int month) {
        return switch (month) {
            case 1 -> "Janeiro";
            case 2 -> "Fevereiro";
            case 3 -> "Março";
            case 4 -> "Abril";
            case 5 -> "Maio";
            case 6 -> "Junho";
            case 7 -> "Julho";
            case 8 -> "Agosto";
            case 9 -> "Setembro";
            case 10 -> "Outubro";
            case 11 -> "Novembro";
            case 12 -> "Dezembro";
            default -> "Mês " + month;
        };
    }

    private String onlyAlphaNumeric(String value) {
        String text = value == null || value.isBlank() ? "SEMNUMERO" : value;
        return text.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "SYSTEM" : value.trim();
    }
}
