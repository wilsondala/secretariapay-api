package com.secretariapay.api.repository.operations;

import com.secretariapay.api.entity.operations.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    boolean existsByChargeIdAndNotificationTypeAndChannelAndBusinessDate(
            UUID chargeId,
            String notificationType,
            String channel,
            LocalDate businessDate
    );

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();
}
