package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.entity.WhatsappSession;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretariaPayWhatsappConversationContextServiceTest {

    @Mock private WhatsappSessionRepository sessionRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private PaymentProofRepository paymentProofRepository;

    private SecretariaPayWhatsappConversationContextService service;
    private WhatsappSession session;
    private Charge charge;

    @BeforeEach
    void setUp() {
        service = new SecretariaPayWhatsappConversationContextService(
                sessionRepository,
                chargeRepository,
                paymentProofRepository
        );

        Student student = new Student()
                .setStudentNumber("202301404")
                .setFullName("Wilson Dala");
        ReflectionTestUtils.setField(student, "id", UUID.randomUUID());

        charge = new Charge()
                .setStudent(student)
                .setChargeCode("IMT-SERVICO-DECLARATION-WHATSAPP")
                .setDescription("Declaração sem Notas")
                .setServiceCode("DECLARATION_WITHOUT_GRADES")
                .setDueDate(LocalDate.now().plusDays(3))
                .setAmount(new BigDecimal("4400.00"))
                .setStatus(ChargeStatus.PENDING);
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());

        session = new WhatsappSession()
                .setId(UUID.randomUUID())
                .setPhoneNumber("5511915102566")
                .setSessionType(WhatsappSessionType.SECRETARIAPAY_ACADEMICO)
                .setStatus(WhatsappSessionStatus.ACTIVE)
                .setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setMetadata("{\"lastChargeCode\":\"IMT-SERVICO-DECLARATION-WHATSAPP\"}")
                .setExpiresAt(LocalDateTime.now().plusHours(1));

        when(sessionRepository.findFirstByPhoneNumberAndSessionTypeAndStatusOrderByUpdatedAtDesc(
                "5511915102566",
                WhatsappSessionType.SECRETARIAPAY_ACADEMICO,
                WhatsappSessionStatus.ACTIVE
        )).thenReturn(Optional.of(session));
        when(chargeRepository.findByChargeCode(charge.getChargeCode())).thenReturn(Optional.of(charge));
        when(sessionRepository.save(any(WhatsappSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void devePersistirComprovativoPrivadoUmaUnicaVezParaFilaDcr() {
        String mediaReference = "whatsapp-cloud-media://media-imetro-123";
        when(paymentProofRepository.existsByFileUrl(mediaReference)).thenReturn(false, true);
        when(paymentProofRepository.save(any(PaymentProof.class))).thenAnswer(invocation -> {
            PaymentProof proof = invocation.getArgument(0);
            ReflectionTestUtils.setField(proof, "id", UUID.randomUUID());
            return proof;
        });

        String firstReply = service.resolveContextualReply(
                "5511915102566",
                "image",
                "[imagem recebida]",
                "media-imetro-123",
                null,
                "image/jpeg"
        ).orElseThrow();
        String duplicateReply = service.resolveContextualReply(
                "5511915102566",
                "image",
                "[imagem recebida]",
                "media-imetro-123",
                null,
                "image/jpeg"
        ).orElseThrow();

        ArgumentCaptor<PaymentProof> proofCaptor = ArgumentCaptor.forClass(PaymentProof.class);
        verify(paymentProofRepository, times(1)).save(proofCaptor.capture());
        PaymentProof saved = proofCaptor.getValue();

        assertThat(saved.getCharge()).isSameAs(charge);
        assertThat(saved.getFileUrl()).isEqualTo(mediaReference);
        assertThat(saved.getFileName()).endsWith(".jpg");
        assertThat(saved.getMimeType()).isEqualTo("image/jpeg");
        assertThat(saved.getSubmittedByPhone()).isEqualTo("5511915102566");
        assertThat(firstReply).contains("registado", "Pendente de validação");
        assertThat(duplicateReply).contains("já estava registado", "Pendente de validação");
    }
}
