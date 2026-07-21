package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ChargeRequest;
import com.secretariapay.api.dto.financial.ChargeResponse;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.academic.AcademicServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeServiceTest {

    @Mock private ChargeRepository chargeRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TuitionChargeSettlementService tuitionChargeSettlementService;
    @Mock private AcademicServiceOrderService academicServiceOrderService;
    @Mock private ReceiptService receiptService;
    @Mock private ReceiptDeliveryService receiptDeliveryService;

    private ChargeService service;
    private Student student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        service = new ChargeService(
                chargeRepository,
                studentRepository,
                tuitionChargeSettlementService,
                new ChargeClassificationService(),
                academicServiceOrderService,
                receiptService,
                receiptDeliveryService
        );
        studentId = UUID.randomUUID();
        student = new Student();
        ReflectionTestUtils.setField(student, "id", studentId);
    }

    @Test
    void shouldRejectManualDuplicateTuitionForSameStudentAndPeriod() {
        ChargeRequest request = new ChargeRequest()
                .setStudentId(studentId)
                .setDescription("Propina Julho/2026")
                .setReferenceMonth("Julho/2026")
                .setDueDate(LocalDate.of(2026, 7, 10))
                .setAmount(new BigDecimal("45000.00"))
                .setCurrency("AOA");

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(chargeRepository.existsActiveTuitionByStudentAndPeriod(
                studentId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe uma propina registada")
                .hasMessageContaining("7/2026");

        verify(chargeRepository, never()).save(any(Charge.class));
        verify(chargeRepository, never()).existsByChargeCode(any());
    }

    @Test
    void shouldUseCanonicalSettlementWhenConfirmingTuitionManually() {
        UUID chargeId = UUID.randomUUID();
        Charge pending = tuitionCharge(chargeId, ChargeStatus.PENDING);
        Charge paid = tuitionCharge(chargeId, ChargeStatus.PAID)
                .setPaidAt(LocalDateTime.of(2026, 7, 12, 16, 0));

        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(pending));
        when(tuitionChargeSettlementService.settleTuitionPayment(
                eq(student), eq("Julho/2026"), eq("Propina Julho/2026"),
                eq(LocalDate.of(2026, 7, 10)), eq(new BigDecimal("45000.00")),
                eq(BigDecimal.ZERO), eq(BigDecimal.ZERO), eq("AOA"), any(LocalDateTime.class)
        )).thenReturn(paid);
        when(chargeRepository.save(paid)).thenReturn(paid);
        ReceiptResponse receipt = new ReceiptResponse().setReceiptCode("RCT-TUITION");
        when(receiptService.issueOrFindForCharge(chargeId)).thenReturn(receipt);

        ChargeResponse response = service.confirmPayment(chargeId);

        assertThat(response.getId()).isEqualTo(chargeId);
        assertThat(response.getStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(response.getChargeCategory()).isEqualTo(ChargeCategory.TUITION);
        assertThat(response.getServiceCode()).isEqualTo("TUITION");
        assertThat(response.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 7, 12, 16, 0));
        verify(chargeRepository).save(paid);
        verify(academicServiceOrderService).confirmPaymentByCharge(paid);
        verify(receiptService).issueOrFindForCharge(chargeId);
        verify(receiptDeliveryService).sendAfterDcrApproval(paid, receipt, "");
    }

    @Test
    void shouldKeepExistingConfirmationFlowForAcademicService() {
        UUID chargeId = UUID.randomUUID();
        Charge declaration = new Charge()
                .setStudent(student)
                .setChargeCode("IMT-SERVICO-DECLARACAO-2026")
                .setDescription("Declaração de frequência")
                .setReferenceMonth("Declaração 2026")
                .setDueDate(LocalDate.of(2026, 7, 15))
                .setAmount(new BigDecimal("4400.00"))
                .setCurrency("AOA")
                .setStatus(ChargeStatus.PENDING);
        ReflectionTestUtils.setField(declaration, "id", chargeId);

        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(declaration));
        when(chargeRepository.save(declaration)).thenReturn(declaration);
        ReceiptResponse receipt = new ReceiptResponse().setReceiptCode("RCT-SERVICE");
        when(receiptService.issueOrFindForCharge(chargeId)).thenReturn(receipt);

        ChargeResponse response = service.confirmPayment(chargeId);

        assertThat(response.getStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(response.getPaidAt()).isNotNull();
        assertThat(response.getChargeCategory()).isEqualTo(ChargeCategory.ACADEMIC_SERVICE);
        assertThat(response.getServiceCode()).isEqualTo("DECLARATION_WITHOUT_GRADES");
        verify(chargeRepository).save(declaration);
        verify(academicServiceOrderService).confirmPaymentByCharge(declaration);
        verify(receiptService).issueOrFindForCharge(chargeId);
        verify(receiptDeliveryService).sendAfterDcrApproval(declaration, receipt, "");
        verify(tuitionChargeSettlementService, never()).settleTuitionPayment(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private Charge tuitionCharge(UUID id, ChargeStatus status) {
        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode("IMT-PROPINA-2026-07-ALUNO")
                .setDescription("Propina Julho/2026")
                .setReferenceMonth("Julho/2026")
                .setDueDate(LocalDate.of(2026, 7, 10))
                .setAmount(new BigDecimal("45000.00"))
                .setCurrency("AOA")
                .setStatus(status);
        ReflectionTestUtils.setField(charge, "id", id);
        return charge;
    }
}
