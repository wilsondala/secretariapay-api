package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class SecretariaPayWhatsappFinancialDemoConversationService extends SecretariaPayWhatsappFinancialConversationService {

    private static final int SESSION_MINUTES = 60;
    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private final Map<String, DemoSession> sessions = new ConcurrentHashMap<>();
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    public SecretariaPayWhatsappFinancialDemoConversationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptRepository receiptRepository,
            WhatsappSessionRepository sessionRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService
    ) {
        super(studentRepository, chargeRepository, receiptRepository, sessionRepository, whatsAppCloudApiClient, mockAutomaticPaymentService);
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
    }

    @Override
    public Optional<String> handle(String fromPhone, String messageType, String rawMessage) {
        String phone = sanitizePhone(fromPhone);
        String type = safe(messageType).toLowerCase();
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        clearExpired(phone);

        if (isClose(normalized)) {
            sessions.remove(phone);
            return Optional.of("""
                    Atendimento financeiro encerrado.

                    Obrigado por contactar o atendimento financeiro académico do IMETRO. Estamos à disposição.
                    """.trim());
        }

        if (isMedia(type) || normalized.contains("imagem recebida") || normalized.contains("documento recebido")) {
            DemoSession current = sessions.get(phone);
            if (current != null && "WAITING_PROOF".equals(current.step())) {
                sessions.remove(phone);
                return Optional.of(buildProofApprovedReceipt(phone, current));
            }

            return Optional.of("""
                    Comprovativo recebido.

                    Esta opção é usada para depósito, pagamento em balcão, Multicaixa presencial/TPA ou transferência de outro banco.

                    Em demonstração, o comprovativo será encaminhado para validação da DCR.
                    Após aprovação, o sistema envia o recibo automaticamente no WhatsApp.
                    """.trim());
        }

        if (isMenu(normalized) || isGreeting(normalized)) {
            sessions.remove(phone);
            return Optional.of(buildMainMenu());
        }

        DemoSession session = sessions.get(phone);
        if (session != null) {
            return Optional.of(handleSession(phone, session, message, normalized));
        }

        if ("1".equals(normalized) || containsAny(normalized, "propina", "mensalidade", "mensalidades")) {
            sessions.put(phone, DemoSession.waitingStudent("MONTHLY"));
            return Optional.of(askStudent("Vou consultar a propina/mensalidade."));
        }

        if ("2".equals(normalized) || containsAny(normalized, "guia", "boleto", "pagar", "referencia", "referência")) {
            sessions.put(phone, DemoSession.waitingStudent("GUIDE"));
            return Optional.of(askStudent("Perfeito. Vou iniciar a emissão da guia de pagamento."));
        }

        if ("3".equals(normalized) || containsAny(normalized, "atraso", "multa", "vencido", "divida", "dívida")) {
            sessions.put(phone, DemoSession.waitingStudent("OVERDUE"));
            return Optional.of(askStudent("Vou consultar pagamentos em atraso, multas ou mensalidades em aberto."));
        }

        if ("4".equals(normalized) || containsAny(normalized, "comprovativo", "comprovante", "talao", "talão")) {
            sessions.put(phone, DemoSession.waitingProof("comprovativo avulso", "sem mês informado", "manual"));
            return Optional.of(askProof());
        }

        if ("5".equals(normalized) || containsAny(normalized, "recibo")) {
            sessions.put(phone, DemoSession.waitingStudent("RECEIPT"));
            return Optional.of(askStudent("Para consultar ou reenviar recibo, preciso identificar o estudante."));
        }

        if ("6".equals(normalized) || containsAny(normalized, "situacao financeira", "situação financeira", "estado financeiro")) {
            sessions.put(phone, DemoSession.waitingStudent("SUMMARY"));
            return Optional.of(askStudent("Vou consultar a situação financeira."));
        }

        return Optional.of(buildOutOfScope());
    }

    private String handleSession(String phone, DemoSession session, String message, String normalized) {
        if ("WAITING_STUDENT".equals(session.step())) {
            if (message.isBlank()) return askStudent("Não consegui identificar o cadastro.");

            if ("RECEIPT".equals(session.action())) {
                sessions.remove(phone);
                return buildReceiptLookup(phone, message);
            }

            if ("SUMMARY".equals(session.action())) {
                sessions.remove(phone);
                return buildSummary(message);
            }

            sessions.put(phone, DemoSession.waitingMonth(session.action(), message));
            return askReferenceMonth(message);
        }

        if ("WAITING_MONTH".equals(session.step())) {
            String reference = resolveReferenceMonth(normalized, message);
            if (reference.isBlank()) return askReferenceMonth(session.studentIdentifier());

            sessions.put(phone, DemoSession.waitingPayment(session.action(), session.studentIdentifier(), reference));
            return askPaymentMethod(session.studentIdentifier(), reference, "OVERDUE".equals(session.action()));
        }

        if ("WAITING_PAYMENT".equals(session.step())) {
            if ("0".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }

            if ("1".equals(normalized) || containsAny(normalized, "multicaixa express", "express")) {
                sessions.put(phone, DemoSession.waitingAutoSimulation(session.studentIdentifier(), session.referenceMonth(), "Multicaixa Express"));
                return buildGuideAndAskAutoSimulation(phone, session, "Multicaixa Express");
            }

            if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
                sessions.put(phone, DemoSession.waitingAutoSimulation(session.studentIdentifier(), session.referenceMonth(), "Pagamento por Referência"));
                return buildGuideAndAskAutoSimulation(phone, session, "Pagamento por Referência");
            }

            if ("3".equals(normalized) || containsAny(normalized, "mesmo banco", "transferencia", "transferência")) {
                sessions.put(phone, DemoSession.waitingAutoSimulation(session.studentIdentifier(), session.referenceMonth(), "Transferência mesmo banco"));
                return buildGuideAndAskAutoSimulation(phone, session, "Transferência mesmo banco");
            }

            if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "balcao", "balcão", "outro banco", "tpa", "presencial")) {
                sessions.put(phone, DemoSession.waitingManualSimulation(session.studentIdentifier(), session.referenceMonth(), "Depósito/balcão/outro banco"));
                return buildGuideAndAskManualPayment(phone, session);
            }

            return askPaymentMethod(session.studentIdentifier(), session.referenceMonth(), "OVERDUE".equals(session.action()));
        }

        if ("WAITING_AUTO_SIMULATION".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "simular", "confirmar", "pago", "paguei", "sucesso")) {
                sessions.remove(phone);
                return buildAutomaticReceipt(phone, session);
            }

            return ("""
                    Guia demonstrativa emitida para %s.

                    Para concluir a demonstração, responda:
                    1. Simular pagamento confirmado
                    """).formatted(session.paymentMethod()).trim();
        }

        if ("WAITING_MANUAL_PAYMENT".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "simular", "paguei", "deposito", "depósito", "feito")) {
                sessions.put(phone, DemoSession.waitingProof(session.studentIdentifier(), session.referenceMonth(), session.paymentMethod()));
                return askProofAfterManualPayment(session);
            }

            return """
                    Guia demonstrativa emitida para pagamento com comprovativo.

                    Para continuar, responda:
                    1. Simular pagamento realizado
                    """.trim();
        }

        if ("WAITING_PROOF".equals(session.step())) {
            return askProofAfterManualPayment(session);
        }

        sessions.remove(phone);
        return buildMainMenu();
    }

    private String buildMainMenu() {
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
                """).formatted(greeting()).trim();
    }

    private String askStudent(String intro) {
        return ("""
                %s

                Envie o número de matrícula, BI ou telefone cadastrado do estudante.

                Exemplo:
                IMETRO-2026-TESTE-002
                """).formatted(intro).trim();
    }

    private String askReferenceMonth(String student) {
        return ("""
                Cadastro informado: %s

                Informe o mês de referência:

                1. Mês atual
                2. Mês passado / em atraso
                3. Escolher outro mês

                Você pode responder com o número ou escrever, por exemplo: mês atual, mês passado ou 06/2026.
                """).formatted(student).trim();
    }

    private String askPaymentMethod(String student, String reference, boolean overdue) {
        String fine = overdue ? "\nO sistema demonstrativo considera multa no total da guia para mensalidade em atraso.\n" : "";
        return ("""
                Guia preparada.%s
                Cadastro: %s
                Mês de referência: %s
                Valor demonstrativo: 45.000,00 Kz

                Escolha a forma de pagamento:

                1. Multicaixa Express
                   Pagamento de confirmação automática. O recibo será enviado após a simulação.

                2. Pagamento por Referência
                   Pagamento de confirmação automática. O sistema mostra entidade, referência e valor.

                3. Transferência mesmo banco
                   Pagamento de confirmação automática no teste.

                4. Depósito, balcão, Multicaixa presencial/TPA ou transferência de outro banco
                   Exige comprovativo. Na demonstração, o comprovativo será aprovado automaticamente.

                Responda com o número ou escreva a forma de pagamento.
                """).formatted(fine, student, reference).trim();
    }

    private String buildGuideAndAskAutoSimulation(String phone, DemoSession session, String method) {
        sendDemoGuidePdf(phone, session.studentIdentifier(), session.referenceMonth(), method);

        String referenceBlock = "Pagamento por Referência".equals(method)
                ? "\nEntidade: 00348\nReferência: 205114879\nValor: 45.000,00 Kz\n"
                : "";

        return ("""
                Guia demonstrativa emitida e enviada em PDF.

                Cadastro: %s
                Mês: %s
                Forma de pagamento: %s
                Valor: 45.000,00 Kz
                %s
                Para demonstrar o fluxo automático, responda:
                1. Simular pagamento confirmado

                Após a simulação, o sistema enviará o recibo/comprovativo de pagamento em PDF e encerrará o atendimento com agradecimento.
                """).formatted(session.studentIdentifier(), session.referenceMonth(), method, referenceBlock).trim();
    }

    private String buildGuideAndAskManualPayment(String phone, DemoSession session) {
        sendDemoGuidePdf(phone, session.studentIdentifier(), session.referenceMonth(), "Depósito/balcão/outro banco");

        return ("""
                Guia demonstrativa emitida e enviada em PDF.

                Cadastro: %s
                Mês: %s
                Forma de pagamento: Depósito/balcão/Multicaixa presencial/TPA/outro banco
                Valor: 45.000,00 Kz

                Para demonstrar o fluxo com DCR, responda:
                1. Simular pagamento realizado

                Depois o sistema solicitará o envio do comprovativo em imagem ou PDF.
                """).formatted(session.studentIdentifier(), session.referenceMonth()).trim();
    }

    private String askProofAfterManualPayment(DemoSession session) {
        return """
                Pagamento demonstrativo realizado.

                Agora envie o comprovativo em imagem ou PDF aqui no WhatsApp.

                Na demonstração, após receber o comprovativo, o sistema simulará a aprovação da DCR e enviará o recibo em PDF automaticamente.
                """.trim();
    }

    private String askProof() {
        return """
                Envie o comprovativo em imagem ou PDF por aqui.

                Essa opção é usada para depósito, pagamento em balcão, Multicaixa presencial/TPA ou transferência de outro banco.

                Na demonstração, após receber o comprovativo, o sistema simulará a aprovação da DCR e enviará o recibo em PDF automaticamente.
                """.trim();
    }

    private String buildAutomaticReceipt(String phone, DemoSession session) {
        String receipt = receiptCode();
        sendDemoReceiptPdf(phone, session.studentIdentifier(), session.referenceMonth(), session.paymentMethod(), receipt);

        return ("""
                ✅ Pagamento confirmado.

                Forma de pagamento: %s
                Mês de referência: %s
                Valor pago: 45.000,00 Kz
                Recibo: %s

                O recibo/comprovativo de pagamento foi gerado e enviado em PDF nesta demonstração.

                Obrigado por utilizar o atendimento financeiro académico do IMETRO. Estamos à disposição.
                """).formatted(session.paymentMethod(), session.referenceMonth(), receipt).trim();
    }

    private String buildProofApprovedReceipt(String phone, DemoSession session) {
        String receipt = receiptCode();
        sendDemoReceiptPdf(phone, session.studentIdentifier(), session.referenceMonth(), session.paymentMethod(), receipt);

        return ("""
                ✅ Comprovativo recebido e aprovado na demonstração.

                Forma de pagamento: %s
                Mês de referência: %s
                Valor pago: 45.000,00 Kz
                Recibo: %s

                Na operação real, a DCR valida o comprovativo antes da emissão do recibo.
                Nesta demonstração, a aprovação foi simulada automaticamente e o recibo foi enviado em PDF.

                Obrigado por utilizar o atendimento financeiro académico do IMETRO. Estamos à disposição.
                """).formatted(session.paymentMethod(), session.referenceMonth(), receipt).trim();
    }

    private String buildReceiptLookup(String phone, String student) {
        String receipt = receiptCode();
        sendDemoReceiptPdf(phone, student, "consulta", "Reenvio de recibo", receipt);
        return ("""
                Cadastro informado: %s

                Recibo demonstrativo encontrado e enviado em PDF:
                Recibo: %s
                Estado: Pagamento confirmado

                Obrigado por contactar o atendimento financeiro académico do IMETRO.
                """).formatted(student, receipt).trim();
    }

    private String buildSummary(String student) {
        return ("""
                Situação financeira demonstrativa.

                Cadastro: %s
                Estado: Regular no ambiente de demonstração
                Último recibo: %s

                Para emitir nova guia, responda 2 ou escreva guia.
                """).formatted(student, receiptCode()).trim();
    }

    private String buildOutOfScope() {
        return """
                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Posso ajudar com propinas, guias de pagamento, atrasos, multas, comprovativos, recibos e situação financeira.

                Para começar, responda menu ou escolha uma opção de 1 a 6.
                """.trim();
    }

    private void sendDemoGuidePdf(String phone, String student, String month, String method) {
        String code = "GUIDE-DEMO-" + shortId();
        String url = API_BASE_URL + "/api/v1/public/demo/payment-guides/" + encode(code) + "/pdf"
                + "?student=" + encode(student)
                + "&month=" + encode(month)
                + "&method=" + encode(method);
        String caption = ("""
                Guia de pagamento demonstrativa.

                Cadastro: %s
                Mês: %s
                Forma de pagamento: %s
                Valor: 45.000,00 Kz
                """).formatted(student, month, method).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, url, "guia-pagamento-" + code + ".pdf", caption);
    }

    private void sendDemoReceiptPdf(String phone, String student, String month, String method, String receipt) {
        String url = API_BASE_URL + "/api/v1/public/demo/receipts/" + encode(receipt) + "/pdf"
                + "?student=" + encode(student)
                + "&month=" + encode(month)
                + "&method=" + encode(method);
        String caption = ("""
                Recibo de pagamento demonstrativo.

                Recibo: %s
                Cadastro: %s
                Mês: %s
                Valor: 45.000,00 Kz
                """).formatted(receipt, student, month).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, url, "recibo-" + receipt + ".pdf", caption);
    }

    private String resolveReferenceMonth(String normalized, String raw) {
        if ("1".equals(normalized) || containsAny(normalized, "mes atual", "mês atual", "atual")) return "mês atual";
        if ("2".equals(normalized) || containsAny(normalized, "mes passado", "mês passado", "atraso", "atrasado")) return "mês passado / em atraso";
        if ("3".equals(normalized) || containsAny(normalized, "outro", "escolher")) return "outro mês informado manualmente";
        if (raw != null && raw.matches("(?i).*\\b(0?[1-9]|1[0-2])[/.-]20\\d{2}\\b.*")) return raw.trim();
        return "";
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Bom dia";
        if (hour >= 12 && hour < 18) return "Boa tarde";
        return "Boa noite";
    }

    private String receiptCode() {
        return "RCT-DEMO-" + shortId();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private boolean isMedia(String type) {
        return "image".equalsIgnoreCase(type) || "document".equalsIgnoreCase(type);
    }

    private boolean isGuideRequest(String value) {
        return containsAny(value, "guia", "boleto", "referencia", "referência", "quero pagar", "pagar", "propina", "mensalidade");
    }

    private boolean isMenu(String value) {
        return containsAny(value, "menu", "inicio", "início", "opcoes", "opções", "voltar");
    }

    private boolean isGreeting(String value) {
        return containsAny(value, "ola", "olá", "oi", "bom dia", "boa tarde", "boa noite");
    }

    private boolean isClose(String value) {
        return "0".equals(value) || containsAny(value, "encerrar", "finalizar", "terminar", "sair", "fim");
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank()) return false;
        for (String term : terms) if (value.contains(normalize(term))) return true;
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String sanitizePhone(String value) {
        if (value == null) return "";
        return value.replaceAll("[^0-9]", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void clearExpired(String phone) {
        DemoSession session = sessions.get(phone);
        if (session != null && session.expiresAt().isBefore(LocalDateTime.now())) sessions.remove(phone);
    }

    private record DemoSession(String step, String action, String studentIdentifier, String referenceMonth, String paymentMethod, LocalDateTime expiresAt) {
        static DemoSession waitingStudent(String action) {
            return new DemoSession("WAITING_STUDENT", action, "", "", "", LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingMonth(String action, String studentIdentifier) {
            return new DemoSession("WAITING_MONTH", action, studentIdentifier, "", "", LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingPayment(String action, String studentIdentifier, String referenceMonth) {
            return new DemoSession("WAITING_PAYMENT", action, studentIdentifier, referenceMonth, "", LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingAutoSimulation(String studentIdentifier, String referenceMonth, String paymentMethod) {
            return new DemoSession("WAITING_AUTO_SIMULATION", "AUTO", studentIdentifier, referenceMonth, paymentMethod, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingManualSimulation(String studentIdentifier, String referenceMonth, String paymentMethod) {
            return new DemoSession("WAITING_MANUAL_PAYMENT", "MANUAL", studentIdentifier, referenceMonth, paymentMethod, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingProof(String studentIdentifier, String referenceMonth, String paymentMethod) {
            return new DemoSession("WAITING_PROOF", "PROOF", studentIdentifier, referenceMonth, paymentMethod, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }
    }
}
