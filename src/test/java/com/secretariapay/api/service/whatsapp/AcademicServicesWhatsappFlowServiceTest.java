package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicServiceCatalogRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.academic.AcademicServiceOrderService;
import com.secretariapay.api.service.payment.AppyPayPaymentGatewayService;
import com.secretariapay.api.service.payment.InfinitePayTestPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServicesWhatsappFlowServiceTest {

    @Mock private AcademicServiceCatalogRepository catalogRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private AcademicServiceOrderService academicServiceOrderService;
    @Mock private SecretariaPayWhatsappConversationContextService conversationContextService;
    @Mock private WhatsAppCloudApiClient whatsAppCloudApiClient;
    @Mock private AppyPayPaymentGatewayService appyPayPaymentGatewayService;
    @Mock private InfinitePayTestPaymentService infinitePayTestPaymentService;
    @Mock private FallbackNotificationService fallbackNotificationService;

    private AcademicServicesWhatsappFlowService flow;
    private Student student;
    private AcademicServiceCatalog catalog;
    private Charge charge;
    private UUID studentId;
    private UUID serviceId;
    private UUID chargeId;

    @BeforeEach
    void setUp() throws Exception {
        flow = new AcademicServicesWhatsappFlowService(
                catalogRepository,
                studentRepository,
                chargeRepository,
                academicServiceOrderService,
                conversationContextService,
                whatsAppCloudApiClient,
                appyPayPaymentGatewayService,
                infinitePayTestPaymentService,
                fallbackNotificationService,
                "https://secretariapay-api.paixaoangola.com",
                "financeiro@imetro.ao"
        );

        studentId = UUID.randomUUID();
        student = new Student()
                .setStudentNumber("202301404")
                .setFullName("Wilson dos Santos Kahango Dala")
                .setEmail("wilson@example.com")
                .setWhatsapp("+5511915102566");
        ReflectionTestUtils.setField(student, "id", studentId);

        serviceId = UUID.randomUUID();
        catalog = new AcademicServiceCatalog()
                .setCode("DECLARATION_WITHOUT_GRADES")
                .setName("Declaração sem Notas")
                .setCategory("DOCUMENT")
                .setUnitPrice(new BigDecimal("4400.00"))
                .setCurrency("AOA")
                .setActive(true)
                .setAvailableWhatsapp(true)
                .setGeneratesGuide(true);
        ReflectionTestUtils.setField(catalog, "id", serviceId);

        chargeId = UUID.randomUUID();
        charge = new Charge()
                .setStudent(student)
                .setChargeCode("IMT-SERVICO-DECLARATION-WHATSAPP")
                .setDescription(catalog.getName())
                .setServiceCode(catalog.getCode())
                .setDueDate(LocalDate.now().plusDays(3))
                .setAmount(catalog.getUnitPrice())
                .setCurrency("AOA")
                .setStatus(ChargeStatus.PENDING);
        ReflectionTestUtils.setField(charge, "id", chargeId);

        when(catalogRepository.findByCodeIgnoreCase(anyString())).thenAnswer(invocation ->
                catalog.getCode().equalsIgnoreCase(invocation.getArgument(0))
                        ? Optional.of(catalog)
                        : Optional.empty());
        when(studentRepository.findByStudentNumber("202301404")).thenReturn(Optional.of(student));
        when(chargeRepository.findByChargeCode(charge.getChargeCode())).thenReturn(Optional.of(charge));
        when(academicServiceOrderService.createFromWhatsapp(
                eq(studentId),
                eq(serviceId),
                eq("5511915102566"),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(orderResponse());
    }

    @Test
    void deveCriarPedidoECobrancaNovaComVencimentoFuturo() {
        String phone = "+55 11 91510-2566";

        assertThat(flow.start(phone)).contains("Declaração sem Notas");
        assertThat(flow.handleIfActive(phone, "text", "1")).hasValueSatisfying(reply ->
                assertThat(reply).contains("Serviço selecionado", "202301404"));

        String prepared = flow.handleIfActive(phone, "text", "202301404").orElseThrow();

        ArgumentCaptor<LocalDate> dueDate = ArgumentCaptor.forClass(LocalDate.class);
        verify(academicServiceOrderService).createFromWhatsapp(
                eq(studentId),
                eq(serviceId),
                eq("5511915102566"),
                dueDate.capture()
        );
        verify(conversationContextService).rememberChargeContext(
                "5511915102566",
                charge,
                "ACADEMIC_SERVICE_ORDER"
        );
        verify(chargeRepository, never()).findByStudentIdOrderByDueDateDesc(studentId);

        assertThat(dueDate.getValue()).isEqualTo(LocalDate.now().plusDays(3));
        assertThat(dueDate.getValue()).isAfterOrEqualTo(LocalDate.now());
        assertThat(prepared)
                .contains("IMT-SRV-20260721-TESTE", "Declaração sem Notas")
                .contains(charge.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Test
    void deveManterContextoPersistenteParaComprovativoDaDcr() {
        String phone = "+55 11 91510-2566";
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));

        flow.start(phone);
        flow.handleIfActive(phone, "text", "1");
        flow.handleIfActive(phone, "text", "202301404");

        String reply = flow.handleIfActive(phone, "text", "4").orElseThrow();

        assertThat(reply)
                .contains("Pedido: IMT-SRV-20260721-TESTE")
                .contains("envie o comprovativo em imagem ou PDF");
        assertThat(flow.handleIfActive(phone, "image", "[imagem recebida]")).isEmpty();
        verify(whatsAppCloudApiClient).sendDocumentByLink(
                eq("5511915102566"),
                org.mockito.ArgumentMatchers.contains(charge.getChargeCode()),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.contains("IMT-SRV-20260721-TESTE")
        );
    }

    private AcademicServiceOrderDto.Response orderResponse() throws Exception {
        Constructor<?> constructor = AcademicServiceOrderDto.Response.class.getDeclaredConstructors()[0];
        Object[] arguments = new Object[constructor.getParameterCount()];
        arguments[0] = UUID.randomUUID();
        arguments[1] = "IMT-SRV-20260721-TESTE";
        arguments[2] = AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO;
        arguments[3] = studentId;
        arguments[4] = student.getStudentNumber();
        arguments[5] = student.getFullName();
        arguments[8] = serviceId;
        arguments[9] = catalog.getCode();
        arguments[10] = catalog.getName();
        arguments[11] = catalog.getCategory();
        arguments[12] = catalog.getUnitPrice();
        arguments[13] = catalog.getCurrency();
        arguments[14] = chargeId;
        arguments[15] = charge.getChargeCode();
        arguments[16] = ChargeStatus.PENDING;
        return (AcademicServiceOrderDto.Response) constructor.newInstance(arguments);
    }
}
