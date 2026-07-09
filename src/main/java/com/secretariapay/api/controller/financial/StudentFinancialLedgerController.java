package com.secretariapay.api.controller.financial;

import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/financial-ledger")
public class StudentFinancialLedgerController {

    private static final List<MonthItem> MONTHS = List.of(
            new MonthItem(1, "Janeiro/2026", "Janeiro", true),
            new MonthItem(2, "Fevereiro/2026", "Fevereiro", true),
            new MonthItem(3, "Março/2026", "Março", true),
            new MonthItem(4, "Abril/2026", "Abril", true),
            new MonthItem(5, "Maio/2026", "Maio", true),
            new MonthItem(6, "Junho/2026", "Junho", true),
            new MonthItem(7, "Julho/2026", "Julho", true),
            new MonthItem(8, "Agosto/2026", "Agosto", false),
            new MonthItem(9, "Setembro/2026", "Setembro", true),
            new MonthItem(10, "Outubro/2026", "Outubro", true),
            new MonthItem(11, "Novembro/2026", "Novembro", true),
            new MonthItem(12, "Dezembro/2026", "Dezembro", false)
    );

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;

    public StudentFinancialLedgerController(StudentRepository studentRepository, ChargeRepository chargeRepository) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> ledger(@PathVariable UUID studentId) {
        if (studentRepository.findById(studentId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var student = studentRepository.findById(studentId).get();
        List<Charge> charges = chargeRepository.findByStudentIdOrderByDueDateDesc(studentId);
        List<Map<String, Object>> months = MONTHS.stream().map(month -> monthView(month, charges)).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("studentId", student.getId());
        body.put("studentName", student.getFullName());
        body.put("studentNumber", student.getStudentNumber());
        body.put("months", months);
        body.put("generatedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> monthView(MonthItem month, List<Charge> charges) {
        Charge charge = charges.stream()
                .filter(item -> matches(item.getReferenceMonth(), month))
                .filter(item -> item.getStatus() != null && "PAID".equalsIgnoreCase(item.getStatus().name()))
                .findFirst()
                .orElseGet(() -> charges.stream().filter(item -> matches(item.getReferenceMonth(), month)).findFirst().orElse(null));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("monthNumber", month.number());
        item.put("month", month.label());
        item.put("payable", month.payable());

        if (charge == null) {
            item.put("status", month.payable() ? "NOT_ISSUED" : "NOT_APPLICABLE");
            item.put("statusLabel", month.payable() ? "Não pago / não emitido" : "Não letivo");
            item.put("baseAmount", BigDecimal.ZERO);
            item.put("fineAmount", BigDecimal.ZERO);
            item.put("interestAmount", BigDecimal.ZERO);
            item.put("totalAmount", BigDecimal.ZERO);
            item.put("paidAt", null);
            return item;
        }

        item.put("chargeId", charge.getId());
        item.put("chargeCode", charge.getChargeCode());
        item.put("status", charge.getStatus());
        item.put("statusLabel", charge.getStatus() == null ? "Pendente" : charge.getStatus().name());
        item.put("dueDate", charge.getDueDate());
        item.put("baseAmount", charge.getAmount());
        item.put("fineAmount", charge.getFineAmount());
        item.put("interestAmount", charge.getInterestAmount());
        item.put("discountAmount", charge.getDiscountAmount());
        item.put("totalAmount", charge.getTotalAmount());
        item.put("paidAt", charge.getPaidAt());
        return item;
    }

    private boolean matches(String referenceMonth, MonthItem month) {
        String value = normalize(referenceMonth);
        return value.contains(normalize(month.label())) || value.contains(normalize(month.shortName()));
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private record MonthItem(int number, String label, String shortName, boolean payable) {}
}
