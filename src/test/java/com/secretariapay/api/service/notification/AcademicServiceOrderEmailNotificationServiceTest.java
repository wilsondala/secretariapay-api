package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServiceOrderEmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Test
    void deveIgnorarEnvioQuandoCanalEstiverDesativado() {
        AcademicServiceOrderEmailNotificationService service = new AcademicServiceOrderEmailNotificationService(
                mailSenderProvider,
                false,
                "dcr_pay@imetroangola.com",
                "",
                "SecretáriaPay Académico — IMETRO"
        );

        AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                service.sendReadyForPickup(buildOrder("estudante@imetro.ao", null));

        assertThat(result.sent()).isFalse();
        assertThat(result.status()).isEqualTo("SKIPPED_DISABLED");
        assertThat(result.recipient()).isEqualTo("estudante@imetro.ao");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void deveEnviarMensagemInstitucionalPeloCanalSmtpValidado() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        AcademicServiceOrderEmailNotificationService service = new AcademicServiceOrderEmailNotificationService(
                mailSenderProvider,
                true,
                "secretariapay@paixaoangola.com",
                "df.oi_pay@imetroangola.com",
                "SecretáriaPay Académico — IMETRO"
        );

        AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                service.sendReadyForPickup(buildOrder("dalakahango@hotmail.com", null));

        assertThat(result.sent()).isTrue();
        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.recipient()).isEqualTo("dalakahango@hotmail.com");

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("secretariapay@paixaoangola.com");
        assertThat(message.getTo()).containsExactly("dalakahango@hotmail.com");
        assertThat(message.getCc()).isNull();
        assertThat(message.getReplyTo()).isNull();
        assertThat(message.getSubject()).isEqualTo("O seu pedido académico está pronto para levantamento");
        assertThat(message.getText())
                .contains("Wilson dos Santos Kahango Dala")
                .contains("Declaração sem notas")
                .contains("202301404")
                .contains("IMT-SRV-20260719-EMAIL")
                .contains("Secretaria Académica do IMETRO")
                .contains("Atenciosamente,")
                .contains("IMETRO");
    }

    @Test
    void deveRegistarAusenciaDeDestinatarioSemFalharFluxo() {
        AcademicServiceOrderEmailNotificationService service = new AcademicServiceOrderEmailNotificationService(
                mailSenderProvider,
                true,
                "dcr_pay@imetroangola.com",
                "",
                "SecretáriaPay Académico — IMETRO"
        );

        AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                service.sendReadyForPickup(buildOrder(null, null));

        assertThat(result.sent()).isFalse();
        assertThat(result.status()).isEqualTo("SKIPPED_NO_RECIPIENT");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void deveInformarQuandoSmtpNaoEstiverConfigurado() {
        AcademicServiceOrderEmailNotificationService service = new AcademicServiceOrderEmailNotificationService(
                mailSenderProvider,
                true,
                "dcr_pay@imetroangola.com",
                "",
                "SecretáriaPay Académico — IMETRO"
        );

        AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                service.sendReadyForPickup(buildOrder("estudante@imetro.ao", null));

        assertThat(result.sent()).isFalse();
        assertThat(result.status()).isEqualTo("SKIPPED_NOT_CONFIGURED");
        assertThat(result.detail()).contains("SMTP");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void deveClassificarBloqueioDoFiltroAntispamDoProvedor() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException(
                "Failed messages: SMTPSendFailedException: 550 5.7.1 Mail contain spam content 4h3IgH1d3kz1xr1"
        )).when(mailSender).send(isA(SimpleMailMessage.class));

        AcademicServiceOrderEmailNotificationService service = new AcademicServiceOrderEmailNotificationService(
                mailSenderProvider,
                true,
                "secretariapay@paixaoangola.com",
                "",
                "SecretariaPay Academico"
        );

        AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                service.sendReadyForPickup(buildOrder("dalakahango@hotmail.com", null));

        assertThat(result.sent()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED_CONTENT_REJECTED");
        assertThat(result.recipient()).isEqualTo("dalakahango@hotmail.com");
        assertThat(result.detail())
                .contains("filtro antispam")
                .contains("Referência SMTP")
                .contains("550 5.7.1 Mail contain spam content 4h3IgH1d3kz1xr1")
                .doesNotContain("SPF", "DKIM");
    }

    private AcademicServiceOrder buildOrder(String studentEmail, String guardianEmail) {
        Student student = new Student()
                .setStudentNumber("202301404")
                .setFullName("Wilson dos Santos Kahango Dala")
                .setEmail(studentEmail)
                .setGuardianEmail(guardianEmail);

        AcademicServiceCatalog service = new AcademicServiceCatalog()
                .setCode("DECLARATION_WITHOUT_GRADES")
                .setName("Declaração sem notas");

        return new AcademicServiceOrder()
                .setOrderCode("IMT-SRV-20260719-EMAIL")
                .setStudent(student)
                .setService(service)
                .setPhysicalLocation("Secretaria Académica do IMETRO");
    }
}
