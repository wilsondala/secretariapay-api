package com.secretariapay.api.service.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
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
import java.util.Locale;
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
    private final SecretariaPayWhatsappAcademicSupportService academicSupportService;
    private final SecretariaPayWhatsappFinancialDemoConversationService financialConversationService;
    private final WhatsappInteractiveMenuFactory interactiveMenuFactory;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SecretariaPayWhatsappWebhookService(
            @Value("${secretariapay.whatsapp.verify-token:secretariapay-dev-token}") String verifyToken,
            @Value("${secretariapay.whatsapp.enabled:false}") boolean whatsappEnabled,
            @Value("${secretariapay.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${secretariapay.whatsapp.access-token:}") String accessToken,
            @Value("${secretariapay.whatsapp.graph-api-version:v20.0}") String graphApiVersion,
            @Value("${secretariapay.whatsapp.graph-api-base-url:https://graph.facebook.com}") String graphApiBaseUrl,
            SecretariaPayWhatsappBrainService brainService,
            SecretariaPayWhatsappAcademicSupportService academicSupportService,
            SecretariaPayWhatsappFinancialDemoConversationService financialConversationService,
            WhatsappInteractiveMenuFactory interactiveMenuFactory,
            WhatsAppCloudApiClient whatsAppCloudApiClient
    ) {
        this.verifyToken = verifyToken;
        this.whatsappEnabled = whatsappEnabled;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.graphApiVersion = graphApiVersion;
        this.graphApiBaseUrl = graphApiBaseUrl;
        this.academicSupportService = academicSupportService;
        this.financialConversationService = financialConversationService;
        this.interactiveMenuFactory = interactiveMenuFactory;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String verifyWebhook(String mode, String token, String challenge) {
        boolean isSubscribeMode = "subscribe".equals(mode);
        boolean isValidToken = verifyToken != null && !verifyToken.isBlank() && verifyToken.equals(token);

        if (!isSubscribeMode || !isValidToken || challenge == null || challenge.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Webhook SecretáriaPay WhatsApp não autorizado.");
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
            response.put("reason", "Payload recebido, mas sem mensagem de usuário para responder.");
            return response;
        }

        InboundWhatsappMessage message = inboundMessage.get();
        String replyText;
        WhatsappSendResult sendResult;
        boolean interactiveAttempted = false;
        boolean interactiveSent = false;

        WhatsappRecipientOverrideContext.set(message.from());
        try {
            Optional<String> financialReply = financialConversationService.handle(message.from(), message.type(), message.body());
            replyText = financialReply.orElseGet(() -> buildMediaOrScopeReply(message));
            replyText = sanitizeInstitutionalLanguage(replyText);

            Optional<WhatsappInteractiveListMessage> interactiveMessage = interactiveMenuFactory.fromReplyText(replyText);
            if (interactiveMessage.isPresent()) {
                interactiveAttempted = true;
                WhatsAppCloudSendResult interactiveResult = whatsAppCloudApiClient.sendInteractiveList(message.from(), interactiveMessage.get());
                interactiveSent = interactiveResult.isSuccess();
                if (interactiveSent) {
                    sendResult = toWebhookSendResult(interactiveResult);
                } else {
                    sendResult = sendTextMessage(message.from(), interactiveMessage.get().fallbackText());
                }
            } else {
                sendResult = sendTextMessage(message.from(), replyText);
            }
        } finally {
            WhatsappRecipientOverrideContext.clear();
        }

        response.put("processed", true);
        response.put("status", sendResult.success() ? "AUTO_REPLY_SENT" : "AUTO_REPLY_FAILED");
        response.put("flow", "SECRETARIAPAY_FINANCIAL_DEMO_CONVERSATION_ROUTER");
        response.put("from", message.from());
        response.put("messageType", message.type());
        response.put("messageText", message.body());
        response.put("replyText", replyText);
        response.put("replyType", interactiveSent ? "INTERACTIVE_LIST" : "TEXT");
        response.put("interactiveAttempted", interactiveAttempted);
        response.put("interactiveSent", interactiveSent);
        response.put("replySent", sendResult.success());
        response.put("providerStatusCode", sendResult.statusCode());

        if (message.mediaId() != null && !message.mediaId().isBlank()) response.put("mediaId", message.mediaId());
        if (message.fileName() != null && !message.fileName().isBlank()) response.put("fileName", message.fileName());
        if (message.mimeType() != null && !message.mimeType().isBlank()) response.put("mimeType", message.mimeType());
        if (sendResult.providerMessageId() != null && !sendResult.providerMessageId().isBlank()) response.put("providerMessageId", sendResult.providerMessageId());
        if (sendResult.errorMessage() != null && !sendResult.errorMessage().isBlank()) response.put("errorMessage", sendResult.errorMessage());

        return response;
    }

    private WhatsappSendResult toWebhookSendResult(WhatsAppCloudSendResult result) {
        return new WhatsappSendResult(
                result != null && result.isSuccess(),
                result == null || result.getHttpStatus() == null ? 0 : result.getHttpStatus(),
                result == null ? null : result.getProviderMessageId(),
                result == null ? "Resultado do WhatsApp vazio." : result.getErrorMessage()
        );
    }

    private String buildMediaOrScopeReply(InboundWhatsappMessage message) {
        if (message.mediaId() != null && !message.mediaId().isBlank()) {
            return academicSupportService.buildDatabaseAwareReply(
                            message.from(),
                            message.type(),
                            message.body(),
                            message.mediaId(),
                            message.fileName(),
                            message.mimeType()
                    )
                    .orElseGet(this::buildFinancialScopeReply);
        }
        return buildFinancialScopeReply();
    }

    private String buildFinancialScopeReply() {
        return """
                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Posso ajudar com propinas, guias de pagamento, atrasos, multas, borderô financeiro, comprovativos e situação financeira.

                Para começar, responda menu ou escolha uma opção de 1 a 6.
                """.trim();
    }

    private Optional<InboundWhatsappMessage> extractInboundMessage(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return Optional.empty();
        JsonNode root = objectMapper.valueToTree(payload);
        JsonNode entries = root.path("entry");
        if (!entries.isArray()) return Optional.empty();

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) continue;
            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                JsonNode messages = value.path("messages");
                if (!messages.isArray() || messages.size() == 0) continue;
                for (JsonNode message : messages) {
                    String from = message.path("from").asText("");
                    String type = message.path("type").asText("");
                    if (from == null || from.isBlank()) continue;
                    InboundWhatsappMedia media = extractMediaPayload(message, type);
                    String body = extractMessageBody(message, type, media);
                    return Optional.of(new InboundWhatsappMessage(from.trim(), type == null || type.isBlank() ? "unknown" : type.trim(), body, media.mediaId(), media.fileName(), media.mimeType()));
                }
            }
        }
        return Optional.empty();
    }

    private InboundWhatsappMedia extractMediaPayload(JsonNode message, String type) {
        if ("image".equalsIgnoreCase(type)) {
            JsonNode image = message.path("image");
            return new InboundWhatsappMedia(image.path("id").asText(""), "", image.path("mime_type").asText(""));
        }
        if ("document".equalsIgnoreCase(type)) {
            JsonNode document = message.path("document");
            return new InboundWhatsappMedia(document.path("id").asText(""), document.path("filename").asText(""), document.path("mime_type").asText(""));
        }
        if ("audio".equalsIgnoreCase(type)) {
            JsonNode audio = message.path("audio");
            return new InboundWhatsappMedia(audio.path("id").asText(""), "", audio.path("mime_type").asText(""));
        }
        if ("video".equalsIgnoreCase(type)) {
            JsonNode video = message.path("video");
            return new InboundWhatsappMedia(video.path("id").asText(""), "", video.path("mime_type").asText(""));
        }
        return new InboundWhatsappMedia("", "", "");
    }

    private String extractMessageBody(JsonNode message, String type, InboundWhatsappMedia media) {
        if ("text".equalsIgnoreCase(type)) return message.path("text").path("body").asText("").trim();
        if ("image".equalsIgnoreCase(type)) return "[imagem recebida]";
        if ("audio".equalsIgnoreCase(type)) return "[áudio recebido]";
        if ("video".equalsIgnoreCase(type)) return "[vídeo recebido]";
        if ("button".equalsIgnoreCase(type)) return message.path("button").path("text").asText("").trim();
        if ("document".equalsIgnoreCase(type)) {
            String filename = media.fileName();
            return filename == null || filename.isBlank() ? "[documento recebido]" : "[documento recebido: " + filename + "]";
        }
        if ("interactive".equalsIgnoreCase(type)) {
            JsonNode interactive = message.path("interactive");

            String buttonReplyId = interactive.path("button_reply").path("id").asText("");
            if (buttonReplyId != null && !buttonReplyId.isBlank()) return buttonReplyId.trim();
            String buttonReplyTitle = interactive.path("button_reply").path("title").asText("");
            if (buttonReplyTitle != null && !buttonReplyTitle.isBlank()) return buttonReplyTitle.trim();

            String listReplyId = interactive.path("list_reply").path("id").asText("");
            if (listReplyId != null && !listReplyId.isBlank()) return listReplyId.trim();
            String listReplyTitle = interactive.path("list_reply").path("title").asText("");
            if (listReplyTitle != null && !listReplyTitle.isBlank()) return listReplyTitle.trim();
        }
        return "";
    }

    private WhatsappSendResult sendTextMessage(String to, String body) {
        if (!whatsappEnabled) return new WhatsappSendResult(false, 0, null, "WhatsApp Cloud API está desativado por configuração.");
        if (isBlank(phoneNumberId) || isBlank(accessToken)) return new WhatsappSendResult(false, 0, null, "WhatsApp Cloud API sem phoneNumberId ou accessToken configurado.");

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", sanitizePhone(to));
            payload.put("type", "text");

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("preview_url", false);
            text.put("body", sanitizeInstitutionalLanguage(body));
            payload.put("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMessagesEndpoint()))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300;
            return new WhatsappSendResult(success, httpResponse.statusCode(), extractProviderMessageId(httpResponse.body()), success ? null : httpResponse.body());
        } catch (Exception exception) {
            return new WhatsappSendResult(false, 0, null, exception.getMessage());
        }
    }

    private String buildMessagesEndpoint() {
        String baseUrl = graphApiBaseUrl == null || graphApiBaseUrl.isBlank() ? "https://graph.facebook.com" : graphApiBaseUrl.trim();
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String version = graphApiVersion == null || graphApiVersion.isBlank() ? "v20.0" : graphApiVersion.trim();
        return baseUrl + "/" + version + "/" + phoneNumberId.trim() + "/messages";
    }

    private String extractProviderMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode messages = objectMapper.readTree(responseBody).path("messages");
            if (!messages.isArray() || messages.size() == 0) return null;
            return messages.get(0).path("id").asText(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String sanitizeInstitutionalLanguage(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;

        String sanitized = value
                .replaceAll("(?iu)PDF da guia oficial", "PDF da guia de pagamento")
                .replaceAll("(?iu)guia oficial", "guia de pagamento")
                .replaceAll("(?iu)comprovativos oficiais", "comprovativos de pagamento")
                .replaceAll("(?iu)comprovativo oficial", "comprovativo de pagamento")
                .replaceAll("(?iu)recibo oficial", "recibo de pagamento")
                .replaceAll("(?iu)documento oficial", "documento financeiro");

        String normalized = sanitized.toLowerCase(Locale.ROOT);
        boolean consolidatedBordereauReply = normalized.contains("comprovativo reenviado com sucesso")
                || normalized.contains("o comprovativo de pagamento foi emitido em pdf");

        if (consolidatedBordereauReply) {
            sanitized = sanitized
                    .replaceAll("(?iu)comprovativo reenviado com sucesso", "borderô financeiro consolidado emitido com sucesso")
                    .replaceAll(
                            "(?iu)📄 O comprovativo de pagamento foi emitido em PDF\\.",
                            "📄 O borderô financeiro consolidado foi emitido em PDF.\\n\\nO documento reúne todos os pagamentos validados deste estudante e será atualizado a cada novo pagamento confirmado."
                    )
                    .replaceAll("(?m)^Forma de pagamento:\\s*", "Forma do último pagamento: ")
                    .replaceAll("(?m)^Referência:\\s*", "Última referência atualizada: ")
                    .replaceAll("(?m)^Valor pago:\\s*", "Valor do último pagamento: ")
                    .replaceAll("(?m)^Comprovativo:\\s*", "Código do último registo: ");
        }

        return sanitized;
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        return phone.replace("+", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record InboundWhatsappMessage(String from, String type, String body, String mediaId, String fileName, String mimeType) {
    }

    private record InboundWhatsappMedia(String mediaId, String fileName, String mimeType) {
    }

    private record WhatsappSendResult(boolean success, int statusCode, String providerMessageId, String errorMessage) {
    }
}
