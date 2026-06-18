package com.vairapido.api.repository;

import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByBooking_Id(UUID bookingId);

    List<Ticket> findByStatus(TicketStatus status);

    boolean existsByBooking_Id(UUID bookingId);

    long countByStatus(TicketStatus status);
}