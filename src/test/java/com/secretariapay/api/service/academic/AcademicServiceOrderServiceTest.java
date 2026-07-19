package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.AcademicDocumentRequestRepository;
import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicServiceCatalogRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServiceOrderServiceTest {

    @Mock private AcademicServiceOrderRepository orderRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AcademicServiceCatalogRepository catalogRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private AcademicDocumentRequestRepository documentRepository;
    @Mock private AcademicDocumentService academicDocumentService;
    @Mock private WhatsAppCloudApiClient whatsAppCloudApiClient;

    private AcademicServiceOrderService service;
    private AcademicServiceOrder order;
    private Charge charge;
    private UUID orderId;
    private UUID chargeId;

    @BeforeEach
    void setUp() {
        service = new AcademicServiceOrderService(
                orderRepository,
                studentRepository,
                catalogRepository,
                chargeRepository,
                documentRepository,
                academicDocumentService,
                whatsAppCloudApiClient
        );

        Course course = new Course().setName("Gestão Financeira e Bancária");
        AcademicClass academicClass = new AcademicClass()
                .setName("GFB-2026")
                .setAcademicYear("2026")
                .setCourse(course);
        Student student = new Student()
                .setStudentNumber("202301404")
                .setFullName("Wilson dos Santos Kahango Dala")
                .setWhatsapp("+244923168085")
                .setAcademicClass(academicClass);
        ReflectionTestUtils.setField(student, "id", UUID.randomUUID());

        AcademicServiceCatalog catalog = new AcademicServiceCatalog()
                .setCode("DECLARATION_WITHOUT_GRADES")
                .setName("Declaração sem notas")
                .setCategory("DOCUMENT")
                .setUnitPrice(new BigDecimal("4400.00"))
                .setCurrency("AOA")
                .setActive(true);
        ReflectionTestUtils.setField(catalog, "id", UUID.randomUUID());

        chargeId = UUID.randomUUID();
        charge = new Charge()
                .setStudent(student)
                .setChargeCode("IMT-SERVICO-DECLARATION-TEST")
                .setDescription(catalog.getName())
                .setServiceCode(catalog.getCode())
                .setDueDate(LocalDate.now().plusDays(2))
                .setAmount(catalog.getUnitPrice())
                .setCurrency("AOA")
                .setStatus(ChargeStatus.PENDING);
        ReflectionTestUtils.setField(charge, "id", chargeId);

        orderId = UUID.randomUUID();
        order = new AcademicServiceOrder()
                .setOrderCode("IMT-SRV-20260718-TESTE")
                .setStudent(student)
                .setService(catalog)
                .setCharge(charge)
                .setPurpose("Comprovação académica")
                .setStatus(AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO);
        ReflectionTestUtils.setField(order, "id", orderId);
        order.prePersist();

        lenient().when(orderRepository.save(any(AcademicServiceOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveLiberarFilaDaSecretariaSomenteDepoisDoPagamentoConfirmado() {
        charge.setStatus(ChargeStatus.PAID).setPaidAt(LocalDateTime.now());
        when(orderRepository.findByChargeId(chargeId)).thenReturn(Optional.of(order));

        service.confirmPaymentByCharge(charge);

        assertThat(order.getStatus()).isEqualTo(AcademicServiceOrderStatus.PAGO);
        assertThat(order.getPaymentConfirmedAt()).isNotNull();
        verify(orderRepository).save(order);
    }

    @Test
    void naoDeveGerarDocumentoEnquantoCobrancaNaoEstiverPaga() {
        order.setStatus(AcademicServiceOrderStatus.PAGO);
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.generateDocument(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pagamento confirmado");
    }

    @Test
    void deveExecutarFluxoFisicoAteEntregaComWhatsapp() {
        charge.setStatus(ChargeStatus.PAID).setPaidAt(LocalDateTime.now());
        order.setStatus(AcademicServiceOrderStatus.PAGO).setPaymentConfirmedAt(LocalDateTime.now());
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.of(order));
        when(documentRepository.save(any(AcademicDocumentRequest.class))).thenAnswer(invocation -> {
            AcademicDocumentRequest document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
            document.prePersist();
            return document;
        });

        AcademicDocumentDto.Response signed = mock(AcademicDocumentDto.Response.class);
        when(signed.signedAt()).thenReturn(LocalDateTime.now());
        when(academicDocumentService.signDemo(any(UUID.class))).thenReturn(signed);
        when(whatsAppCloudApiClient.sendText(any(String.class), any(String.class)))
                .thenReturn(WhatsAppCloudSendResult.sent("wamid.test", 200));

        assertThat(service.generateDocument(orderId).status()).isEqualTo(AcademicServiceOrderStatus.DOCUMENTO_GERADO);
        assertThat(service.markReadyForPrint(orderId).status()).isEqualTo(AcademicServiceOrderStatus.PRONTO_PARA_IMPRESSAO);
        assertThat(service.markPrinted(orderId).status()).isEqualTo(AcademicServiceOrderStatus.IMPRESSO);
        assertThat(service.submitForSignature(orderId).status()).isEqualTo(AcademicServiceOrderStatus.AGUARDANDO_ASSINATURA);
        assertThat(service.sign(orderId).status()).isEqualTo(AcademicServiceOrderStatus.ASSINADO);
        assertThat(service.markReadyForPickup(orderId,
                new AcademicServiceOrderDto.ActionRequest("Secretaria Académica", null, null, null)).status())
                .isEqualTo(AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO);
        assertThat(service.sendPickupWhatsapp(orderId).status()).isEqualTo(AcademicServiceOrderStatus.WHATSAPP_ENVIADO);

        AcademicServiceOrderDto.Response delivered = service.deliver(orderId,
                new AcademicServiceOrderDto.ActionRequest(null, "Wilson Dala", "006123456LA042", "Entregue presencialmente."));

        assertThat(delivered.status()).isEqualTo(AcademicServiceOrderStatus.ENTREGUE);
        assertThat(delivered.recipientName()).isEqualTo("Wilson Dala");
        assertThat(delivered.recipientDocumentNumber()).isEqualTo("006123456LA042");
        assertThat(delivered.deliveredAt()).isNotNull();
    }
}