package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.TuitionChargeGeneratedItem;
import com.secretariapay.api.dto.financial.TuitionChargeGenerationRequest;
import com.secretariapay.api.dto.financial.TuitionChargeGenerationResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TuitionChargeGenerationService {

    private static final UUID IMETRO_INSTITUTION_ID = UUID.fromString("c3726494-46b5-4563-8e84-0a04334fac8c");

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final JdbcTemplate jdbcTemplate;

    public TuitionChargeGenerationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TuitionChargeGenerationResponse generate(TuitionChargeGenerationRequest request) {
        UUID institutionId = request.getInstitutionId() == null ? IMETRO_INSTITUTION_ID : request.getInstitutionId();
        LocalDate referenceDate = request.getReferenceDate() == null ? LocalDate.now() : request.getReferenceDate();
        String referenceMonth = normalizeReferenceMonth(request.getReferenceMonth(), request.getDueDate());
        String serviceCode = normalizeCode(request.getServiceCode(), "PROPINA");
        String currency = normalizeCurrency(request.getCurrency());
        BigDecimal baseAmount = money(request.getBaseAmount());
        BigDecimal discountAmount = money(request.getDiscountAmount());

        DcrPolicySnapshot policy = loadPolicy(institutionId, currency);
        BigDecimal penaltyPercent = policy.penaltyPercentFor(request.getDueDate(), referenceDate);
        BigDecimal fineAmount = baseAmount
                .multiply(penaltyPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount
                .add(fineAmount)
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor total da cobrança não pode ser negativo.");
        }

        List<Student> students = selectStudents(request, institutionId);
        TuitionChargeGenerationResponse response = new TuitionChargeGenerationResponse()
                .setInstitutionId(institutionId)
                .setAcademicYear(clean(request.getAcademicYear()))
                .setReferenceMonth(referenceMonth)
                .setDueDate(request.getDueDate())
                .setReferenceDate(referenceDate)
                .setServiceCode(serviceCode)
                .setCurrency(currency)
                .setBaseAmount(baseAmount)
                .setPenaltyPercent(penaltyPercent)
                .setFineAmountPerStudent(fineAmount)
                .setDiscountAmountPerStudent(discountAmount)
                .setTotalAmountPerStudent(totalAmount)
                .setSelectedStudents(students.size())
                .setProvisionalAutomaticConfirmation(policy.provisionalAutomaticConfirmation)
                .setManualDcrConfirmationRequired(policy.manualDcrConfirmationRequired)
                .setDcrApprovalRole(policy.dcrApprovalRole);

        int created = 0;
        int reused = 0;
        int skipped = 0;

        for (Student student : students) {
            if (student == null || isBlank(student.getStudentNumber())) {
                skipped++;
                continue;
            }

            AcademicClass academicClass = student.getAcademicClass();
            Course course = academicClass != null ? academicClass.getCourse() : null;

            String chargeCode = buildChargeCode(serviceCode, referenceMonth, student.getStudentNumber());
            Optional<Charge> existingCharge = chargeRepository.findByChargeCode(chargeCode);

            Charge charge;
            String action;

            if (existingCharge.isPresent()) {
                charge = existingCharge.get();
                reused++;
                action = "REUSED";
            } else {
                charge = new Charge()
                        .setStudent(student)
                        .setChargeCode(chargeCode)
                        .setDescription(buildDescription(request.getDescriptionPrefix(), referenceMonth, serviceCode))
                        .setReferenceMonth(referenceMonth)
                        .setDueDate(request.getDueDate())
                        .setAmount(baseAmount)
                        .setFineAmount(fineAmount)
                        .setInterestAmount(BigDecimal.ZERO)
                        .setDiscountAmount(discountAmount)
                        .setTotalAmount(totalAmount)
                        .setCurrency(currency)
                        .setStatus(ChargeStatus.PENDING);

                charge = chargeRepository.save(charge);
                created++;
                action = "CREATED";
            }

            response.getItems().add(new TuitionChargeGeneratedItem()
                    .setStudentId(student.getId())
                    .setStudentNumber(student.getStudentNumber())
                    .setStudentName(student.getFullName())
                    .setChargeId(charge.getId())
                    .setChargeCode(charge.getChargeCode())
                    .setCourseName(course != null ? course.getName() : null)
                    .setAcademicClassName(academicClass != null ? academicClass.getName() : null)
                    .setReferenceMonth(charge.getReferenceMonth())
                    .setDueDate(charge.getDueDate())
                    .setBaseAmount(charge.getAmount())
                    .setFineAmount(charge.getFineAmount())
                    .setDiscountAmount(charge.getDiscountAmount())
                    .setTotalAmount(charge.getTotalAmount())
                    .setCurrency(charge.getCurrency())
                    .setStatus(charge.getStatus() != null ? charge.getStatus().name() : null)
                    .setAction(action));
        }

        return response
                .setCreatedCharges(created)
                .setReusedCharges(reused)
                .setSkippedStudents(skipped)
                .setStatus("COMPLETED")
                .setMessage("Geração de propinas concluída com idempotência. As cobranças permanecem pendentes até validação manual da DCR.");
    }

    private List<Student> selectStudents(TuitionChargeGenerationRequest request, UUID institutionId) {
        Set<UUID> requestedStudentIds = request.getStudentIds() == null
                ? Set.of()
                : new HashSet<>(request.getStudentIds());

        return studentRepository.findAll()
                .stream()
                .filter(student -> belongsToInstitution(student, institutionId))
                .filter(student -> matchesActiveRule(student, request.getOnlyActiveStudents()))
                .filter(student -> requestedStudentIds.isEmpty() || requestedStudentIds.contains(student.getId()))
                .filter(student -> request.getAcademicClassId() == null || matchesAcademicClass(student, request.getAcademicClassId()))
                .filter(student -> request.getCourseId() == null || matchesCourse(student, request.getCourseId()))
                .filter(student -> isBlank(request.getAcademicYear()) || matchesAcademicYear(student, request.getAcademicYear()))
                .toList();
    }

    private boolean belongsToInstitution(Student student, UUID institutionId) {
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        Institution institution = course != null ? course.getInstitution() : null;

        return institution != null
                && institution.getId() != null
                && institution.getId().equals(institutionId);
    }

    private boolean matchesActiveRule(Student student, Boolean onlyActiveStudents) {
        if (!Boolean.TRUE.equals(onlyActiveStudents)) {
            return true;
        }

        return StudentStatus.ACTIVE.equals(student.getStatus());
    }

    private boolean matchesAcademicClass(Student student, UUID academicClassId) {
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        return academicClass != null && academicClassId.equals(academicClass.getId());
    }

    private boolean matchesCourse(Student student, UUID courseId) {
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        return course != null && courseId.equals(course.getId());
    }

    private boolean matchesAcademicYear(Student student, String academicYear) {
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        return academicClass != null
                && academicClass.getAcademicYear() != null
                && academicClass.getAcademicYear().equalsIgnoreCase(academicYear.trim());
    }

    private DcrPolicySnapshot loadPolicy(UUID institutionId, String fallbackCurrency) {
        try {
            return jdbcTemplate.queryForObject("""
                    select
                        currency,
                        coalesce(no_penalty_until_day, 10) as no_penalty_until_day,
                        coalesce(first_penalty_start_day, 11) as first_penalty_start_day,
                        coalesce(first_penalty_percent, 20) as first_penalty_percent,
                        coalesce(second_penalty_start_day, 15) as second_penalty_start_day,
                        coalesce(second_penalty_percent, 30) as second_penalty_percent,
                        coalesce(daily_interest_enabled, false) as daily_interest_enabled,
                        coalesce(daily_interest_percent, 0) as daily_interest_percent,
                        coalesce(debt_after_days, 30) as debt_after_days,
                        coalesce(delinquency_after_days, 90) as delinquency_after_days,
                        coalesce(provisional_automatic_confirmation, true) as provisional_automatic_confirmation,
                        coalesce(manual_dcr_confirmation_required, true) as manual_dcr_confirmation_required,
                        coalesce(dcr_approval_role, 'DCR_COORDENACAO') as dcr_approval_role
                    from institution_dcr_policies
                    where institution_id = ?
                      and active = true
                    order by updated_at desc
                    limit 1
                    """,
                    (rs, rowNum) -> new DcrPolicySnapshot()
                            .setCurrency(defaultIfBlank(rs.getString("currency"), fallbackCurrency))
                            .setNoPenaltyUntilDay(rs.getInt("no_penalty_until_day"))
                            .setFirstPenaltyStartDay(rs.getInt("first_penalty_start_day"))
                            .setFirstPenaltyPercent(rs.getBigDecimal("first_penalty_percent"))
                            .setSecondPenaltyStartDay(rs.getInt("second_penalty_start_day"))
                            .setSecondPenaltyPercent(rs.getBigDecimal("second_penalty_percent"))
                            .setDailyInterestEnabled(rs.getBoolean("daily_interest_enabled"))
                            .setDailyInterestPercent(rs.getBigDecimal("daily_interest_percent"))
                            .setDebtAfterDays(rs.getInt("debt_after_days"))
                            .setDelinquencyAfterDays(rs.getInt("delinquency_after_days"))
                            .setProvisionalAutomaticConfirmation(rs.getBoolean("provisional_automatic_confirmation"))
                            .setManualDcrConfirmationRequired(rs.getBoolean("manual_dcr_confirmation_required"))
                            .setDcrApprovalRole(defaultIfBlank(rs.getString("dcr_approval_role"), "DCR_COORDENACAO")),
                    institutionId
            );
        } catch (Exception ignored) {
            return DcrPolicySnapshot.defaults(fallbackCurrency);
        }
    }

    private String buildChargeCode(String serviceCode, String referenceMonth, String studentNumber) {
        String service = abbreviate(normalizeCode(serviceCode, "PROPINA"), 8);
        String period = normalizeCode(referenceMonth, "MES");
        String student = normalizeCode(studentNumber, "ALUNO");

        String code = "IMT-" + service + "-" + period + "-" + student;

        if (code.length() <= 60) {
            return code;
        }

        return code.substring(0, 60);
    }

    private String buildDescription(String prefix, String referenceMonth, String serviceCode) {
        String cleanPrefix = defaultIfBlank(prefix, "Propina");
        String description = cleanPrefix + " " + referenceMonth + " - " + serviceCode;

        if (description.length() <= 120) {
            return description;
        }

        return description.substring(0, 120);
    }

    private String normalizeReferenceMonth(String referenceMonth, LocalDate dueDate) {
        if (!isBlank(referenceMonth)) {
            return referenceMonth.trim();
        }

        return dueDate.getYear() + "-" + String.format("%02d", dueDate.getMonthValue());
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        return defaultIfBlank(currency, "AOA").trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCode(String value, String fallback) {
        String clean = defaultIfBlank(value, fallback);
        String normalized = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        return normalized.isBlank() ? fallback : normalized;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static class DcrPolicySnapshot {
        private String currency = "AOA";
        private int noPenaltyUntilDay = 10;
        private int firstPenaltyStartDay = 11;
        private BigDecimal firstPenaltyPercent = BigDecimal.valueOf(20);
        private int secondPenaltyStartDay = 15;
        private BigDecimal secondPenaltyPercent = BigDecimal.valueOf(30);
        private boolean dailyInterestEnabled = false;
        private BigDecimal dailyInterestPercent = BigDecimal.ZERO;
        private int debtAfterDays = 30;
        private int delinquencyAfterDays = 90;
        private boolean provisionalAutomaticConfirmation = true;
        private boolean manualDcrConfirmationRequired = true;
        private String dcrApprovalRole = "DCR_COORDENACAO";

        private static DcrPolicySnapshot defaults(String currency) {
            DcrPolicySnapshot snapshot = new DcrPolicySnapshot();
            snapshot.currency = currency == null || currency.isBlank() ? "AOA" : currency;
            return snapshot;
        }

        private BigDecimal penaltyPercentFor(LocalDate dueDate, LocalDate referenceDate) {
            if (dueDate == null || referenceDate == null || referenceDate.isBefore(dueDate)) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueDate, referenceDate));
            int dayOfMonth = referenceDate.getDayOfMonth();

            BigDecimal percent = BigDecimal.ZERO;

            if (dayOfMonth >= secondPenaltyStartDay) {
                percent = secondPenaltyPercent;
            } else if (dayOfMonth >= firstPenaltyStartDay || dayOfMonth > noPenaltyUntilDay) {
                percent = firstPenaltyPercent;
            }

            if (dailyInterestEnabled && daysLate > 0 && dailyInterestPercent != null) {
                percent = percent.add(dailyInterestPercent.multiply(BigDecimal.valueOf(daysLate)));
            }

            return percent.setScale(2, RoundingMode.HALF_UP);
        }

        private DcrPolicySnapshot setCurrency(String currency) { this.currency = currency; return this; }
        private DcrPolicySnapshot setNoPenaltyUntilDay(int noPenaltyUntilDay) { this.noPenaltyUntilDay = noPenaltyUntilDay; return this; }
        private DcrPolicySnapshot setFirstPenaltyStartDay(int firstPenaltyStartDay) { this.firstPenaltyStartDay = firstPenaltyStartDay; return this; }
        private DcrPolicySnapshot setFirstPenaltyPercent(BigDecimal firstPenaltyPercent) { this.firstPenaltyPercent = firstPenaltyPercent == null ? BigDecimal.ZERO : firstPenaltyPercent; return this; }
        private DcrPolicySnapshot setSecondPenaltyStartDay(int secondPenaltyStartDay) { this.secondPenaltyStartDay = secondPenaltyStartDay; return this; }
        private DcrPolicySnapshot setSecondPenaltyPercent(BigDecimal secondPenaltyPercent) { this.secondPenaltyPercent = secondPenaltyPercent == null ? BigDecimal.ZERO : secondPenaltyPercent; return this; }
        private DcrPolicySnapshot setDailyInterestEnabled(boolean dailyInterestEnabled) { this.dailyInterestEnabled = dailyInterestEnabled; return this; }
        private DcrPolicySnapshot setDailyInterestPercent(BigDecimal dailyInterestPercent) { this.dailyInterestPercent = dailyInterestPercent == null ? BigDecimal.ZERO : dailyInterestPercent; return this; }
        private DcrPolicySnapshot setDebtAfterDays(int debtAfterDays) { this.debtAfterDays = debtAfterDays; return this; }
        private DcrPolicySnapshot setDelinquencyAfterDays(int delinquencyAfterDays) { this.delinquencyAfterDays = delinquencyAfterDays; return this; }
        private DcrPolicySnapshot setProvisionalAutomaticConfirmation(boolean provisionalAutomaticConfirmation) { this.provisionalAutomaticConfirmation = provisionalAutomaticConfirmation; return this; }
        private DcrPolicySnapshot setManualDcrConfirmationRequired(boolean manualDcrConfirmationRequired) { this.manualDcrConfirmationRequired = manualDcrConfirmationRequired; return this; }
        private DcrPolicySnapshot setDcrApprovalRole(String dcrApprovalRole) { this.dcrApprovalRole = dcrApprovalRole; return this; }
    }
}
