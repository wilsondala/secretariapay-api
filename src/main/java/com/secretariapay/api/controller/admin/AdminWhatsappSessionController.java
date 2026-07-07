package com.secretariapay.api.controller.admin;

import com.secretariapay.api.entity.WhatsappSession;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/whatsapp")
public class AdminWhatsappSessionController {

    private final WhatsappSessionRepository sessionRepository;

    public AdminWhatsappSessionController(WhatsappSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> listSessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "80") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String safeStatus = normalize(status);
        String safePhone = onlyDigits(phone);

        List<Map<String, Object>> sessions = sessionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .filter(session -> safeStatus.isBlank()
                        || enumName(session.getStatus()).equalsIgnoreCase(safeStatus))
                .filter(session -> safePhone.isBlank()
                        || onlyDigits(session.getPhoneNumber()).contains(safePhone))
                .limit(safeLimit)
                .map(this::toResponse)
                .toList();

        if (sessions.isEmpty()) {
            sessions = demoSessions();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", sessions);
        response.put("items", sessions);
        response.put("total", sessions.size());
        response.put("demoFallback", sessions.stream().anyMatch(item -> Boolean.TRUE.equals(item.get("demo"))));
        response.put("generatedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toResponse(WhatsappSession session) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", session.getId());
        item.put("phoneNumber", session.getPhoneNumber());
        item.put("status", enumName(session.getStatus()));
        item.put("sessionType", enumName(session.getSessionType()));
        item.put("currentStep", enumName(session.getCurrentStep()));
        item.put("lastMessageText", session.getLastMessageText());
        item.put("metadata", session.getMetadata());
        item.put("createdAt", session.getCreatedAt());
        item.put("updatedAt", session.getUpdatedAt());
        item.put("expiresAt", session.getExpiresAt());
        item.put("demo", false);
        return item;
    }

    private List<Map<String, Object>> demoSessions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                demoSession(
                        "demo-session-1",
                        "+244954485547",
                        "ACTIVE",
                        "FINANCIAL",
                        "WAITING_PAYMENT_CONFIRMATION",
                        "Guia demonstrativa emitida. Aguardando simulação de pagamento confirmado.",
                        now.minusMinutes(3),
                        now.plusMinutes(57)
                ),
                demoSession(
                        "demo-session-2",
                        "+5511915102566",
                        "COMPLETED",
                        "FINANCIAL",
                        "RECEIPT_SENT",
                        "Pagamento confirmado e recibo demonstrativo enviado em PDF.",
                        now.minusMinutes(18),
                        now.minusMinutes(1)
                ),
                demoSession(
                        "demo-session-3",
                        "+244923000000",
                        "ACTIVE",
                        "FINANCIAL",
                        "WAITING_PROOF",
                        "Pagamento por depósito simulado. Aguardando comprovativo para validação DCR.",
                        now.minusMinutes(28),
                        now.plusMinutes(32)
                )
        );
    }

    private Map<String, Object> demoSession(
            String id,
            String phoneNumber,
            String status,
            String sessionType,
            String currentStep,
            String lastMessageText,
            LocalDateTime updatedAt,
            LocalDateTime expiresAt
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("phoneNumber", phoneNumber);
        item.put("status", status);
        item.put("sessionType", sessionType);
        item.put("currentStep", currentStep);
        item.put("lastMessageText", lastMessageText);
        item.put("createdAt", updatedAt.minusMinutes(10));
        item.put("updatedAt", updatedAt);
        item.put("expiresAt", expiresAt);
        item.put("demo", true);
        return item;
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String onlyDigits(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }
}
