package com.vairapido.api.repository;

import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByBooking_Id(UUID bookingId);

    List<Ticket> findByBooking_Passenger_IdOrderByIssuedAtDesc(UUID passengerId);

    List<Ticket> findByStatus(TicketStatus status);

    boolean existsByBooking_Id(UUID bookingId);

    long countByStatus(TicketStatus status);

    long countByIssuedAtBetween(
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByStatusAndIssuedAtBetween(
            TicketStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByBooking_Trip_TransportCompany_Id(UUID companyId);

    long countByStatusAndBooking_Trip_TransportCompany_Id(
            TicketStatus status,
            UUID companyId
    );

    long countByBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    long countByStatusAndBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
            TicketStatus status,
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<Ticket> findByIssuedAtBetweenOrderByIssuedAtDesc(
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<Ticket> findByBooking_Trip_TransportCompany_IdAndIssuedAtBetweenOrderByIssuedAtDesc(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}