package com.secretariapay.api.service.notification;

import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class AcademicServiceOrderPickupNotificationAspect {

    private static final Logger log = LoggerFactory.getLogger(AcademicServiceOrderPickupNotificationAspect.class);

    private final AcademicServiceOrderRepository orderRepository;
    private final AcademicServiceOrderEmailNotificationService emailNotificationService;

    public AcademicServiceOrderPickupNotificationAspect(
            AcademicServiceOrderRepository orderRepository,
            AcademicServiceOrderEmailNotificationService emailNotificationService
    ) {
        this.orderRepository = orderRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * O WhatsApp continua sendo a transição obrigatória do fluxo. Depois que o
     * provedor confirma esse canal, o e-mail é processado como comunicação
     * complementar. Qualquer falha de SMTP é absorvida pelo serviço de e-mail,
     * evitando rollback e reenvio duplicado do WhatsApp.
     */
    @AfterReturning(
            pointcut = "execution(* com.secretariapay.api.service.academic.AcademicServiceOrderService.sendPickupWhatsapp(java.util.UUID)) && args(orderId)"
    )
    public void sendComplementaryEmail(UUID orderId) {
        orderRepository.findOneById(orderId).ifPresentOrElse(order -> {
            AcademicServiceOrderEmailNotificationService.DeliveryResult result =
                    emailNotificationService.sendReadyForPickup(order);
            log.info("Canal de e-mail do pedido {} concluído com estado {} e destinatário {}.",
                    order.getOrderCode(), result.status(), result.recipient());
        }, () -> log.warn("Pedido {} não encontrado após o envio do WhatsApp; e-mail complementar ignorado.", orderId));
    }
}
