package com.vairapido.api.repository;

import com.vairapido.api.entity.TicketAuditLog;
import com.vairapido.api.entity.enums.TicketAuditAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, UUID> {

    List<TicketAuditLog> findAllByOrderByCreatedAtDesc();

    List<TicketAuditLog> findByTicketCodeOrderByCreatedAtDesc(String ticketCode);

    List<TicketAuditLog> findByActionOrderByCreatedAtDesc(TicketAuditAction action);
}