package com.secretariapay.api.service.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SecretariaPayWhatsappWebhookService {

    private final String verifyToken;
    private final boolean whatsappEnabled;
    private final String phoneNumberId;
    private final String accessToken;
    private final String graphApiVersion;
    private final String graphApiBaseUrl;

    private final SecretariaPayWhatsappBrainService brainService;
    private final SecretariaPayWhatsappAcademicSupportService academicSupportService;
    private final SecretariaPayWhatsappFinancialConversationService financialConversationService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SecretariaPayWhatsappWebhookService(
            @Value("${secretariapay.whatsapp.verify-token:secretariapay-dev-token}")
            String verifyToken,

            @Value("${secretariapay.whatsapp.enabled:false}")
            boolean whatsappEnabled,

            @Value("${secretariapay.whatsapp.phone-number-id:}")
            String phoneNumberId,

            @Value("${secretariapay.whatsapp.access-token:}")
            String accessToken,

            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}")
            String graphApiVersion,

            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}")
            String graphApiBaseUrl,

            SecretariaPayWhatsappBrainService brainService,
            SecretariaPayWhatsappAcademicSupportService academicSupportService,
            SecretariaPayWhatsappFinancialConversationService financialConversationService
    ) {
        this.verifyToken = verifyToken;
        this.whatsappEnabled = whatsappEnabled;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.graphApiVersion = graphApiVersion;
        this.graphApiBaseUrl = graphApiBaseUrl;
        this.brainService = brainService;
        this.academicSupportService = academicSupportService;
        this.financialConversationService = financialConversationService;

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String verifyWebhook(
            String mode,
            String token,
            String challenge
    ) {
        boolean isSubscribeMode = "subscribe".equals(mode);
        boolean isValidToken = verifyToken != null
                && !verifyToken.isBlank()
                && verifyToken.equals(token);

        if (!isSubscribeMode || !isValidToken || challenge == null || challenge.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Webhook SecretáriaPay WhatsApp não autorizado."
            );
        }

        return challenge;
    }

    public Map<String, Object> receiveWebhookPayload(Map<String, Object> payload) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("received", true);
        response.put("module", "SECRETARIAPAY_ACADEMICO_WHATSAPP_WEBHOOK");
        response.put("receivedAt", LocalDateTime.now().toString());
        response.put("payloadPresent", payload != null && !payload.isEmpty());

        Optional<InboundWhatsappMessage> inboundMessage = extractInboundMessage(payload);

        if (inboundMessage.isEmpty()) {
            response.put("processed", false);
            response.put("status", "IGNORED");
            response.put(
                    "reason",
                    "Payload recebido, mas sem mensagem de usuário para responder. Pode ser status, delivery ou evento administrativo."
            );
            return response;
        }

        InboundWhatsappMessage message = inboundMessage.get();

        Optional<String> financialReply = financialConversationService.handle(
                message.from(),
                message.type(),
                message.body()
        );

        String replyText = financialReply
                .orElseGet(() -> academicSupportService.buildDatabaseAwareReply(
                                message.from(),
                                message.type(),
                                message.body(),
                                message.mediaId(),
                                message.fileName(),
                                message.mimeType()
                        )
                        .orElseGet(() -> brainService.buildReply(
                                message.type(),
                                message.body()
                        )));

        WhatsappSendResult sendResult = sendTextMessage(
                message.from(),
                replyText
        );

        response.put("processed", true);
        response.put("status", sendResult.success() ? "AUTO_REPLY_SENT" : "AUTO_REPLY_FAILED");
        response.put("from", message.from());
        response.put("messageType", message.type());
        response.put("messageText", message.body());
        response.put("replyText", replyText);
        response.put("replySent", sendResult.success());
        response.put("providerStatusCode", sendResult.statusCode());

        if (financialReply.isPresent()) {
            response.put("flow", "SECRETARIAPAY_FINANCIAL_CONVERSATION_ROUTER");
        }

        if (message.mediaId() != null && !message.mediaId().isBlank()) {
            response.put("mediaId", message.mediaId());
        }

        if (message.fileName() != null && !message.fileName().isBlank()) {
            response.put("fileName", message.fileName());
        }

        if (message.mimeType() != null && !message.mimeType().isBlank()) {
            response.put("mimeType", message.mimeType());
        }

        if (sendResult.providerMessageId() != null && !sendResult.providerMessageId().isBlank()) {
            response.put("providerMessageId", sendResult.providerMessageId());
        }

        if (sendResult.errorMessage() != null && !sendResult.errorMessage().isBlank()) {
            response.put("errorMessage", sendResult.errorMessage());
        }

        return response;
    }

    private Optional<InboundWhatsappMessage> extractInboundMessage(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.valueToTree(payload);
        JsonNode entries = root.path("entry");

        if (!entries.isArray()) {
            return Optional.empty();
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");

            if (!changes.isArray()) {
                continue;
            }

            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                JsonNode messages = value.path("messages");

                if (!messages.isArray() || messages.size() == 0) {
                    continue;
                }

                for (JsonNode message : messages) {
                    String from = message.path("from").asText("");
                    String type = message.path("type").asText("");

                    if (from == null || from.isBlank()) {
                        continue;
                    }

                    InboundWhatsappMedia media = extractMediaPayload(message, type);
                    String body = extractMessageBody(message, type, media);

                    return Optional.of(
                            new InboundWhatsappMessage(
                                    from.trim(),
                                    type == null || type.isBlank() ? "unknown" : type.trim(),
                                    body,
                                    media.mediaId(),
                                    media.fileName(),
                                    media.mimeType()
                            )
                    );
                }
            }
        }

        return Optional.empty();
    }

    private InboundWhatsappMedia extractMediaPayload(JsonNode message, String type) {
        if ("image".equalsIgnoreCase(type)) {
            JsonNode image = message.path("image");

            return new InboundWhatsappMedia(
                    image.path("id").asText(""),
                    "",
                    image.path("mime_type").asText("")
            );
        }

        if ("document".equalsIgnoreCase(type)) {
            JsonNode document = message.path("document");

            return new InboundWhatsappMedia(
                    document.path("id").asText(""),
                    document.path("filename").asText(""),
                    document.path("mime_type").asText("")
            );
        }

        if ("audio".equalsIgnoreCase(type)) {
            JsonNode audio = message.path("audio");

            return new InboundWhatsappMedia(
                    audio.path("id").asText(""),
                    "",
                    audio.path("mime_type").asText("")
            );
        }

        if ("video".equalsIgnoreCase(type)) {
            JsonNode video = message.path("video");

            return new InboundWhatsappMedia(
                    video.path("id").asText(""),
                    "",
                    video.path("mime_type").asText("")
            );
        }

        return new InboundWhatsappMedia("", "", "");
    }

    private String extractMessageBody(
            JsonNode message,
            String type,
            InboundWhatsappMedia media
    ) {
        if ("text".equalsIgnoreCase(type)) {
            return message.path("text").path("body").asText("").trim();
        }

        if ("image".equalsIgnoreCase(type)) {
            return "[imagem recebida]";
        }

        if ("document".equalsIgnoreCase(type)) {
            String filename = media.fileName();

            if (filename == null || filename.isBlank()) {
                return "[documento recebido]";
            }

            return "[documento recebido: " + filename + "]";
        }

        if ("audio".equalsIgnoreCase(type)) {
            return "[áudio recebido]";
        }

        if ("video".equalsIgnoreCase(type)) {
            return "[vídeo recebido]";
        }

        if ("button".equalsIgnoreCase(type)) {
            return message.path("button").path("text").asText("").trim();
        }

        if ("interactive".equalsIgnoreCase(type)) {
            JsonNode interactive = message.path("interactive");

            String buttonReplyTitle = interactive.path("button_reply").path("title").asText("");
            if (buttonReplyTitle != null && !buttonReplyTitle.isBlank()) {
                return buttonReplyTitle.trim();
            }

            String listReplyTitle = interactive.path("list_reply").path("title").asText("");
            if (listReplyTitle != null && !listReplyTitle.isBlank()) {
                return listReplyTitle.trim();
            }
        }

        return "";
    }

    private WhatsappSendResult sendTextMessage(String to, String body) {
        if (!whatsappEnabled) {
            return new WhatsappSendResult(
                    false,
                    0,
                    null,
                    "WhatsApp Cloud API está desativado por configuração."
            );
        }

        if (isBlank(phoneNumberId) || isBlank(accessToken)) {
            return new WhatsappSendResult(
                    false,
                    0,
                    null,
                    "WhatsApp Cloud API sem phoneNumberId ou accessToken configurado."
            );
        }

        try {
            String endpoint = buildMessagesEndpoint();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", sanitizePhone(to));
            payload.put("type", "text");

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("preview_url", false);
            text.put("body", body);

            payload.put("text", text);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            String providerMessageId = extractProviderMessageId(response.body());

            return new WhatsappSendResult(
                    success,
                    response.statusCode(),
                    providerMessageId,
                    success ? null : response.body()
            );
        } catch (Exception exception) {
            return new WhatsappSendResult(
                    false,
                    0,
                    null,
                    exception.getMessage()
            );
        }
    }

    private String buildMessagesEndpoint() {
        String baseUrl = graphApiBaseUrl == null || graphApiBaseUrl.isBlank()
                ? "https://graph.facebook.com"
                : graphApiBaseUrl.trim();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String version = graphApiVersion == null || graphApiVersion.isBlank()
                ? "v20.0"
                : graphApiVersion.trim();

        return baseUrl + "/" + version + "/" + phoneNumberId.trim() + "/messages";
    }

    private String extractProviderMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messages = root.path("messages");

            if (!messages.isArray() || messages.size() == 0) {
                return null;
            }

            return messages.get(0).path("id").asText(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String sanitizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        return phone.replace("+", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record InboundWhatsappMessage(
            String from,
            String type,
            String body,
            String mediaId,
            String fileName,
            String mimeType
    ) {
    }

    private record InboundWhatsappMedia(
            String mediaId,
            String fileName,
            String mimeType
    ) {
    }

    private record WhatsappSendResult(
            boolean success,
            int statusCode,
            String providerMessageId,
            String errorMessage
    ) {
    }
}
