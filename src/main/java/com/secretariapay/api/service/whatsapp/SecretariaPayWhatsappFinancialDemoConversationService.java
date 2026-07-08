package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class SecretariaPayWhatsappFinancialDemoConversationService extends SecretariaPayWhatsappFinancialConversationService {

    private static final int SESSION_MINUTES = 60;
    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final String PANEL_BASE_URL = "https://painel-secretariapay.paixaoangola.com";
    private static final BigDecimal PROPINA_AMOUNT = new BigDecimal("45000.00");
    private static final BigDecimal MATRICULA_AMOUNT = new BigDecimal("30000.00");
    private static final BigDecimal RECURSO_AMOUNT = new BigDecimal("15000.00");
    private static final BigDecimal DECLARACAO_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal TOTAL_EM_ABERTO = new BigDecimal("145000.00");
    private static final BigDecimal DEMO_FINE = new BigDecimal("5000.00");

    private final Map<String, DemoSession> sessions = new ConcurrentHashMap<>();
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final String demoEmail;

    public SecretariaPayWhatsappFinancialDemoConversationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptRepository receiptRepository,
            WhatsappSessionRepository sessionRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService,
            FallbackNotificationService fallbackNotificationService,
            @Value("${secretariapay.demo.email:dalawilson1244@gmail.com}") String demoEmail
    ) {
        super(studentRepository, chargeRepository, receiptRepository, sessionRepository, whatsAppCloudApiClient, mockAutomaticPaymentService);
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.demoEmail = demoEmail == null || demoEmail.isBlank() ? "dalawilson1244@gmail.com" : demoEmail.trim();
    }

    @Override
    public Optional<String> handle(String fromPhone, String messageType, String rawMessage) {
        String phone = sanitizePhone(fromPhone);
        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        clearExpired(phone);

        if (isClose(normalized)) {
            DemoSession session = sessions.remove(phone);
            return Optional.of(buildClosing(session));
        }

        if (isMedia(type) || normalized.contains("imagem recebida") || normalized.contains("documento recebido")) {
            DemoSession current = sessions.get(phone);
            if (current != null && "WAITING_PROOF".equals(current.step())) {
                sessions.remove(phone);
                return Optional.of(buildProofReceivedAndReceipt(phone, current));
            }
            return Optional.of(buildProofReceivedWithoutContext());
        }

        if (isMenu(normalized) || isGreeting(normalized)) {
            sessions.remove(phone);
            return Optional.of(buildMainMenu());
        }

        DemoSession session = sessions.get(phone);
        if (session != null) {
            return Optional.of(handleSession(phone, session, message, normalized));
        }

        if ("1".equals(normalized) || isPropinaIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("PROPINA"));
            return Optional.of(askStudent("📌 Atendimento de Propinas"));
        }

        if ("2".equals(normalized) || isFinancialSummaryIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("SUMMARY"));
            return Optional.of(askStudent("📊 Vou consultar a sua situação financeira académica.\n\nOpção - 2"));
        }

        if ("3".equals(normalized) || isBordereauIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("BORDEREAU"));
            return Optional.of(askStudent("📄 Para localizar o bordereau/comprovativo já pago, preciso identificar o estudante.\n\nOpção - 3"));
        }

        if ("4".equals(normalized) || isMatriculaIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("MATRICULA"));
            return Optional.of(askStudent("🧾 Vou preparar o pagamento de matrícula.\n\nOpção - 4"));
        }

        if ("5".equals(normalized) || isRecursoIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("RECURSO"));
            return Optional.of(askStudent("📝 Vou preparar o pagamento de recurso.\n\nOpção - 5"));
        }

        if ("6".equals(normalized) || isDeclaracaoIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("DECLARACAO"));
            return Optional.of(askStudent("📄 Vou preparar o pagamento de declaração.\n\nOpção - 6"));
        }

        if ("7".equals(normalized) || isHumanIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingHumanReason());
            return Optional.of(buildHumanSupportMenu());
        }

        if (isOverdueIntent(normalized)) {
            sessions.put(phone, DemoSession.waitingStudent("OVERDUE"));
            return Optional.of(askStudent("📌 Vou consultar propinas em atraso, multas e pendências."));
        }

        return Optional.of(buildOutOfScope());
    }

    private String handleSession(String phone, DemoSession session, String message, String normalized) {
        if ("WAITING_STUDENT".equals(session.step())) {
            if (message.isBlank()) return askStudent("⚠️ Não consegui identificar o cadastro.");
            String student = normalizeStudentIdentifier(message);
            DemoSession withStudent = session.withStudent(student);

            if ("SUMMARY".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingSummaryAction(student));
                return buildFinancialSummary(student);
            }

            if ("BORDEREAU".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingReceiptChoice(student));
                return buildBordereauList(student);
            }

            if ("MATRICULA".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingPayment("MATRICULA", student, "Matrícula 2026", "Matrícula", MATRICULA_AMOUNT));
                return buildServiceGuide(student, "Matrícula 2026", "Matrícula", MATRICULA_AMOUNT);
            }

            if ("RECURSO".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingPayment("RECURSO", student, "Recurso", "Exame de recurso", RECURSO_AMOUNT));
                return buildServiceGuide(student, "Recurso", "Exame de recurso", RECURSO_AMOUNT);
            }

            if ("DECLARACAO".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingPayment("DECLARACAO", student, "Declaração", "Declaração", DECLARACAO_AMOUNT));
                return buildServiceGuide(student, "Declaração", "Declaração", DECLARACAO_AMOUNT);
            }

            if ("OVERDUE".equals(session.action())) {
                sessions.put(phone, DemoSession.waitingOverdueChoice(student));
                return buildOverdueList(student);
            }

            sessions.put(phone, DemoSession.waitingMonth(session.action(), student));
            return buildStudentFoundAndAskMonth(withStudent.studentIdentifier());
        }

        if ("WAITING_SUMMARY_ACTION".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "pagar pendencias", "pagar pendências", "pendencias", "pendências")) {
                sessions.put(phone, DemoSession.waitingPayment("SUMMARY_PENDING", session.studentIdentifier(), currentMonthLabel(), "Propinas pendentes", TOTAL_EM_ABERTO));
                return buildGuidePrepared(session.studentIdentifier(), currentMonthLabel(), TOTAL_EM_ABERTO, true);
            }

            if ("2".equals(normalized) || containsAny(normalized, "guia do mes", "guia do mês", "mes atual", "mês atual")) {
                sessions.put(phone, DemoSession.waitingPayment("SUMMARY_CURRENT", session.studentIdentifier(), currentMonthLabel(), "Propina mensal", PROPINA_AMOUNT));
                return buildGuidePrepared(session.studentIdentifier(), currentMonthLabel(), PROPINA_AMOUNT, false);
            }

            if ("3".equals(normalized) || isBordereauIntent(normalized)) {
                sessions.put(phone, DemoSession.waitingReceiptChoice(session.studentIdentifier()));
                return buildBordereauList(session.studentIdentifier());
            }

            if ("4".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }

            return buildFinancialSummary(session.studentIdentifier());
        }

        if ("WAITING_MONTH".equals(session.step())) {
            if ("4".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }

            if ("1".equals(normalized) || containsAny(normalized, "mes atual", "mês atual", "propina atual", "propina do mes")) {
                String reference = currentMonthLabel();
                sessions.put(phone, DemoSession.waitingPayment(session.action(), session.studentIdentifier(), reference, "Propina mensal", PROPINA_AMOUNT));
                return buildGuidePrepared(session.studentIdentifier(), reference, PROPINA_AMOUNT, false);
            }

            if ("2".equals(normalized) || containsAny(normalized, "outro mes", "outro mês", "escolher")) {
                sessions.put(phone, DemoSession.waitingOtherMonth(session.studentIdentifier()));
                return askSpecificMonth();
            }

            if ("3".equals(normalized) || isOverdueIntent(normalized)) {
                sessions.put(phone, DemoSession.waitingOverdueChoice(session.studentIdentifier()));
                return buildOverdueList(session.studentIdentifier());
            }

            String explicitMonth = resolveReferenceMonth(normalized, message);
            if (!explicitMonth.isBlank()) {
                MonthKind kind = classifyMonth(message);
                if (kind == MonthKind.FUTURE) {
                    sessions.put(phone, DemoSession.waitingFutureConfirm(session.studentIdentifier(), explicitMonth));
                    return buildFutureMonthConfirmation(explicitMonth);
                }
                boolean overdue = kind == MonthKind.PAST;
                sessions.put(phone, DemoSession.waitingPayment("PROPINA", session.studentIdentifier(), explicitMonth, "Propina mensal", overdue ? PROPINA_AMOUNT.add(DEMO_FINE) : PROPINA_AMOUNT));
                return buildGuidePrepared(session.studentIdentifier(), explicitMonth, overdue ? PROPINA_AMOUNT.add(DEMO_FINE) : PROPINA_AMOUNT, overdue);
            }

            return buildStudentFoundAndAskMonth(session.studentIdentifier());
        }

        if ("WAITING_OTHER_MONTH".equals(session.step())) {
            String reference = resolveReferenceMonth(normalized, message);
            if (reference.isBlank()) return askSpecificMonth();

            MonthKind kind = classifyMonth(message);
            if (kind == MonthKind.FUTURE) {
                sessions.put(phone, DemoSession.waitingFutureConfirm(session.studentIdentifier(), reference));
                return buildFutureMonthConfirmation(reference);
            }

            boolean overdue = kind == MonthKind.PAST;
            BigDecimal amount = overdue ? PROPINA_AMOUNT.add(DEMO_FINE) : PROPINA_AMOUNT;
            sessions.put(phone, DemoSession.waitingPayment("PROPINA", session.studentIdentifier(), reference, "Propina mensal", amount));
            return buildGuidePrepared(session.studentIdentifier(), reference, amount, overdue);
        }

        if ("WAITING_FUTURE_CONFIRM".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "sim", "antecipar", "quero")) {
                sessions.put(phone, DemoSession.waitingPayment("PROPINA", session.studentIdentifier(), session.referenceMonth(), "Propina mensal antecipada", PROPINA_AMOUNT));
                return buildGuidePrepared(session.studentIdentifier(), session.referenceMonth(), PROPINA_AMOUNT, false);
            }
            sessions.remove(phone);
            return buildMainMenu();
        }

        if ("WAITING_OVERDUE_CHOICE".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "todos", "pagar tudo", "pagar todos")) {
                sessions.put(phone, DemoSession.waitingPayment("OVERDUE_ALL", session.studentIdentifier(), "Maio/2026 + Junho/2026", "Propinas em atraso", new BigDecimal("100000.00")));
                return buildGuidePrepared(session.studentIdentifier(), "Maio/2026 + Junho/2026", new BigDecimal("100000.00"), true);
            }
            if ("2".equals(normalized) || containsAny(normalized, "um mes", "um mês", "apenas")) {
                sessions.put(phone, DemoSession.waitingPayment("OVERDUE_ONE", session.studentIdentifier(), "Maio/2026", "Propina Maio/2026", new BigDecimal("50000.00")));
                return buildGuidePrepared(session.studentIdentifier(), "Maio/2026", new BigDecimal("50000.00"), true);
            }
            sessions.remove(phone);
            return buildMainMenu();
        }

        if ("WAITING_RECEIPT_CHOICE".equals(session.step())) {
            if ("4".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }
            sessions.remove(phone);
            return buildBordereauSent(phone, session, normalized);
        }

        if ("WAITING_PAYMENT".equals(session.step())) {
            if ("5".equals(normalized) || "0".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }

            if ("1".equals(normalized) || containsAny(normalized, "multicaixa express", "express")) {
                sessions.put(phone, session.withPaymentMethod("Multicaixa Express").withStep("WAITING_AUTO_SIMULATION"));
                return buildGuideAndAskAutoSimulation(phone, session.withPaymentMethod("Multicaixa Express"));
            }

            if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
                sessions.put(phone, session.withPaymentMethod("Pagamento por Referência").withStep("WAITING_AUTO_SIMULATION"));
                return buildGuideAndAskAutoSimulation(phone, session.withPaymentMethod("Pagamento por Referência"));
            }

            if ("3".equals(normalized) || containsAny(normalized, "transferencia", "transferência", "mesmo banco")) {
                sessions.put(phone, session.withPaymentMethod("Transferência mesmo banco").withStep("WAITING_AUTO_SIMULATION"));
                return buildGuideAndAskAutoSimulation(phone, session.withPaymentMethod("Transferência mesmo banco"));
            }

            if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "outro banco", "bancario", "bancário")) {
                sessions.put(phone, session.withPaymentMethod("Depósito bancário / transferência de outro banco").withStep("WAITING_MANUAL_PAYMENT"));
                return buildBankPaymentInstructions(phone, session.withPaymentMethod("Depósito bancário / transferência de outro banco"));
            }

            return buildPaymentMethods(session.studentIdentifier(), session.referenceMonth(), session.amount(), session.serviceName());
        }

        if ("WAITING_AUTO_SIMULATION".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "simular", "confirmar", "pago", "paguei", "sucesso")) {
                sessions.remove(phone);
                return buildAutomaticReceipt(phone, session);
            }
            return buildAutoSimulationReminder(session);
        }

        if ("WAITING_MANUAL_PAYMENT".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "simular", "paguei", "deposito", "depósito", "feito")) {
                sessions.put(phone, session.withStep("WAITING_PROOF"));
                return askProofAfterManualPayment();
            }
            return buildManualPaymentReminder();
        }

        if ("WAITING_PROOF".equals(session.step())) return askProofAfterManualPayment();

        if ("WAITING_HUMAN_REASON".equals(session.step())) {
            sessions.remove(phone);
            return ("""
                    Obrigado. A sua solicitação foi encaminhada para a DCR.

                    Motivo informado: %s
                    Protocolo: ATD-%s

                    Horário de atendimento humano:
                    Segunda a sexta, das 08h às 17h.
                    """).formatted(message, shortId()).trim();
        }

        sessions.remove(phone);
        return buildMainMenu();
    }

    private String buildMainMenu() {
        return ("""
                Secretaria Pay (IMETRO): %s 👋

                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Como posso ajudar?

                Responda com o número ou escreva o nome da opção:

                [1] Propinas
                [2] Situação Financeira
                [3] Solicitar Bordereau já pago
                [4] Pagar Matrícula
                [5] Pagar Recurso
                [6] Pagar Declaração
                [7] Falar com a DCR
                """).formatted(greeting()).trim();
    }

    private String askStudent(String intro) {
        return ("""
                %s

                Para consultar ou gerar a sua guia de pagamento, informe um dos dados abaixo:

                - Número de matrícula
                - Número do BI
                - Telefone cadastrado

                Exemplo:
                IMETRO-2026-TESTE-002
                """).formatted(intro).trim();
    }

    private String buildStudentFoundAndAskMonth(String student) {
        return ("""
                ✅ Cadastro encontrado.

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula: %s
                Curso: Direito
                Ano académico: 2026
                Estado financeiro: Com propina disponível para pagamento

                Informe o mês de referência da propina:

                [1] Propina do mês atual
                [2] Escolher outro mês
                [3] Propinas em atraso e multas
                [4] Voltar ao menu principal
                """).formatted(student).trim();
    }

    private String askSpecificMonth() {
        return """
                Informe o mês desejado.

                Pode responder, por exemplo:
                Julho/2026
                07/2026
                Agosto/2026

                O sistema irá verificar se o mês é anterior, atual ou futuro.
                """.trim();
    }

    private String buildFutureMonthConfirmation(String reference) {
        return ("""
                📌 O mês escolhido ainda não venceu.

                Mês: %s

                Deseja antecipar o pagamento da propina?

                [1] Sim, quero antecipar
                [2] Não, voltar
                """).formatted(reference).trim();
    }

    private String buildGuidePrepared(String student, String reference, BigDecimal amount, boolean overdue) {
        String warning = overdue ? "\n⚠️ Identificámos pendência, multa ou atraso associado a este mês.\n" : "";
        return ("""
                📌

                📄 Guia preparada.%s

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula: %s
                Mês de referência: %s
                Valor da propina: %s
                Vencimento: 10/07/2026

                Escolha a forma de pagamento:

                [1] Multicaixa Express
                [2] Pagamento por Referência
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [5] Voltar
                """).formatted(warning, student, reference, money(amount)).trim();
    }

    private String buildPaymentMethods(String student, String reference, BigDecimal amount, String service) {
        return ("""
                Serviço: %s
                Cadastro: %s
                Referência: %s
                Valor: %s

                Escolha a forma de pagamento:

                [1] Multicaixa Express
                [2] Pagamento por Referência
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [5] Voltar
                """).formatted(service, student, reference, money(amount)).trim();
    }

    private String buildOverdueList(String student) {
        return ("""
                📌 Propinas em atraso encontradas.

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula: %s

                Pendências:

                1. Maio/2026 — 45.000,00 Kz
                   Multa: 5.000,00 Kz
                   Total: 50.000,00 Kz

                2. Junho/2026 — 45.000,00 Kz
                   Multa: 5.000,00 Kz
                   Total: 50.000,00 Kz

                Total em atraso: 100.000,00 Kz

                Escolha uma opção:

                [1] Pagar todos os meses em atraso
                [2] Escolher apenas um mês
                [3] Voltar ao menu anterior
                """).formatted(student).trim();
    }

    private String buildServiceGuide(String student, String reference, String service, BigDecimal amount) {
        return ("""
                📄 Pagamento de %s

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula: %s
                Serviço: %s
                Valor: %s

                Escolha a forma de pagamento:

                [1] Multicaixa Express
                [2] Pagamento por Referência
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [5] Voltar
                """).formatted(service, student, reference, money(amount)).trim();
    }

    private String buildGuideAndAskAutoSimulation(String phone, DemoSession session) {
        sendDemoGuidePdf(phone, session);

        String referenceBlock = "Pagamento por Referência".equals(session.paymentMethod())
                ? "\nEntidade: 00348\nReferência: 205114879\nValor: " + money(session.amount()) + "\n"
                : "";

        return ("""
                ✅ Guia de pagamento criada.

                Forma de pagamento: %s
                Valor: %s
                Mês/Serviço de referência: %s
                Vencimento: 10/07/2026
                %s
                O PDF da guia foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Para demonstrar o fluxo automático, responda:

                [1] Simular pagamento confirmado
                [2] Voltar
                """).formatted(session.paymentMethod(), money(session.amount()), session.referenceMonth(), referenceBlock).trim();
    }

    private String buildBankPaymentInstructions(String phone, DemoSession session) {
        sendDemoGuidePdf(phone, session);
        return ("""
                🏦 Dados para pagamento bancário

                Banco: Banco de exemplo
                IBAN: AO06 0000 0000 0000 0000 0000 0
                Titular: Instituto Superior Politécnico Metropolitano de Angola
                Valor: %s

                Após o pagamento, envie o comprovativo neste canal para validação pela DCR.

                Para demonstrar o fluxo com comprovativo, responda:
                [1] Simular pagamento realizado
                """).formatted(money(session.amount())).trim();
    }

    private String buildAutomaticReceipt(String phone, DemoSession session) {
        String receipt = receiptCode();
        sendDemoReceiptPdf(phone, session, receipt);

        return ("""
                ✅ Pagamento confirmado com sucesso.

                📄 O bordereau foi emitido em PDF.

                Forma de pagamento: %s
                Referência: %s
                Valor pago: %s
                Bordereau: %s

                Enviei o PDF neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                %s
                """).formatted(session.paymentMethod(), session.referenceMonth(), money(session.amount()), receipt, buildNeutralClosing()).trim();
    }

    private String buildProofReceivedAndReceipt(String phone, DemoSession session) {
        String receipt = receiptCode();
        sendDemoReceiptPdf(phone, session, receipt);

        return ("""
                📎 Comprovativo recebido.

                O seu pagamento será analisado pela DCR.

                ✅ Na demonstração, a validação foi simulada como aprovada.

                📄 O bordereau/comprovativo foi emitido em PDF.

                Enviei o PDF neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Também poderá apresentar o comprovativo presencialmente na DCR, caso seja necessário.
                """).trim();
    }

    private String buildProofReceivedWithoutContext() {
        return """
                📎 Comprovativo recebido.

                O seu pagamento será analisado pela DCR.

                Após a validação, o sistema enviará automaticamente o bordereau/comprovativo neste mesmo canal.

                Também poderá apresentar o comprovativo presencialmente na DCR, caso seja necessário.
                """.trim();
    }

    private String askProofAfterManualPayment() {
        return """
                📎 Por favor, envie o comprovativo de pagamento neste canal.

                Formatos aceitos:

                - Imagem
                - PDF
                - Foto do talão
                - Comprovativo bancário

                Após o envio, a DCR fará a validação.
                """.trim();
    }

    private String buildManualPaymentReminder() {
        return """
                Guia emitida para pagamento bancário.

                Para continuar a demonstração, responda:
                [1] Simular pagamento realizado

                Depois o sistema irá solicitar o comprovativo para validação pela DCR.
                """.trim();
    }

    private String buildAutoSimulationReminder(DemoSession session) {
        return ("""
                Guia criada para %s.

                Para concluir a demonstração, responda:
                [1] Simular pagamento confirmado
                """).formatted(session.paymentMethod()).trim();
    }

    private String buildBordereauList(String student) {
        return ("""
                📄 Bordereaux encontrados.

                Cadastro: %s

                Escolha qual comprovativo deseja receber:

                [1] Propina Julho/2026 — 45.000,00 Kz
                [2] Matrícula 2026 — valor cadastrado
                [3] Recurso — valor cadastrado
                [4] Voltar
                """).formatted(student).trim();
    }

    private String buildBordereauSent(String phone, DemoSession session, String choice) {
        String receipt = receiptCode();
        DemoSession receiptSession = session.withPaymentMethod("Reenvio de bordereau").withReferenceMonth(resolveReceiptReference(choice)).withAmount(PROPINA_AMOUNT);
        sendDemoReceiptPdf(phone, receiptSession, receipt);
        return """
                📄 Bordereau enviado com sucesso.

                Enviei o PDF neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.
                """.trim();
    }

    private String buildFinancialSummary(String student) {
        return ("""
                📊 Situação Financeira Académica

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula: %s
                Ano académico: 2026

                Propinas pagas: Janeiro, Fevereiro, Março, Abril
                Propinas em atraso: Maio, Junho
                Propina do mês atual: Julho/2026
                Total em aberto: 145.000,00 Kz
                Estado financeiro: Com pendências

                Escolha uma opção:

                [1] Pagar pendências
                [2] Gerar guia do mês atual
                [3] Solicitar bordereau
                [4] Voltar
                """).formatted(student).trim();
    }

    private String buildHumanSupportMenu() {
        return """
                Certo. Vou encaminhar o seu atendimento para a DCR.

                Antes disso, informe o motivo:

                [1] Pagamento não confirmado
                [2] Guia com erro
                [3] Valor incorreto
                [4] Solicitação urgente
                [5] Problema com comprovativo
                [6] Outro assunto
                """.trim();
    }

    private String buildOutOfScope() {
        return """
                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Posso ajudar com propinas, situação financeira, bordereaux, matrícula, recurso, declaração, comprovativos e atendimento da DCR.

                Para começar, responda menu ou escolha uma opção de 1 a 7.
                """.trim();
    }

    private String buildClosing(DemoSession session) {
        return ("""
                %s

                O atendimento financeiro académico foi encerrado.
                Sempre que precisar, envie nova mensagem por aqui.

                DCR / IMETRO
                """).formatted(buildNeutralClosing()).trim();
    }

    private void sendDemoGuidePdf(String phone, DemoSession session) {
        String code = "GUIA-WPP-" + shortId();
        String pdfUrl = API_BASE_URL + "/api/v1/public/guides/" + encode(code) + "/pdf";
        String guideUrl = PANEL_BASE_URL + "/guias/" + encode(code);
        String caption = ("""
                SecretáriaPay Académico: guia de pagamento em PDF.

                Estudante: WILSON DOS SANTOS KAHANGO DALA
                Matrícula/Cadastro: %s
                Referência: %s
                Serviço: %s
                Valor: %s

                Guia online: %s
                """).formatted(session.studentIdentifier(), session.referenceMonth(), session.serviceName(), money(session.amount()), guideUrl).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, "guia-" + code + ".pdf", caption);
        sendGuideEmail(session, code, guideUrl);
    }

    private void sendDemoReceiptPdf(String phone, DemoSession session, String receipt) {
        String pdfUrl = API_BASE_URL + "/api/v1/public/demo/receipts/" + encode(receipt) + "/pdf"
                + "?student=" + encode(session.studentIdentifier())
                + "&month=" + encode(session.referenceMonth())
                + "&method=" + encode(session.paymentMethod());
        String caption = ("""
                SecretáriaPay Académico: bordereau/comprovativo emitido.

                Bordereau: %s
                Cadastro: %s
                Referência: %s
                Valor: %s
                """).formatted(receipt, session.studentIdentifier(), session.referenceMonth(), money(session.amount())).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, "bordereau-" + receipt + ".pdf", caption);
        sendReceiptEmail(session, receipt, pdfUrl);
    }

    private void sendGuideEmail(DemoSession session, String code, String guideUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName("WILSON DOS SANTOS KAHANGO DALA");
        request.setStudentNumber(session.studentIdentifier());
        request.setEmail(demoEmail);
        request.setGuideCode(code);
        request.setGuideUrl(guideUrl);
        request.setAmount(session.amount());
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 10));
        request.setMessage("Guia emitida automaticamente pelo atendimento financeiro académico do IMETRO via SecretáriaPay.");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private void sendReceiptEmail(DemoSession session, String receipt, String pdfUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName("WILSON DOS SANTOS KAHANGO DALA");
        request.setStudentNumber(session.studentIdentifier());
        request.setEmail(demoEmail);
        request.setGuideCode(receipt);
        request.setGuideUrl(pdfUrl);
        request.setAmount(session.amount());
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 10));
        request.setMessage("Bordereau/comprovativo emitido automaticamente pelo SecretáriaPay após confirmação do pagamento.");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private String resolveReferenceMonth(String normalized, String raw) {
        if ("1".equals(normalized) || containsAny(normalized, "mes atual", "mês atual", "atual")) return currentMonthLabel();
        if ("2".equals(normalized) || containsAny(normalized, "mes passado", "mês passado", "atraso", "atrasado")) return "Junho/2026";
        if (raw != null && raw.matches("(?i).*\\b(0?[1-9]|1[0-2])[/.-]20\\d{2}\\b.*")) return raw.trim();
        if (containsAny(normalized, "janeiro")) return "Janeiro/2026";
        if (containsAny(normalized, "fevereiro")) return "Fevereiro/2026";
        if (containsAny(normalized, "marco", "março")) return "Março/2026";
        if (containsAny(normalized, "abril")) return "Abril/2026";
        if (containsAny(normalized, "maio")) return "Maio/2026";
        if (containsAny(normalized, "junho")) return "Junho/2026";
        if (containsAny(normalized, "julho")) return "Julho/2026";
        if (containsAny(normalized, "agosto")) return "Agosto/2026";
        if (containsAny(normalized, "setembro")) return "Setembro/2026";
        if (containsAny(normalized, "outubro")) return "Outubro/2026";
        if (containsAny(normalized, "novembro")) return "Novembro/2026";
        if (containsAny(normalized, "dezembro")) return "Dezembro/2026";
        return "";
    }

    private MonthKind classifyMonth(String raw) {
        YearMonth selected = tryParseYearMonth(raw).orElse(null);
        if (selected == null) return MonthKind.CURRENT;
        YearMonth current = YearMonth.of(2026, 7);
        if (selected.isBefore(current)) return MonthKind.PAST;
        if (selected.isAfter(current)) return MonthKind.FUTURE;
        return MonthKind.CURRENT;
    }

    private Optional<YearMonth> tryParseYearMonth(String raw) {
        String normalized = normalize(raw);
        int month = 0;
        if (containsAny(normalized, "janeiro")) month = 1;
        else if (containsAny(normalized, "fevereiro")) month = 2;
        else if (containsAny(normalized, "marco", "março")) month = 3;
        else if (containsAny(normalized, "abril")) month = 4;
        else if (containsAny(normalized, "maio")) month = 5;
        else if (containsAny(normalized, "junho")) month = 6;
        else if (containsAny(normalized, "julho")) month = 7;
        else if (containsAny(normalized, "agosto")) month = 8;
        else if (containsAny(normalized, "setembro")) month = 9;
        else if (containsAny(normalized, "outubro")) month = 10;
        else if (containsAny(normalized, "novembro")) month = 11;
        else if (containsAny(normalized, "dezembro")) month = 12;

        if (month > 0) return Optional.of(YearMonth.of(2026, month));

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(0?[1-9]|1[0-2])[/.-](20\\d{2})\\b").matcher(raw == null ? "" : raw);
        if (matcher.find()) return Optional.of(YearMonth.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1))));
        return Optional.empty();
    }

    private String currentMonthLabel() {
        return "Julho/2026";
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Bom dia";
        if (hour >= 12 && hour < 18) return "Boa tarde";
        return "Boa noite";
    }

    private String receiptCode() {
        return "BORD-DEMO-" + shortId();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " Kz";
    }

    private String normalizeStudentIdentifier(String value) {
        String clean = safe(value).trim();
        return clean.isBlank() ? "IMETRO-2026-TESTE-002" : clean.toUpperCase(Locale.ROOT);
    }

    private String resolveReceiptReference(String choice) {
        if ("2".equals(choice)) return "Matrícula 2026";
        if ("3".equals(choice)) return "Recurso";
        return "Propina Julho/2026";
    }

    private String buildNeutralClosing() {
        return "Obrigado. Foi um prazer atender.";
    }

    private boolean isMedia(String type) {
        return "image".equalsIgnoreCase(type) || "document".equalsIgnoreCase(type);
    }

    private boolean isPropinaIntent(String value) {
        return containsAny(value, "propina", "propinas", "mensalidade", "pagar propina", "pagar mensalidade", "mes atual", "mês atual", "propina atual", "propina do mes", "propina do mês");
    }

    private boolean isBordereauIntent(String value) {
        return containsAny(value, "bordereau", "bordero", "borderô", "comprovativo", "recibo", "comprovante", "guia paga", "comprovativo de pagamento");
    }

    private boolean isFinancialSummaryIntent(String value) {
        return containsAny(value, "situacao financeira", "situação financeira", "estado financeiro", "minhas dividas", "minhas dívidas", "pendencias", "pendências", "divida", "dívida", "saldo", "pagamentos em atraso");
    }

    private boolean isMatriculaIntent(String value) {
        return containsAny(value, "matricula", "matrícula", "pagar matricula", "pagar matrícula", "inscricao", "inscrição", "confirmacao de matricula", "confirmação de matrícula");
    }

    private boolean isRecursoIntent(String value) {
        return containsAny(value, "recurso", "exame de recurso", "pagar recurso");
    }

    private boolean isDeclaracaoIntent(String value) {
        return containsAny(value, "declaracao", "declaração", "declaracao escolar", "declaração escolar", "documento", "solicitar declaracao", "solicitar declaração", "pagar declaracao", "pagar declaração");
    }

    private boolean isOverdueIntent(String value) {
        return containsAny(value, "atraso", "atrasadas", "multa", "multas", "vencido", "pendencia", "pendência", "divida", "dívida");
    }

    private boolean isHumanIntent(String value) {
        return containsAny(value, "dcr", "falar com a dcr", "atendente", "humano", "secretaria", "secretária");
    }

    private boolean isMenu(String value) {
        return containsAny(value, "menu", "inicio", "início", "opcoes", "opções", "voltar");
    }

    private boolean isGreeting(String value) {
        return containsAny(value, "ola", "olá", "oi", "bom dia", "boa tarde", "boa noite");
    }

    private boolean isClose(String value) {
        return "0".equals(value) || containsAny(value, "encerrar", "finalizar", "terminar", "sair", "fim", "obrigado", "obrigada", "valeu");
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank()) return false;
        for (String term : terms) if (value.contains(normalize(term))) return true;
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
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

    private enum MonthKind {
        PAST,
        CURRENT,
        FUTURE
    }

    private record DemoSession(String step, String action, String studentIdentifier, String referenceMonth, String paymentMethod, String serviceName, BigDecimal amount, LocalDateTime expiresAt) {
        static DemoSession waitingStudent(String action) {
            return new DemoSession("WAITING_STUDENT", action, "", "", "", "", BigDecimal.ZERO, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingSummaryAction(String studentIdentifier) {
            return new DemoSession("WAITING_SUMMARY_ACTION", "SUMMARY", studentIdentifier, "", "", "Situação Financeira", TOTAL_EM_ABERTO, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingMonth(String action, String studentIdentifier) {
            return new DemoSession("WAITING_MONTH", action, studentIdentifier, "", "", "Propina mensal", PROPINA_AMOUNT, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingOtherMonth(String studentIdentifier) {
            return new DemoSession("WAITING_OTHER_MONTH", "PROPINA", studentIdentifier, "", "", "Propina mensal", PROPINA_AMOUNT, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingFutureConfirm(String studentIdentifier, String referenceMonth) {
            return new DemoSession("WAITING_FUTURE_CONFIRM", "PROPINA", studentIdentifier, referenceMonth, "", "Propina mensal antecipada", PROPINA_AMOUNT, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingOverdueChoice(String studentIdentifier) {
            return new DemoSession("WAITING_OVERDUE_CHOICE", "OVERDUE", studentIdentifier, "", "", "Propinas em atraso", new BigDecimal("100000.00"), LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingReceiptChoice(String studentIdentifier) {
            return new DemoSession("WAITING_RECEIPT_CHOICE", "BORDEREAU", studentIdentifier, "", "", "Bordereau", BigDecimal.ZERO, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingPayment(String action, String studentIdentifier, String referenceMonth, String serviceName, BigDecimal amount) {
            return new DemoSession("WAITING_PAYMENT", action, studentIdentifier, referenceMonth, "", serviceName, amount, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static DemoSession waitingHumanReason() {
            return new DemoSession("WAITING_HUMAN_REASON", "HUMAN", "", "", "", "Atendimento DCR", BigDecimal.ZERO, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        DemoSession withStudent(String studentIdentifier) {
            return new DemoSession(step, action, studentIdentifier, referenceMonth, paymentMethod, serviceName, amount, expiresAt);
        }

        DemoSession withStep(String step) {
            return new DemoSession(step, action, studentIdentifier, referenceMonth, paymentMethod, serviceName, amount, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        DemoSession withPaymentMethod(String paymentMethod) {
            return new DemoSession(step, action, studentIdentifier, referenceMonth, paymentMethod, serviceName, amount, expiresAt);
        }

        DemoSession withReferenceMonth(String referenceMonth) {
            return new DemoSession(step, action, studentIdentifier, referenceMonth, paymentMethod, serviceName, amount, expiresAt);
        }

        DemoSession withAmount(BigDecimal amount) {
            return new DemoSession(step, action, studentIdentifier, referenceMonth, paymentMethod, serviceName, amount, expiresAt);
        }
    }
}
