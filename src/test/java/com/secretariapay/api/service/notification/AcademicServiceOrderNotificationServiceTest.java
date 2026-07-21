package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServiceOrderNotificationServiceTest {

    @Mock
    private AcademicServiceOrderRepository orderRepository;

    @Mock
    private AcademicServiceOrderEmailNotificationService emailNotificationService;

    @Test
    void devePermitirEmailDepoisDoWhatsapp() {
        UUID orderId = UUID.randomUUID();
        AcademicServiceOrder order = new AcademicServiceOrder()
                .setOrderCode("IMT-SRV-EMAIL-MANUAL")
                .setStatus(AcademicServiceOrderStatus.WHATSAPP_ENVIADO);
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.of(order));
        when(emailNotificationService.sendReadyForPickup(order))
                .thenReturn(AcademicServiceOrderEmailNotificationService.DeliveryResult.sent("estudante@imetro.ao"));

        AcademicServiceOrderNotificationService service =
                new AcademicServiceOrderNotificationService(orderRepository, emailNotificationService);

        AcademicServiceOrderEmailNotificationService.DeliveryResult result = service.sendPickupEmail(orderId);

        assertThat(result.sent()).isTrue();
        assertThat(result.status()).isEqualTo("SENT");
        verify(emailNotificationService).sendReadyForPickup(order);
    }

    @Test
    void naoDevePermitirEmailAntesDaDisponibilidadeFisica() {
        UUID orderId = UUID.randomUUID();
        AcademicServiceOrder order = new AcademicServiceOrder()
                .setOrderCode("IMT-SRV-EMAIL-BLOCKED")
                .setStatus(AcademicServiceOrderStatus.ASSINADO);
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.of(order));

        AcademicServiceOrderNotificationService service =
                new AcademicServiceOrderNotificationService(orderRepository, emailNotificationService);

        assertThatThrownBy(() -> service.sendPickupEmail(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disponibilidade física");
    }
}
