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
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, GuidedFinancialContext> guidedFinancialContexts = new ConcurrentHashMap<>();

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

        Optional<String> financialReply;
        String replyText;
        WhatsappSendResult sendResult;

        WhatsappRecipientOverrideContext.set(message.from());

        try {
            financialReply = financialConversationService.handle(
                    message.from(),
                    message.type(),
                    message.body()
            );

            replyText = financialReply.orElseGet(() -> buildFinancialOnlyReply(message));
            replyText = normalizeInstitutionalReply(replyText, message.body());

            sendResult = sendTextMessage(
                    message.from(),
                    replyText
            );
        } finally {
            WhatsappRecipientOverrideContext.clear();
        }

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
        } else {
            response.put("flow", "SECRETARIAPAY_FINANCIAL_SCOPE_GUARD");
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

            HttpResponse<String> httpResponse = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            boolean success = httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300;
            String providerMessageId = extractProviderMessageId(httpResponse.body());

            return new WhatsappSendResult(
                    success,
                    httpResponse.statusCode(),
                    providerMessageId,
                    success ? null : httpResponse.body()
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

    private String buildFinancialOnlyReply(InboundWhatsappMessage message) {
        String phone = sanitizePhone(message.from());
        String body = message.body() == null ? "" : message.body().trim();
        String normalized = normalize(body);

        if (isMenuIntent(normalized) || isGreetingIntent(normalized)) {
            guidedFinancialContexts.remove(phone);
            return buildFinancialScopeReply();
        }

        GuidedFinancialContext context = guidedFinancialContexts.get(phone);
        if (context != null) {
            return handleGuidedFinancialContext(phone, context, body, normalized);
        }

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

        if (isGuideRequest(normalized) || "1".equals(normalized) || "2".equals(normalized)) {
            guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_STUDENT_IDENTIFIER", "", ""));
            return buildAskStudentForGuideReply();
        }

        if ("3".equals(normalized) || containsAny(normalized, "atraso", "multa", "vencido", "divida", "dívida")) {
            guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_STUDENT_IDENTIFIER", "", "OPEN_CHARGES"));
            return buildAskStudentForOpenChargesReply();
        }

        if ("4".equals(normalized) || containsAny(normalized, "comprovativo", "comprovante", "talao", "talão")) {
            return """
                    Envie o comprovativo em imagem ou PDF por aqui.

                    Essa opção é usada para depósito, pagamento feito em balcão, Multicaixa presencial/TPA ou transferência de outro banco.

                    Após a validação da DCR, o sistema envia o recibo automaticamente no WhatsApp.
                    """.trim();
        }

        if ("5".equals(normalized) || containsAny(normalized, "recibo")) {
            return """
                    Para consultar ou reenviar recibo, envie o número de matrícula ou BI do estudante.

                    Exemplo:
                    IMETRO-2026-TESTE-002
                    """.trim();
        }

        if ("6".equals(normalized) || containsAny(normalized, "situacao financeira", "situação financeira", "estado financeiro")) {
            guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_STUDENT_IDENTIFIER", "", "SUMMARY"));
            return """
                    Para consultar a situação financeira, envie o número de matrícula, BI ou telefone cadastrado.

                    Exemplo:
                    IMETRO-2026-TESTE-002
                    """.trim();
        }

        return buildFinancialScopeReply();
    }

    private String handleGuidedFinancialContext(String phone, GuidedFinancialContext context, String body, String normalized) {
        if ("WAITING_STUDENT_IDENTIFIER".equals(context.step())) {
            if (body.isBlank()) {
                return buildAskStudentForGuideReply();
            }

            if ("OPEN_CHARGES".equals(context.action())) {
                guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_REFERENCE_MONTH", body, "OPEN_CHARGES"));
                return buildReferenceMonthOptions(body, true);
            }

            if ("SUMMARY".equals(context.action())) {
                guidedFinancialContexts.remove(phone);
                return """
                        Obrigado. Vou consultar a situação financeira desse cadastro.

                        Para uma análise completa, solicite a guia ou informe o mês de referência.

                        1. Mês atual
                        2. Mês passado / em atraso
                        3. Escolher outro mês
                        """.trim();
            }

            guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_REFERENCE_MONTH", body, "GUIDE"));
            return buildReferenceMonthOptions(body, false);
        }

        if ("WAITING_REFERENCE_MONTH".equals(context.step())) {
            String reference = resolveReferenceMonthOption(normalized, body);
            if (reference.isBlank()) {
                return buildReferenceMonthOptions(context.studentIdentifier(), "OPEN_CHARGES".equals(context.action()));
            }

            guidedFinancialContexts.put(phone, new GuidedFinancialContext("WAITING_PAYMENT_METHOD", context.studentIdentifier(), reference));
            return buildPaymentMethodOptions(context.studentIdentifier(), reference);
        }

        if ("WAITING_PAYMENT_METHOD".equals(context.step())) {
            if ("0".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                guidedFinancialContexts.remove(phone);
                return buildFinancialScopeReply();
            }

            if ("1".equals(normalized) || containsAny(normalized, "multicaixa express")) {
                guidedFinancialContexts.remove(phone);
                return buildAutomaticPaymentSelectedReply("Multicaixa Express", context.referenceMonth());
            }

            if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
                guidedFinancialContexts.remove(phone);
                return buildReferencePaymentSelectedReply(context.referenceMonth());
            }

            if ("3".equals(normalized) || containsAny(normalized, "transferencia mesmo banco", "transferência mesmo banco", "mesmo banco")) {
                guidedFinancialContexts.remove(phone);
                return buildAutomaticPaymentSelectedReply("Transferência mesmo banco", context.referenceMonth());
            }

            if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "balcao", "balcão", "outro banco", "tpa")) {
                guidedFinancialContexts.remove(phone);
                return buildManualPaymentSelectedReply(context.referenceMonth());
            }

            return buildPaymentMethodOptions(context.studentIdentifier(), context.referenceMonth());
        }

        guidedFinancialContexts.remove(phone);
        return buildFinancialScopeReply();
    }

    private String normalizeInstitutionalReply(String replyText, String inboundText) {
        String reply = replyText == null ? "" : replyText.trim();

        if (reply.startsWith("Boa tarde!")) {
            reply = greetingByCurrentHour() + reply.substring("Boa tarde".length());
        }

        if (isAcademicScopeReply(reply)) {
            reply = buildFinancialScopeReply();
        }

        reply = applyPaymentModesWording(reply);

        if (isCloseMessage(inboundText) && !reply.toLowerCase().contains("obrigado")) {
            reply = reply + "\n\nObrigado por contactar o atendimento financeiro académico do IMETRO. Estamos à disposição.";
        }

        return reply;
    }

    private String applyPaymentModesWording(String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }

        String updated = reply;

        if (updated.contains("Formas de pagamento disponíveis:")) {
            int start = updated.indexOf("Formas de pagamento disponíveis:");
            int end = updated.indexOf("Quando receber", start);
            if (end < 0) {
                end = updated.indexOf("Precisa de mais", start);
            }

            if (end > start) {
                updated = updated.substring(0, start).trim()
                        + "\n\n"
                        + buildPaymentModesBlock()
                        + "\n\n"
                        + updated.substring(end).trim();
            } else {
                updated = updated.substring(0, start).trim()
                        + "\n\n"
                        + buildPaymentModesBlock();
            }
        }

        updated = updated.replace(
                "Após o pagamento, envie o comprovativo para validação da DCR.",
                "Multicaixa Express, Pagamento por Referência e Transferência mesmo banco têm recibo automático quando o pagamento é confirmado pelo sistema. Depósito, pagamento em balcão, Multicaixa presencial/TPA e transferência de outro banco exigem comprovativo e validação da DCR."
        );

        updated = updated.replace(
                "O recibo institucional será emitido somente após validação manual da DCR.",
                "O recibo institucional será emitido automaticamente quando o pagamento for confirmado pelo método escolhido."
        );

        return updated;
    }

    private String buildFinancialScopeReply() {
        return ("""
                %s 👋
                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Escolha uma opção respondendo com o número ou escrevendo o nome da opção:

                1. Propinas e mensalidades
                2. Guia de pagamento
                3. Pagamentos em atraso e multas
                4. Enviar comprovativo
                5. Recibos
                6. Situação financeira

                Exemplo: responda 2 ou escreva guia.

                Para outros assuntos académicos ou administrativos, contacte a secretaria académica ou o setor responsável do IMETRO.
                """).formatted(greetingByCurrentHour()).trim();
    }

    private String buildAskStudentForGuideReply() {
        return """
                Perfeito. Vou iniciar a emissão da guia de pagamento.

                Envie o número de matrícula, BI ou telefone cadastrado do estudante.

                Exemplo:
                IMETRO-2026-TESTE-002
                """.trim();
    }

    private String buildAskStudentForOpenChargesReply() {
        return """
                Para consultar atraso, multa ou mensalidades em aberto, envie o número de matrícula, BI ou telefone cadastrado.

                Exemplo:
                IMETRO-2026-TESTE-002
                """.trim();
    }

    private String buildReferenceMonthOptions(String studentIdentifier, boolean openCharges) {
        return ("""
                Cadastro informado: %s

                Informe o mês de referência:

                1. Mês atual
                2. Mês passado / em atraso
                3. Escolher outro mês

                Você pode responder com o número ou escrever, por exemplo: mês atual, mês passado ou 06/2026.
                """).formatted(studentIdentifier).trim();
    }

    private String buildPaymentMethodOptions(String studentIdentifier, String referenceMonth) {
        return ("""
                Guia preparada.

                Cadastro: %s
                Mês de referência: %s

                Escolha a forma de pagamento:

                1. Multicaixa Express
                   Pagamento automático. O recibo é enviado após confirmação.

                2. Pagamento por Referência
                   Pagamento automático. O sistema envia entidade, referência e valor.

                3. Transferência mesmo banco
                   Pagamento automático no ambiente de teste.

                4. Depósito, balcão, Multicaixa presencial/TPA ou transferência de outro banco
                   Envie o comprovativo. A DCR valida antes do recibo.

                Responda com o número ou escreva a forma de pagamento.
                """).formatted(studentIdentifier, referenceMonth).trim();
    }

    private String buildAutomaticPaymentSelectedReply(String methodName, String referenceMonth) {
        return ("""
                %s selecionado.

                Mês de referência: %s

                Essa forma de pagamento é automática no teste.
                Assim que o pagamento for confirmado pelo sistema, o recibo será enviado automaticamente no WhatsApp.

                Obrigado por utilizar o atendimento financeiro académico do IMETRO.
                """).formatted(methodName, referenceMonth).trim();
    }

    private String buildReferencePaymentSelectedReply(String referenceMonth) {
        return ("""
                Pagamento por Referência selecionado.

                Mês de referência: %s

                O sistema deverá enviar entidade, referência e valor da guia.
                Após a confirmação do pagamento, o recibo será enviado automaticamente no WhatsApp.

                Obrigado por utilizar o atendimento financeiro académico do IMETRO.
                """).formatted(referenceMonth).trim();
    }

    private String buildManualPaymentSelectedReply(String referenceMonth) {
        return ("""
                Modalidade com comprovativo selecionada.

                Mês de referência: %s

                Após pagar por depósito, balcão, Multicaixa presencial/TPA ou transferência de outro banco, envie o comprovativo em imagem ou PDF.

                A DCR fará a validação e, após aprovação, o sistema enviará o recibo automaticamente no WhatsApp.
                """).formatted(referenceMonth).trim();
    }

    private String buildPaymentModesBlock() {
        return """
                Formas de pagamento:

                1. Multicaixa Express
                   Pagamento automático.
                   Após a confirmação, o sistema envia o recibo no WhatsApp.

                2. Pagamento por Referência
                   Pagamento automático.
                   O sistema gera entidade, referência e valor.
                   Após a confirmação, o sistema envia o recibo no WhatsApp.

                3. Transferência mesmo banco
                   Pagamento automático no ambiente de teste.
                   O aluno é identificado pelo WhatsApp cadastrado e o recibo é enviado automaticamente.

                4. Depósito, balcão, Multicaixa presencial ou transferência de outro banco
                   Envie o comprovativo em imagem/PDF.
                   A DCR valida e, após aprovação, o sistema envia o recibo no WhatsApp.
                """.trim();
    }

    private boolean isAcademicScopeReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return false;
        }

        String lower = reply.toLowerCase();
        return lower.contains("outras solicitações académicas")
                || lower.contains("outras solicitacoes academicas")
                || lower.contains("declaração de frequência")
                || lower.contains("declaracao de frequencia")
                || lower.contains("certificado")
                || lower.contains("histórico escolar")
                || lower.contains("historico escolar")
                || lower.contains("requerimento académico")
                || lower.contains("requerimento academico");
    }

    private String greetingByCurrentHour() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) {
            return "Bom dia";
        }
        if (hour >= 12 && hour < 18) {
            return "Boa tarde";
        }
        return "Boa noite";
    }

    private String resolveReferenceMonthOption(String normalized, String raw) {
        if ("1".equals(normalized) || containsAny(normalized, "mes atual", "mês atual", "atual")) {
            return "mês atual";
        }
        if ("2".equals(normalized) || containsAny(normalized, "mes passado", "mês passado", "atraso", "atrasado")) {
            return "mês passado / em atraso";
        }
        if ("3".equals(normalized) || containsAny(normalized, "outro", "escolher")) {
            return "outro mês informado manualmente";
        }
        if (raw != null && raw.matches("(?i).*\\b(0?[1-9]|1[0-2])[/.-]20\\d{2}\\b.*")) {
            return raw.trim();
        }
        return "";
    }

    private boolean isGuideRequest(String normalized) {
        return containsAny(normalized, "guia", "boleto", "referencia", "referência", "quero pagar", "pagar", "propina", "mensalidade");
    }

    private boolean isGreetingIntent(String normalized) {
        return containsAny(normalized, "ola", "olá", "bom dia", "boa tarde", "boa noite", "oi");
    }

    private boolean isMenuIntent(String normalized) {
        return containsAny(normalized, "menu", "inicio", "início", "opcoes", "opções", "voltar");
    }

    private boolean isCloseMessage(String value) {
        if (value == null) {
            return false;
        }

        String lower = value.toLowerCase().trim();
        return "0".equals(lower)
                || lower.contains("encerrar")
                || lower.contains("sair")
                || lower.contains("terminar")
                || lower.contains("finalizar");
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank()) return false;
        for (String term : terms) {
            if (value.contains(normalize(term))) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase().trim().replaceAll("\\s+", " ");
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

    private record GuidedFinancialContext(
            String step,
            String studentIdentifier,
            String action
    ) {
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
