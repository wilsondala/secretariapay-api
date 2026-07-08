package com.secretariapay.api.controller.publicapi;

import com.secretariapay.api.service.payment.InfinitePayTestPaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/infinitepay")
public class InfinitePayPublicController {

    private final InfinitePayTestPaymentService infinitePayTestPaymentService;

    public InfinitePayPublicController(InfinitePayTestPaymentService infinitePayTestPaymentService) {
        this.infinitePayTestPaymentService = infinitePayTestPaymentService;
    }

    @GetMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> success(@RequestParam(name = "order_nsu", required = false) String orderNsu) {
        InfinitePayTestPaymentService.InfinitePayConfirmationResult result = infinitePayTestPaymentService.confirmBySuccessReturn(orderNsu);
        return ResponseEntity.ok(buildHtml(result.success(), result.message(), result.orderNsu(), result.receiptCode()));
    }

    @GetMapping(value = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> cancel(@RequestParam(name = "order_nsu", required = false) String orderNsu) {
        InfinitePayTestPaymentService.InfinitePayConfirmationResult result = infinitePayTestPaymentService.cancel(orderNsu);
        return ResponseEntity.ok(buildHtml(false, result.message(), result.orderNsu(), null));
    }

    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestBody(required = false) Map<String, Object> payload) {
        String orderNsu = extractOrderNsu(payload);
        String status = extractStatus(payload);

        if (status.equals("paid") || status.equals("approved") || status.equals("success") || status.equals("completed")) {
            InfinitePayTestPaymentService.InfinitePayConfirmationResult result = infinitePayTestPaymentService.confirmBySuccessReturn(orderNsu);
            return Map.of(
                    "received", true,
                    "processed", result.success(),
                    "message", result.message(),
                    "order_nsu", result.orderNsu() == null ? "" : result.orderNsu(),
                    "receipt_code", result.receiptCode() == null ? "" : result.receiptCode()
            );
        }

        return Map.of(
                "received", true,
                "processed", false,
                "message", "Webhook InfinitePay recebido, mas status ainda não é pago.",
                "order_nsu", orderNsu == null ? "" : orderNsu,
                "status", status
        );
    }

    private String extractOrderNsu(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return "";
        Object value = firstNonNull(
                payload.get("order_nsu"),
                payload.get("orderNsu"),
                payload.get("external_reference"),
                payload.get("reference"),
                payload.get("order_id")
        );
        if (value == null && payload.get("data") instanceof Map<?, ?> data) {
            value = firstNonNull(
                    data.get("order_nsu"),
                    data.get("orderNsu"),
                    data.get("external_reference"),
                    data.get("reference"),
                    data.get("order_id")
            );
        }
        return value == null ? "" : value.toString().trim();
    }

    private String extractStatus(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return "";
        Object value = firstNonNull(payload.get("status"), payload.get("event"));
        if (value == null && payload.get("data") instanceof Map<?, ?> data) {
            value = firstNonNull(data.get("status"), data.get("event"));
        }
        return value == null ? "" : value.toString().trim().toLowerCase();
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) return value;
        }
        return null;
    }

    private String buildHtml(boolean success, String message, String orderNsu, String receiptCode) {
        String title = success ? "Pagamento confirmado" : "Pagamento não confirmado";
        String color = success ? "#16a34a" : "#dc2626";
        String safeMessage = message == null ? "" : message;
        String safeOrder = orderNsu == null ? "" : orderNsu;
        String safeReceipt = receiptCode == null ? "" : receiptCode;

        return """
                <!doctype html>
                <html lang=\"pt-BR\">
                <head>
                    <meta charset=\"utf-8\" />
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; background: #f8fafc; margin: 0; padding: 24px; color: #0f172a; }
                        .card { max-width: 620px; margin: 40px auto; background: white; border-radius: 18px; padding: 28px; box-shadow: 0 12px 32px rgba(15,23,42,.12); }
                        .badge { display: inline-block; background: %s; color: white; border-radius: 999px; padding: 8px 14px; font-weight: 700; }
                        h1 { margin: 18px 0 10px; }
                        p { line-height: 1.5; }
                        .muted { color: #64748b; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <main class=\"card\">
                        <span class=\"badge\">SecretáriaPay Académico</span>
                        <h1>%s</h1>
                        <p>%s</p>
                        <p class=\"muted\">Código InfinitePay: %s</p>
                        <p class=\"muted\">Bordereau: %s</p>
                        <p>Já pode voltar ao WhatsApp. Se o pagamento foi confirmado, o comprovativo será enviado automaticamente.</p>
                    </main>
                </body>
                </html>
                """.formatted(title, color, title, safeMessage, safeOrder, safeReceipt);
    }
}
