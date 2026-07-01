package com.secretariapay.api.repository;

import com.secretariapay.api.entity.TicketAuditLog;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, UUID> {

    List<TicketAuditLog> findAllByOrderByCreatedAtDesc();

    List<TicketAuditLog> findByTicketCodeOrderByCreatedAtDesc(String ticketCode);

    List<TicketAuditLog> findByActionOrderByCreatedAtDesc(TicketAuditAction action);

    long countByAction(TicketAuditAction action);

    long countByActionAndSuccess(
            TicketAuditAction action,
            Boolean success
    );

    long countByActionAndCreatedAtBetween(
            TicketAuditAction action,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByActionAndSuccessAndCreatedAtBetween(
            TicketAuditAction action,
            Boolean success,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByActionAndTicket_Booking_Trip_TransportCompany_Id(
            TicketAuditAction action,
            UUID companyId
    );

    long countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_Id(
            TicketAuditAction action,
            Boolean success,
            UUID companyId
    );

    long countByActionAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
            TicketAuditAction action,
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
            TicketAuditAction action,
            Boolean success,
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<TicketAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<TicketAuditLog> findByTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}
