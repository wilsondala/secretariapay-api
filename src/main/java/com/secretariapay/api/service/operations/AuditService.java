package com.secretariapay.api.service.operations;

import com.secretariapay.api.entity.operations.AuditLog;
import com.secretariapay.api.repository.operations.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog record(String actor, String action, String entityType, String entityId, String details) {
        AuditLog log = new AuditLog()
                .setActor(clean(actor, "SYSTEM"))
                .setAction(clean(action, "UNKNOWN_ACTION"))
                .setEntityType(clean(entityType, "SYSTEM"))
                .setEntityId(clean(entityId, ""))
                .setDetails(clean(details, ""));
        return auditLogRepository.save(log);
    }

    public List<AuditLog> recent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isBlank()) return fallback;
        return value.trim();
    }
}
