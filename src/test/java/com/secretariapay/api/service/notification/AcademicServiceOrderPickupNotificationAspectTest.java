package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServiceOrderPickupNotificationAspectTest {

    @Mock
    private AcademicServiceOrderRepository orderRepository;

    @Mock
    private AcademicServiceOrderEmailNotificationService emailNotificationService;

    @Test
    void deveEnviarEmailDepoisDoWhatsappConfirmado() {
        UUID orderId = UUID.randomUUID();
        AcademicServiceOrder order = new AcademicServiceOrder().setOrderCode("IMT-SRV-EMAIL-TEST");
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.of(order));
        when(emailNotificationService.sendReadyForPickup(order))
                .thenReturn(AcademicServiceOrderEmailNotificationService.DeliveryResult.sent("estudante@imetro.ao"));

        AcademicServiceOrderPickupNotificationAspect aspect =
                new AcademicServiceOrderPickupNotificationAspect(orderRepository, emailNotificationService);

        aspect.sendComplementaryEmail(orderId);

        verify(emailNotificationService).sendReadyForPickup(order);
    }

    @Test
    void naoDeveTentarEnviarEmailQuandoPedidoNaoForEncontrado() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findOneById(orderId)).thenReturn(Optional.empty());

        AcademicServiceOrderPickupNotificationAspect aspect =
                new AcademicServiceOrderPickupNotificationAspect(orderRepository, emailNotificationService);

        aspect.sendComplementaryEmail(orderId);

        verify(emailNotificationService, never()).sendReadyForPickup(org.mockito.ArgumentMatchers.any());
    }
}
