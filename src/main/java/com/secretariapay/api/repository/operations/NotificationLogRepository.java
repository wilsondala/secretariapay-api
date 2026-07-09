package com.secretariapay.api.repository.operations;

import com.secretariapay.api.entity.operations.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Optional<NotificationLog> findByChargeIdAndNotificationTypeAndChannelAndBusinessDate(
            UUID chargeId,
            String notificationType,
            String channel,
            LocalDate businessDate
    );

    boolean existsByChargeIdAndNotificationTypeAndChannelAndBusinessDateAndStatus(
            UUID chargeId,
            String notificationType,
            String channel,
            LocalDate businessDate,
            String status
    );

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();
}
