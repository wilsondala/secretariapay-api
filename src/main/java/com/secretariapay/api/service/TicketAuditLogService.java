package com.secretariapay.api.service;

import com.secretariapay.api.dto.ticketaudit.TicketAuditLogResponse;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.TicketAuditLog;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import com.secretariapay.api.entity.enums.TicketStatus;
import com.secretariapay.api.repository.TicketAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketAuditLogService {

    private final TicketAuditLogRepository ticketAuditLogRepository;

    public TicketAuditLogService(TicketAuditLogRepository ticketAuditLogRepository) {
        this.ticketAuditLogRepository = ticketAuditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            TicketAuditAction action,
            Ticket ticket,
            String ticketCode,
            boolean success,
            String message,
            TicketStatus ticketStatus,
            BookingStatus bookingStatus,
            String ipAddress,
            String userAgent
    ) {
        TicketAuditLog log = new TicketAuditLog()
                .setAction(action)
                .setTicket(ticket)
                .setTicketCode(ticketCode)
                .setSuccess(success)
                .setMessage(message)
                .setTicketStatus(ticketStatus)
                .setBookingStatus(bookingStatus)
                .setIpAddress(ipAddress)
                .setUserAgent(userAgent);

        ticketAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> findAll() {
        return ticketAuditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> findByTicketCode(String ticketCode) {
        return ticketAuditLogRepository.findByTicketCodeOrderByCreatedAtDesc(ticketCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> findByAction(TicketAuditAction action) {
        return ticketAuditLogRepository.findByActionOrderByCreatedAtDesc(action)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketAuditLogResponse toResponse(TicketAuditLog log) {
        return new TicketAuditLogResponse()
                .setId(log.getId())
                .setTicketCode(log.getTicketCode())
                .setAction(log.getAction())
                .setSuccess(log.getSuccess())
                .setMessage(log.getMessage())
                .setTicketStatus(log.getTicketStatus())
                .setBookingStatus(log.getBookingStatus())
                .setIpAddress(log.getIpAddress())
                .setUserAgent(log.getUserAgent())
                .setCreatedAt(log.getCreatedAt());
    }
}
