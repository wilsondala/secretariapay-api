package com.vairapido.api.repository;

import com.vairapido.api.entity.WhatsappSession;
import com.vairapido.api.entity.enums.WhatsappSessionStatus;
import com.vairapido.api.entity.enums.WhatsappSessionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappSessionRepository extends JpaRepository<WhatsappSession, UUID> {

    Optional<WhatsappSession> findFirstByPhoneNumberAndStatusOrderByUpdatedAtDesc(
            String phoneNumber,
            WhatsappSessionStatus status
    );

    Optional<WhatsappSession> findFirstByPhoneNumberAndSessionTypeAndStatusOrderByUpdatedAtDesc(
            String phoneNumber,
            WhatsappSessionType sessionType,
            WhatsappSessionStatus status
    );

    List<WhatsappSession> findByStatusAndExpiresAtBefore(
            WhatsappSessionStatus status,
            LocalDateTime expiresAt
    );

    List<WhatsappSession> findByPhoneNumberOrderByUpdatedAtDesc(String phoneNumber);
}