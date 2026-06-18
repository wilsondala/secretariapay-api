package com.vairapido.api.repository;

import com.vairapido.api.entity.WhatsAppMessage;
import com.vairapido.api.entity.enums.WhatsAppMessageStatus;
import com.vairapido.api.entity.enums.WhatsAppMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {

    List<WhatsAppMessage> findByStatus(WhatsAppMessageStatus status);

    List<WhatsAppMessage> findByMessageType(WhatsAppMessageType messageType);

    List<WhatsAppMessage> findByBooking_Id(UUID bookingId);

    List<WhatsAppMessage> findByTicket_Id(UUID ticketId);
}