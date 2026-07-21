package com.secretariapay.api.service.notification;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class AcademicServiceOrderNotificationService {

    private static final Set<AcademicServiceOrderStatus> EMAIL_ALLOWED_STATUSES = Set.of(
            AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO,
            AcademicServiceOrderStatus.WHATSAPP_ENVIADO,
            AcademicServiceOrderStatus.ENTREGUE
    );

    private final AcademicServiceOrderRepository orderRepository;
    private final AcademicServiceOrderEmailNotificationService emailNotificationService;

    public AcademicServiceOrderNotificationService(
            AcademicServiceOrderRepository orderRepository,
            AcademicServiceOrderEmailNotificationService emailNotificationService
    ) {
        this.orderRepository = orderRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional(readOnly = true)
    public AcademicServiceOrderEmailNotificationService.DeliveryResult sendPickupEmail(UUID orderId) {
        AcademicServiceOrder order = orderRepository.findOneById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido de serviço académico não encontrado."));

        if (order.getStatus() == null || !EMAIL_ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new IllegalStateException(
                    "O e-mail de levantamento só pode ser enviado depois da disponibilidade física do documento. Estado atual: "
                            + order.getStatus() + "."
            );
        }

        return emailNotificationService.sendReadyForPickup(order);
    }
}
