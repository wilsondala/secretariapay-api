package com.secretariapay.api.controller;

import com.secretariapay.api.service.notification.AcademicServiceOrderEmailNotificationService;
import com.secretariapay.api.service.notification.AcademicServiceOrderNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-service-orders")
public class AcademicServiceOrderNotificationController {

    private static final String SECRETARIA_AUTHORITIES = "hasAnyAuthority(" +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO'," +
            "'ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION'," +
            "'ADMIN_IMETRO','ROLE_ADMIN_IMETRO','SECRETARIA','ROLE_SECRETARIA')";

    private final AcademicServiceOrderNotificationService notificationService;

    public AcademicServiceOrderNotificationController(AcademicServiceOrderNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/{id}/send-pickup-email")
    @PreAuthorize(SECRETARIA_AUTHORITIES)
    public AcademicServiceOrderEmailNotificationService.DeliveryResult sendPickupEmail(@PathVariable UUID id) {
        return notificationService.sendPickupEmail(id);
    }
}
