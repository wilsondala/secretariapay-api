package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TuitionChargeSettlementServiceTest {

    @Mock
    private ChargeRepository chargeRepository;

    private TuitionChargeSettlementService service;
    private Student student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        service = new TuitionChargeSettlementService(chargeRepository);
        studentId = UUID.randomUUID();
        student = new Student();
        ReflectionTestUtils.setField(student, "id", studentId);
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSettleExistingOpenTuitionWithoutCreatingAnotherCharge() {
        Charge pending = charge(UUID.randomUUID(), ChargeStatus.PENDING, "IMT-PROPINA-2026-07-ALUNO", "Julho/2026");
        when(chargeRepository.findActiveTuitionByStudentAndPeriodForUpdate(
                studentId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(pending));

        Charge settled = service.settleTuitionPayment(
                student,
                "Julho/2026",
                "Propina Julho/2026",
                LocalDate.of(2026, 7, 10),
                new BigDecimal("45000.00"),
                new BigDecimal("9000.00"),
                BigDecimal.ZERO,
                "AOA",
                LocalDateTime.of(2026, 7, 12, 14, 30)
        );

        assertThat(settled).isSameAs(pending);
        assertThat(settled.getStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(settled.getChargeCode()).isEqualTo("IMT-PROPINA-2026-07-ALUNO");
        assertThat(settled.getAmount()).isEqualByComparingTo("45000.00");
        assertThat(settled.getFineAmount()).isEqualByComparingTo("9000.00");
        assertThat(settled.getTotalAmount()).isEqualByComparingTo("54000.00");
        assertThat(settled.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 7, 12, 14, 30));
        verify(chargeRepository, never()).existsByChargeCode(any());
        verify(chargeRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldReusePaidTuitionAndCancelOnlyOpenDuplicate() {
        Charge paid = charge(UUID.randomUUID(), ChargeStatus.PAID, "IP-PROPINA-202607-PAGO", "Julho/2026");
        paid.setPaidAt(LocalDateTime.of(2026, 7, 11, 9, 0));
        Charge duplicate = charge(UUID.randomUUID(), ChargeStatus.OVERDUE, "IMT-PROPINA-2026-07-DUP", "2026-07");

        when(chargeRepository.findActiveTuitionByStudentAndPeriodForUpdate(
                studentId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(duplicate, paid));

        Charge settled = service.settleTuitionPayment(
                student,
                "Julho/2026",
                "Propina Julho/2026",
                LocalDate.of(2026, 7, 10),
                new BigDecimal("45000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "AOA",
                LocalDateTime.of(2026, 7, 12, 15, 0)
        );

        assertThat(settled).isSameAs(paid);
        assertThat(settled.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 7, 11, 9, 0));
        assertThat(duplicate.getStatus()).isEqualTo(ChargeStatus.CANCELLED);
        assertThat(duplicate.getCancelledAt()).isNotNull();

        ArgumentCaptor<List<Charge>> duplicatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(chargeRepository).saveAll(duplicatesCaptor.capture());
        assertThat(duplicatesCaptor.getValue()).containsExactly(duplicate);
    }

    @Test
    void shouldCreatePaidTuitionOnlyWhenPeriodHasNoCharge() {
        when(chargeRepository.findActiveTuitionByStudentAndPeriodForUpdate(
                studentId,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        )).thenReturn(List.of());
        when(chargeRepository.existsByChargeCode(any())).thenReturn(false);

        Charge settled = service.settleTuitionPayment(
                student,
                "Setembro/2026",
                "Propina Setembro/2026",
                LocalDate.of(2026, 9, 10),
                new BigDecimal("45000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "AOA",
                LocalDateTime.of(2026, 9, 8, 10, 0)
        );

        assertThat(settled.getStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(settled.getStudent()).isSameAs(student);
        assertThat(settled.getChargeCode()).startsWith("IP-PROPINA-202609-");
        assertThat(settled.getReferenceMonth()).isEqualTo("Setembro/2026");
        assertThat(settled.getTotalAmount()).isEqualByComparingTo("45000.00");
        verify(chargeRepository, never()).saveAll(anyList());
    }

    private Charge charge(UUID id, ChargeStatus status, String code, String referenceMonth) {
        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(code)
                .setDescription("Propina " + referenceMonth)
                .setReferenceMonth(referenceMonth)
                .setDueDate(LocalDate.of(2026, 7, 10))
                .setAmount(new BigDecimal("45000.00"))
                .setFineAmount(BigDecimal.ZERO)
                .setInterestAmount(BigDecimal.ZERO)
                .setDiscountAmount(BigDecimal.ZERO)
                .setCurrency("AOA")
                .setStatus(status);
        ReflectionTestUtils.setField(charge, "id", id);
        return charge;
    }
}
