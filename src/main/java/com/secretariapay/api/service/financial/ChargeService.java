package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ChargeRequest;
import com.secretariapay.api.dto.financial.ChargeResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChargeService {

    private final ChargeRepository chargeRepository;
    private final StudentRepository studentRepository;
    private final TuitionChargeSettlementService tuitionChargeSettlementService;
    private final ChargeClassificationService classificationService;

    public ChargeService(
            ChargeRepository chargeRepository,
            StudentRepository studentRepository,
            TuitionChargeSettlementService tuitionChargeSettlementService,
            ChargeClassificationService classificationService
    ) {
        this.chargeRepository = chargeRepository;
        this.studentRepository = studentRepository;
        this.tuitionChargeSettlementService = tuitionChargeSettlementService;
        this.classificationService = classificationService;
    }

    @Transactional
    public ChargeResponse create(ChargeRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));

        ChargeCategory category = request.getChargeCategory() != null
                ? request.getChargeCategory()
                : classificationService.resolveCategory(request.getServiceCode(), request.getDescription(), request.getReferenceMonth(), null);
        String serviceCode = request.getServiceCode() == null || request.getServiceCode().isBlank()
                ? classificationService.resolveServiceCode(request.getDescription(), request.getReferenceMonth(), null)
                : request.getServiceCode().trim().toUpperCase(Locale.ROOT);

        preventDuplicateTuition(student, request, category);

        BigDecimal fineAmount = valueOrZero(request.getFineAmount());
        BigDecimal interestAmount = valueOrZero(request.getInterestAmount());
        BigDecimal discountAmount = valueOrZero(request.getDiscountAmount());
        BigDecimal totalAmount = request.getAmount()
                .add(fineAmount)
                .add(interestAmount)
                .subtract(discountAmount);

        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(generateChargeCode(category, serviceCode))
                .setDescription(request.getDescription())
                .setReferenceMonth(request.getReferenceMonth())
                .setChargeCategory(category)
                .setServiceCode(serviceCode)
                .setDueDate(request.getDueDate())
                .setAmount(request.getAmount())
                .setFineAmount(fineAmount)
                .setInterestAmount(interestAmount)
                .setDiscountAmount(discountAmount)
                .setTotalAmount(totalAmount)
                .setCurrency(normalizeCurrency(request.getCurrency()))
                .setStatus(ChargeStatus.PENDING);

        return toResponse(chargeRepository.save(charge));
    }

    @Transactional(readOnly = true)
    public List<ChargeResponse> findAll() {
        return chargeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ChargeResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public ChargeResponse findByCode(String chargeCode) {
        return toResponse(chargeRepository.findByChargeCode(chargeCode)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada.")));
    }

    @Transactional(readOnly = true)
    public List<ChargeResponse> findByStudent(UUID studentId) {
        return chargeRepository.findByStudentIdOrderByDueDateDesc(studentId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargeResponse> findByStatus(ChargeStatus status) {
        return chargeRepository.findByStatusOrderByDueDateAsc(status).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<ChargeResponse> markOverdueCharges() {
        List<Charge> overdueCharges = chargeRepository.findByDueDateBeforeAndStatusOrderByDueDateAsc(LocalDate.now(), ChargeStatus.PENDING);
        overdueCharges.forEach(charge -> charge.setStatus(ChargeStatus.OVERDUE));
        return chargeRepository.saveAll(overdueCharges).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ChargeResponse confirmPayment(UUID id) {
        Charge charge = findEntityById(id);
        if (charge.getStatus() == ChargeStatus.PAID) return toResponse(charge);
        if (charge.getStatus() == ChargeStatus.CANCELLED) {
            throw new IllegalArgumentException("Não é possível confirmar uma cobrança cancelada.");
        }

        if (classificationService.isTuition(charge)) {
            Charge settled = tuitionChargeSettlementService.settleTuitionPayment(
                    charge.getStudent(),
                    charge.getReferenceMonth(),
                    charge.getDescription(),
                    charge.getDueDate(),
                    charge.getAmount(),
                    charge.getFineAmount(),
                    charge.getInterestAmount(),
                    charge.getCurrency(),
                    LocalDateTime.now()
            );
            settled.setChargeCategory(ChargeCategory.TUITION).setServiceCode("TUITION");
            return toResponse(chargeRepository.save(settled));
        }

        charge.setStatus(ChargeStatus.PAID).setPaidAt(LocalDateTime.now());
        classificationService.classify(charge);
        return toResponse(chargeRepository.save(charge));
    }

    @Transactional
    public ChargeResponse cancel(UUID id) {
        Charge charge = findEntityById(id);
        if (charge.getStatus() == ChargeStatus.PAID) {
            throw new IllegalArgumentException("Não é possível cancelar uma cobrança já paga.");
        }
        charge.setStatus(ChargeStatus.CANCELLED).setCancelledAt(LocalDateTime.now());
        return toResponse(chargeRepository.save(charge));
    }

    public ChargeResponse toResponse(Charge charge) {
        Student student = charge.getStudent();
        AcademicClass academicClass = student != null ? student.getAcademicClass() : null;
        Course course = academicClass != null ? academicClass.getCourse() : null;
        ChargeCategory category = classificationService.resolveCategory(charge);
        String serviceCode = classificationService.resolveServiceCode(charge);

        return new ChargeResponse()
                .setId(charge.getId())
                .setChargeCode(charge.getChargeCode())
                .setStudentId(student != null ? student.getId() : null)
                .setStudentNumber(student != null ? student.getStudentNumber() : null)
                .setStudentName(student != null ? student.getFullName() : null)
                .setAcademicClassId(academicClass != null ? academicClass.getId() : null)
                .setAcademicClassName(academicClass != null ? academicClass.getName() : null)
                .setCourseId(course != null ? course.getId() : null)
                .setCourseName(course != null ? course.getName() : null)
                .setDescription(charge.getDescription())
                .setReferenceMonth(charge.getReferenceMonth())
                .setChargeCategory(category)
                .setServiceCode(serviceCode)
                .setDueDate(charge.getDueDate())
                .setAmount(charge.getAmount())
                .setFineAmount(charge.getFineAmount())
                .setInterestAmount(charge.getInterestAmount())
                .setDiscountAmount(charge.getDiscountAmount())
                .setTotalAmount(charge.getTotalAmount())
                .setCurrency(charge.getCurrency())
                .setStatus(charge.getStatus())
                .setPaidAt(charge.getPaidAt())
                .setCancelledAt(charge.getCancelledAt())
                .setCreatedAt(charge.getCreatedAt())
                .setUpdatedAt(charge.getUpdatedAt());
    }

    private void preventDuplicateTuition(Student student, ChargeRequest request, ChargeCategory category) {
        if (category != ChargeCategory.TUITION) return;
        LocalDate periodStart = request.getDueDate().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        if (chargeRepository.existsActiveTuitionByStudentAndPeriod(student.getId(), periodStart, periodEnd)) {
            throw new IllegalArgumentException("Já existe uma propina registada para este estudante no período "
                    + periodStart.getMonthValue() + "/" + periodStart.getYear() + ".");
        }
    }

    private Charge findEntityById(UUID id) {
        return chargeRepository.findById(id).orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
    }

    private String generateChargeCode(ChargeCategory category, String serviceCode) {
        String prefix = category == ChargeCategory.TUITION ? "IMT-PROPINA" : "IMT-SERVICO";
        String safeService = serviceCode == null ? "OTHER" : serviceCode.replaceAll("[^A-Z0-9]+", "-");
        String code;
        do {
            code = prefix + "-" + safeService + "-" + System.currentTimeMillis();
        } while (chargeRepository.existsByChargeCode(code));
        return code.length() <= 60 ? code : code.substring(0, 60);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "AOA" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
