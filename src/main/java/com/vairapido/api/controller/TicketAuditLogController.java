package com.vairapido.api.controller;

import com.vairapido.api.dto.ticketaudit.TicketAuditLogResponse;
import com.vairapido.api.entity.enums.TicketAuditAction;
import com.vairapido.api.service.TicketAuditLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ticket-audit-logs")
public class TicketAuditLogController {

    private final TicketAuditLogService ticketAuditLogService;

    public TicketAuditLogController(TicketAuditLogService ticketAuditLogService) {
        this.ticketAuditLogService = ticketAuditLogService;
    }

    @GetMapping
    public List<TicketAuditLogResponse> findAll() {
        return ticketAuditLogService.findAll();
    }

    @GetMapping("/ticket/{ticketCode}")
    public List<TicketAuditLogResponse> findByTicketCode(
            @PathVariable String ticketCode
    ) {
        return ticketAuditLogService.findByTicketCode(ticketCode);
    }

    @GetMapping("/action/{action}")
    public List<TicketAuditLogResponse> findByAction(
            @PathVariable TicketAuditAction action
    ) {
        return ticketAuditLogService.findByAction(action);
    }
}