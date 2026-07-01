package com.secretariapay.api.repository;

import com.secretariapay.api.entity.WhatsAppMessage;
import com.secretariapay.api.entity.enums.WhatsAppMessageStatus;
import com.secretariapay.api.entity.enums.WhatsAppMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {

    List<WhatsAppMessage> findByStatus(WhatsAppMessageStatus status);

    List<WhatsAppMessage> findByMessageType(WhatsAppMessageType messageType);

    List<WhatsAppMessage> findByBooking_Id(UUID bookingId);

    List<WhatsAppMessage> findByTicket_Id(UUID ticketId);

    Optional<WhatsAppMessage> findFirstByBooking_IdAndMessageType(
            UUID bookingId,
            WhatsAppMessageType messageType
    );

    Optional<WhatsAppMessage> findFirstByTicket_IdAndMessageType(
            UUID ticketId,
            WhatsAppMessageType messageType
    );

    boolean existsByBooking_IdAndMessageType(
            UUID bookingId,
            WhatsAppMessageType messageType
    );

    boolean existsByTicket_IdAndMessageType(
            UUID ticketId,
            WhatsAppMessageType messageType
    );
}

