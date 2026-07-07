package com.secretariapay.api.controller.admin;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.service.FallbackNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/communication-channels")
public class AdminCommunicationChannelController {

    private final FallbackNotificationService fallbackNotificationService;

    public AdminCommunicationChannelController(FallbackNotificationService fallbackNotificationService) {
        this.fallbackNotificationService = fallbackNotificationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listChannels() {
        List<Map<String, Object>> channels = fallbackNotificationService.listChannels();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", channels);
        response.put("items", channels);
        response.put("total", channels.size());
        response.put("generatedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/send-guide")
    public ResponseEntity<Map<String, Object>> sendGuideByEmail(@RequestBody GuideFallbackRequest request) {
        return ResponseEntity.ok(fallbackNotificationService.sendGuideByEmail(request));
    }

    @PostMapping("/sms/send-guide")
    public ResponseEntity<Map<String, Object>> sendGuideBySms(@RequestBody GuideFallbackRequest request) {
        return ResponseEntity.ok(fallbackNotificationService.sendGuideBySms(request));
    }
}
