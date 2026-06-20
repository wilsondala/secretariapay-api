package com.vairapido.api.service;

import com.vairapido.api.dto.whatsappcommand.WhatsappCommandResult;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionResponse;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionStartRequest;
import com.vairapido.api.dto.whatsappwebhook.WhatsappWebhookReceiveResponse;
import com.vairapido.api.entity.enums.UserStatus;
import com.vairapido.api.entity.enums.WhatsappSessionType;
import com.vairapido.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WhatsappWebhookService {

    private final WhatsappSessionService whatsappSessionService;
    private final WhatsappCommandService whatsappCommandService;
    private final UserRepository userRepository;

    @Value("${vairapido.whatsapp.verify-token:vairapido-dev-token}")
    private String verifyToken;

    public WhatsappWebhookService(
            WhatsappSessionService whatsappSessionService,
            WhatsappCommandService whatsappCommandService,
            UserRepository userRepository
    ) {
        this.whatsappSessionService = whatsappSessionService;
        this.whatsappCommandService = whatsappCommandService;
        this.userRepository = userRepository;
    }

    public String verifyWebhook(
            String mode,
            String token,
            String challenge
    ) {
        boolean isSubscribeMode = "subscribe".equals(mode);
        boolean isValidToken = verifyToken != null && verifyToken.equals(token);

        if (!isSubscribeMode || !isValidToken || challenge == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Webhook WhatsApp não autorizado."
            );
        }

        return challenge;
    }

    public WhatsappWebhookReceiveResponse receiveMessage(
            Map<String, Object> payload
    ) {
        Optional<IncomingWhatsappMessage> incomingMessage =
                extractIncomingMessage(payload);

        if (incomingMessage.isEmpty()) {
            return new WhatsappWebhookReceiveResponse()
                    .setProcessed(false)
                    .setReason("Payload recebido, mas sem mensagem de usuário para processar.")
                    .setCommandProcessed(false)
                    .setCommandAllowed(false)
                    .setReceivedAt(LocalDateTime.now());
        }

        IncomingWhatsappMessage message = incomingMessage.get();

        WhatsappSessionType sessionType = resolveSessionType(message.phoneNumber());

        WhatsappSessionStartRequest startRequest = new WhatsappSessionStartRequest()
                .setPhoneNumber(message.phoneNumber())
                .setSessionType(sessionType)
                .setMessageText(message.messageText());

        WhatsappSessionResponse sessionResponse =
                whatsappSessionService.startSession(startRequest);

        WhatsappCommandResult commandResult =
                whatsappCommandService.handleCommand(
                        sessionResponse,
                        message.messageText()
                );

        return new WhatsappWebhookReceiveResponse()
                .setProcessed(true)
                .setReason("Mensagem WhatsApp processada com sucesso.")
                .setPhoneNumber(sessionResponse.getPhoneNumber())
                .setMessageText(message.messageText())
                .setSessionType(sessionResponse.getSessionType())
                .setSessionId(sessionResponse.getId())
                .setCurrentStep(sessionResponse.getCurrentStep())
                .setCommandProcessed(commandResult.getProcessed())
                .setCommandAllowed(commandResult.getAllowed())
                .setCommandName(commandResult.getCommandName())
                .setReplyMessage(commandResult.getReplyMessage())
                .setReceivedAt(LocalDateTime.now());
    }

    private WhatsappSessionType resolveSessionType(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        boolean userExists = userRepository
                .findByWhatsappAndStatus(normalizedPhone, UserStatus.ACTIVE)
                .isPresent();

        return userExists
                ? WhatsappSessionType.USER
                : WhatsappSessionType.PASSENGER;
    }

    private Optional<IncomingWhatsappMessage> extractIncomingMessage(
            Map<String, Object> payload
    ) {
        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }

        Optional<Map<String, Object>> value = firstMap(payload.get("entry"))
                .flatMap(entry -> firstMap(entry.get("changes")))
                .flatMap(change -> asMap(change.get("value")));

        Optional<Map<String, Object>> message = value
                .flatMap(v -> firstMap(v.get("messages")));

        if (message.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> messageMap = message.get();

        String phoneNumber = asString(messageMap.get("from"));
        String messageText = extractMessageText(messageMap);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }

        if (messageText == null || messageText.isBlank()) {
            messageText = "[mensagem sem texto]";
        }

        return Optional.of(
                new IncomingWhatsappMessage(
                        normalizePhoneNumber(phoneNumber),
                        messageText
                )
        );
    }

    private String extractMessageText(Map<String, Object> messageMap) {
        String type = asString(messageMap.get("type"));

        if ("text".equals(type)) {
            return asMap(messageMap.get("text"))
                    .map(text -> asString(text.get("body")))
                    .orElse(null);
        }

        if ("button".equals(type)) {
            return asMap(messageMap.get("button"))
                    .map(button -> asString(button.get("text")))
                    .orElse(null);
        }

        if ("interactive".equals(type)) {
            Optional<Map<String, Object>> interactive =
                    asMap(messageMap.get("interactive"));

            Optional<Map<String, Object>> buttonReply =
                    interactive.flatMap(value -> asMap(value.get("button_reply")));

            if (buttonReply.isPresent()) {
                String title = asString(buttonReply.get().get("title"));
                String id = asString(buttonReply.get().get("id"));

                return title != null ? title : id;
            }

            Optional<Map<String, Object>> listReply =
                    interactive.flatMap(value -> asMap(value.get("list_reply")));

            if (listReply.isPresent()) {
                String title = asString(listReply.get().get("title"));
                String id = asString(listReply.get().get("id"));

                return title != null ? title : id;
            }
        }

        if (type != null && !type.isBlank()) {
            return "[tipo de mensagem: " + type + "]";
        }

        return null;
    }

    private Optional<Map<String, Object>> firstMap(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return Optional.empty();
        }

        return asMap(list.get(0));
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return Optional.of((Map<String, Object>) map);
        }

        return Optional.empty();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "";
        }

        String cleaned = phoneNumber.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    private record IncomingWhatsappMessage(
            String phoneNumber,
            String messageText
    ) {
    }
}