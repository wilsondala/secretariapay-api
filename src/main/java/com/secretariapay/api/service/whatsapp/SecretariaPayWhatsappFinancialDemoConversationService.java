package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import com.secretariapay.api.service.payment.AppyPayChargeResponse;
import com.secretariapay.api.service.payment.AppyPayPaymentGatewayService;
import com.secretariapay.api.service.payment.InfinitePayLinkPaymentResponse;
import com.secretariapay.api.service.payment.InfinitePayTestPaymentService;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private static final BigDecimal MATRICULA_AMOUNT = new BigDecimal("30000.00");
    private static final BigDecimal RECURSO_AMOUNT = new BigDecimal("15000.00");
    private static final BigDecimal DECLARACAO_AMOUNT = new BigDecimal("5000.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CHARGE_CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final Map<String, AppyPaySession> sessions = new ConcurrentHashMap<>();
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final ReceiptRepository receiptRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final AppyPayPaymentGatewayService appyPayPaymentGatewayService;
    private final InfinitePayTestPaymentService infinitePayTestPaymentService;
    private final String demoEmail;

    public SecretariaPayWhatsappFinancialDemoConversationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptRepository receiptRepository,
            WhatsappSessionRepository sessionRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService,
            FallbackNotificationService fallbackNotificationService,
            AppyPayPaymentGatewayService appyPayPaymentGatewayService,
            InfinitePayTestPaymentService infinitePayTestPaymentService,
            @Value("${secretariapay.demo.email:dalawilson1244@gmail.com}") String demoEmail
    ) {
        super(studentRepository, chargeRepository, receiptRepository, sessionRepository, whatsAppCloudApiClient, mockAutomaticPaymentService);
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.receiptRepository = receiptRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.appyPayPaymentGatewayService = appyPayPaymentGatewayService;
        this.infinitePayTestPaymentService = infinitePayTestPaymentService;
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
            sessions.remove(phone);
            return Optional.of("Obrigado. Foi um prazer atender.\n\nDCR / IMETRO");
        }

        if (isMedia(type) || normalized.contains("imagem recebida") || normalized.contains("documento recebido")) {
            AppyPaySession current = sessions.get(phone);
            if (current != null && "WAITING_PROOF".equals(current.step())) {
                sessions.remove(phone);
                return Optional.of(sendReceiptAndBuildReply(phone, current, "Comprovativo recebido e enviado para validação DCR."));
            }
            return Optional.of("📎 Comprovativo recebido.\n\nO seu pagamento será analisado pela DCR. Após validação, o comprovativo oficial será enviado neste WhatsApp e por e-mail.");
        }

        if (isMenu(normalized) || isGreeting(normalized)) {
            sessions.remove(phone);
            return Optional.of(buildMainMenu());
        }

        AppyPaySession session = sessions.get(phone);
        if (session != null) {
            return Optional.of(handleSession(phone, session, message, normalized));
        }

        if ("1".equals(normalized) || isPropinaIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("PROPINA"));
            return Optional.of(askStudent("📌 Atendimento de Propinas"));
        }
        if ("2".equals(normalized) || isFinancialSummaryIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("SUMMARY"));
            return Optional.of(askStudent("📊 Vou consultar a sua situação financeira académica.\n\nOpção - 2"));
        }
        if ("3".equals(normalized) || isBordereauIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("BORDEREAU"));
            return Optional.of(askStudent("📄 Para localizar o comprovativo oficial já pago, preciso identificar o estudante."));
        }
        if ("4".equals(normalized) || isMatriculaIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("MATRICULA"));
            return Optional.of(askStudent("🧾 Vou preparar o pagamento de matrícula."));
        }
        if ("5".equals(normalized) || isRecursoIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("RECURSO"));
            return Optional.of(askStudent("📝 Vou preparar o pagamento de recurso."));
        }
        if ("6".equals(normalized) || isDeclaracaoIntent(normalized)) {
            sessions.put(phone, AppyPaySession.waitingStudent("DECLARACAO"));
            return Optional.of(askStudent("📄 Vou preparar o pagamento de declaração."));
        }
        if ("7".equals(normalized) || isHumanIntent(normalized)) {
            return Optional.of("Certo. Vou encaminhar o seu atendimento para a DCR.\n\nInforme o motivo da solicitação.");
        }

        return Optional.of(buildOutOfScope());
    }

    private String handleSession(String phone, AppyPaySession session, String message, String normalized) {
        if ("WAITING_STUDENT".equals(session.step())) {
            Optional<Student> studentOptional = findRegisteredStudent(message, phone);
            if (studentOptional.isEmpty()) {
                return buildStudentNotFound();
            }

            AppyPaySession withStudent = session.withStudent(studentOptional.get());
            if ("SUMMARY".equals(session.action())) {
                sessions.put(phone, withStudent.withStep("WAITING_SUMMARY_ACTION"));
                return buildFinancialSummary(withStudent);
            }
            if ("BORDEREAU".equals(session.action())) {
                sessions.put(phone, withStudent.withStep("WAITING_RECEIPT_CHOICE"));
                return buildBordereauList(withStudent);
            }
            if ("MATRICULA".equals(session.action())) {
                AppyPaySession payment = syncWithOfficialCharge(withStudent.withSimplePayment("Matrícula 2026", "Matrícula", MATRICULA_AMOUNT).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildServiceGuide(payment);
            }
            if ("RECURSO".equals(session.action())) {
                AppyPaySession payment = syncWithOfficialCharge(withStudent.withSimplePayment("Recurso", "Exame de recurso", RECURSO_AMOUNT).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildServiceGuide(payment);
            }
            if ("DECLARACAO".equals(session.action())) {
                AppyPaySession payment = syncWithOfficialCharge(withStudent.withSimplePayment("Declaração", "Declaração", DECLARACAO_AMOUNT).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildServiceGuide(payment);
            }
            sessions.put(phone, withStudent.withStep("WAITING_MONTH"));
            return buildStudentFoundAndAskMonth(withStudent);
        }

        if ("WAITING_SUMMARY_ACTION".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "pagar pendencias", "pagar pendências", "pendencias", "pendências")) {
                sessions.put(phone, session.withStep("WAITING_MONTH"));
                return buildStudentFoundAndAskMonth(session);
            }
            if ("2".equals(normalized) || containsAny(normalized, "guia do mes", "guia do mês", "mes atual", "mês atual", "primeiro mes", "primeiro mês")) {
                Optional<Charge> selected = findPreferredOpenPropinaCharge(session);
                if (selected.isEmpty()) {
                    sessions.put(phone, session.withStep("WAITING_MONTH"));
                    return buildStudentFoundAndAskMonth(session);
                }
                AppyPaySession payment = syncWithOfficialCharge(session.withCharge(selected.get()).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildGuidePrepared(payment, chargeIsOverdue(payment));
            }
            if ("3".equals(normalized) || isBordereauIntent(normalized)) {
                sessions.put(phone, session.withStep("WAITING_RECEIPT_CHOICE"));
                return buildBordereauList(session);
            }
            sessions.remove(phone);
            return buildMainMenu();
        }

        if ("WAITING_MONTH".equals(session.step())) {
            List<Charge> openCharges = findPayablePropinaCharges(session);
            if (openCharges.isEmpty()) {
                sessions.remove(phone);
                return buildNoOpenPropinaReply(session);
            }

            int overdueOption = openCharges.size() + 1;
            int backOption = openCharges.size() + 2;
            Integer option = parseOption(normalized);

            if (containsAny(normalized, "voltar", "menu") || (option != null && option == backOption)) {
                sessions.remove(phone);
                return buildMainMenu();
            }
            if (containsAny(normalized, "atraso", "multa") || (option != null && option == overdueOption)) {
                sessions.put(phone, session.withStep("WAITING_OVERDUE_CHOICE"));
                return buildOverdueList(session);
            }

            Optional<Charge> selected = resolveChargeSelection(normalized, openCharges);
            if (selected.isPresent()) {
                AppyPaySession payment = syncWithOfficialCharge(session.withCharge(selected.get()).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildGuidePrepared(payment, chargeIsOverdue(payment));
            }

            Optional<Charge> alreadyPaid = resolveChargeSelection(normalized, findPaidPropinaCharges(session));
            if (alreadyPaid.isPresent()) {
                return buildAlreadyPaidReply(session, alreadyPaid.get());
            }

            return buildMonthSelectionRetry(session);
        }

        if ("WAITING_OVERDUE_CHOICE".equals(session.step())) {
            List<Charge> overdueCharges = findOverduePropinaCharges(session);
            if (overdueCharges.isEmpty()) {
                sessions.put(phone, session.withStep("WAITING_MONTH"));
                return buildNoOverdueReply(session);
            }

            int payAllOption = overdueCharges.size() + 1;
            int backOption = overdueCharges.size() + 2;
            Integer option = parseOption(normalized);

            if (containsAny(normalized, "voltar", "menu") || (option != null && option == backOption)) {
                sessions.put(phone, session.withStep("WAITING_MONTH"));
                return buildStudentFoundAndAskMonth(session);
            }
            if (containsAny(normalized, "todos", "regularizar tudo") || (option != null && option == payAllOption)) {
                AppyPaySession payment = combineCharges(session, overdueCharges, "Propinas em atraso").withStep("WAITING_PAYMENT");
                sessions.put(phone, payment);
                return buildGuidePrepared(payment, true);
            }

            Optional<Charge> selected = resolveChargeSelection(normalized, overdueCharges);
            if (selected.isPresent()) {
                AppyPaySession payment = syncWithOfficialCharge(session.withCharge(selected.get()).withStep("WAITING_PAYMENT"));
                sessions.put(phone, payment);
                return buildGuidePrepared(payment, true);
            }

            return buildOverdueList(session);
        }

        if ("WAITING_RECEIPT_CHOICE".equals(session.step())) {
            List<Receipt> receipts = findAvailableReceipts(session);
            if (receipts.isEmpty()) {
                sessions.remove(phone);
                return "⚠️ Não encontrei comprovativos oficiais disponíveis para este estudante.";
            }

            Integer option = parseOption(normalized);
            if (containsAny(normalized, "voltar", "menu") || (option != null && option == receipts.size() + 1)) {
                sessions.remove(phone);
                return buildMainMenu();
            }

            Optional<Receipt> selected = resolveReceiptSelection(normalized, receipts);
            if (selected.isEmpty()) {
                return buildBordereauList(session);
            }

            sessions.remove(phone);
            return sendOfficialReceiptAndBuildReply(phone, session, selected.get(), "Comprovativo reenviado com sucesso.");
        }

        if ("WAITING_PAYMENT".equals(session.step())) {
            if ("5".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }
            if ("1".equals(normalized) || containsAny(normalized, "multicaixa", "express")) {
                return createAppyPayChargeAndSendGuide(phone, session.withPaymentMethod("Multicaixa Express"), true);
            }
            if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
                return createAppyPayChargeAndSendGuide(phone, session.withPaymentMethod("Pagamento por Referência"), false);
            }
            if ("8".equals(normalized) || containsAny(normalized, "infinitepay", "infinite pay", "infinitypay", "infinity pay", "teste real brasil", "brasil", "pix brasil")) {
                return createInfinitePayRealTestAndSendGuide(phone, session.withPaymentMethod("InfinitePay Brasil - teste real"));
            }
            if ("3".equals(normalized) || containsAny(normalized, "transferencia", "transferência", "mesmo banco")) {
                AppyPaySession next = syncWithOfficialCharge(session.withPaymentMethod("Transferência mesmo banco")).withStep("WAITING_BANK_CONFIRMATION");
                sessions.put(phone, next);
                sendOfficialGuidePdf(phone, next);
                return buildTransferSameBankReal(next);
            }
            if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "outro banco")) {
                AppyPaySession next = syncWithOfficialCharge(session.withPaymentMethod("Depósito bancário / transferência de outro banco")).withStep("WAITING_PROOF");
                sessions.put(phone, next);
                sendOfficialGuidePdf(phone, next);
                return buildBankPaymentInstructions(next);
            }
            return buildGuidePrepared(session, chargeIsOverdue(session));
        }

        if ("WAITING_APPYPAY_PAYMENT".equals(session.step())) {
            return "✅ A cobrança já foi enviada para a AppyPay.\n\nEstado: aguardando confirmação real do pagamento.\n\nO comprovativo oficial será emitido automaticamente somente quando a AppyPay confirmar o pagamento como Sucesso.\n\nPara iniciar outro atendimento, responda menu.";
        }
        if ("WAITING_INFINITEPAY_PAYMENT".equals(session.step())) {
            return "✅ O link InfinitePay já foi gerado.\n\nEstado: aguardando pagamento real no Brasil.\n\nApós o retorno de sucesso da InfinitePay, o SecretáriaPay emitirá o comprovativo oficial automaticamente e enviará no WhatsApp/e-mail.\n\nPara iniciar outro atendimento, responda menu.";
        }
        if ("WAITING_BANK_CONFIRMATION".equals(session.step())) {
            return "✅ A guia foi emitida para transferência no mesmo banco.\n\nO comprovativo oficial será emitido automaticamente somente após confirmação bancária real.\n\nPara iniciar outro atendimento, responda menu.";
        }
        if ("WAITING_PROOF".equals(session.step())) {
            return "📎 Por favor, envie o comprovativo de pagamento neste canal.\n\nA DCR fará a validação.";
        }

        sessions.remove(phone);
        return buildMainMenu();
    }

    private String createAppyPayChargeAndSendGuide(String phone, AppyPaySession session, boolean gpo) {
        AppyPaySession officialSession = syncWithOfficialCharge(session);
        String merchantTransactionId = "SPAY-" + officialSession.studentNumber() + "-" + shortId();
        AppyPayChargeResponse appyPay = gpo
                ? appyPayPaymentGatewayService.createMulticaixaExpressCharge(officialSession.amount(), officialSession.serviceName(), merchantTransactionId, officialSession.phone())
                : appyPayPaymentGatewayService.createReferenceCharge(officialSession.amount(), officialSession.serviceName(), merchantTransactionId);

        if (!appyPay.isSuccess()) {
            sessions.remove(phone);
            return ("""
                    ⚠️ Não foi possível criar a cobrança na AppyPay.

                    Integração: AppyPay Sandbox
                    Forma de pagamento: %s
                    Valor: %s
                    Referência académica: %s

                    Estado AppyPay: %s
                    MerchantTransactionId: %s
                    Mensagem: %s

                    Nenhum comprovativo foi emitido.
                    Tente novamente ou fale com a DCR.

                    [1] Voltar ao menu principal
                    [2] Falar com a DCR
                    """).formatted(
                    officialSession.paymentMethod(),
                    money(officialSession.amount()),
                    officialSession.referenceMonth(),
                    safe(appyPay.getStatus()),
                    safe(appyPay.getMerchantTransactionId()),
                    safe(appyPay.getMessage())
            ).trim();
        }

        AppyPaySession waitingPayment = officialSession.withStep("WAITING_APPYPAY_PAYMENT");
        sessions.put(phone, waitingPayment);
        sendOfficialGuidePdf(phone, waitingPayment);

        return ("""
                ✅ Guia de pagamento criada.

                Integração: AppyPay Sandbox
                Forma de pagamento: %s
                Valor a pagar: %s
                Referência académica: %s
                Vencimento: %s

                Estado AppyPay: %s
                MerchantTransactionId: %s
                Mensagem: %s

                O PDF da guia oficial foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                ⏳ Aguardando confirmação real do pagamento pela AppyPay.

                O comprovativo oficial será emitido automaticamente somente quando a AppyPay confirmar o pagamento como Sucesso.
                """).formatted(
                officialSession.paymentMethod(),
                money(officialSession.amount()),
                officialSession.referenceMonth(),
                formatDate(officialSession.dueDate()),
                safe(appyPay.getStatus()),
                safe(appyPay.getMerchantTransactionId()),
                safe(appyPay.getMessage())
        ).trim();
    }

    private String createInfinitePayRealTestAndSendGuide(String phone, AppyPaySession session) {
        AppyPaySession officialSession = syncWithOfficialCharge(session);
        InfinitePayLinkPaymentResponse infinitePay = infinitePayTestPaymentService.createLink(
                phone,
                officialSession.studentName(),
                officialSession.studentNumber(),
                firstNonBlank(officialSession.email(), demoEmail),
                officialSession.referenceMonth(),
                officialSession.serviceName(),
                officialSession.amount(),
                officialSession.baseAmount(),
                officialSession.fineAmount(),
                officialSession.interestAmount(),
                officialSession.dueDate()
        );

        if (!infinitePay.isSuccess()) {
            sessions.remove(phone);
            return ("""
                    ⚠️ Não foi possível gerar o link InfinitePay.

                    Provedor: InfinitePay Brasil - teste real
                    Valor académico original: %s
                    Valor teste Brasil: %s
                    Referência académica: %s

                    Estado: %s
                    Código teste: %s
                    Mensagem: %s

                    Nenhum comprovativo foi emitido.
                    A integração AppyPay Angola continua intacta.
                    """).formatted(
                    money(officialSession.amount()),
                    moneyBrl(infinitePay.getAmountBrl()),
                    officialSession.referenceMonth(),
                    safe(infinitePay.getStatus()),
                    safe(infinitePay.getOrderNsu()),
                    safe(infinitePay.getMessage())
            ).trim();
        }

        AppyPaySession waitingPayment = officialSession.withStep("WAITING_INFINITEPAY_PAYMENT");
        sessions.put(phone, waitingPayment);
        sendOfficialGuidePdf(phone, waitingPayment);

        return ("""
                ✅ Link de pagamento gerado.

                Provedor: InfinitePay Brasil - teste real
                Forma de pagamento: Pix/link InfinitePay
                Valor académico original: %s
                Valor teste Brasil: %s
                Referência académica: %s
                Vencimento académico: %s

                Código teste: %s

                Pague pelo link:
                %s

                O PDF da guia oficial foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                ⏳ Aguardando pagamento real pela InfinitePay.

                Após o retorno de sucesso da InfinitePay, o comprovativo oficial será emitido automaticamente e enviado neste WhatsApp/e-mail.
                """).formatted(
                money(officialSession.amount()),
                moneyBrl(infinitePay.getAmountBrl()),
                officialSession.referenceMonth(),
                formatDate(officialSession.dueDate()),
                safe(infinitePay.getOrderNsu()),
                safe(infinitePay.getCheckoutUrl())
        ).trim();
    }

    private String buildTransferSameBankReal(AppyPaySession session) {
        return ("""
                ✅ Guia de pagamento criada.

                Forma de pagamento: Transferência mesmo banco
                Valor a pagar: %s
                Referência académica: %s
                Vencimento: %s

                O PDF da guia oficial foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                ⏳ Aguardando confirmação bancária real.

                O comprovativo oficial será emitido automaticamente somente após a confirmação do pagamento.
                """).formatted(money(session.amount()), session.referenceMonth(), formatDate(session.dueDate())).trim();
    }

    private String buildMainMenu() {
        return ("""
                Secretaria Pay (IMETRO): %s 👋

                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Como posso ajudar?

                Responda com o número ou escreva o nome da opção:

                [1] Propinas
                [2] Situação Financeira
                [3] Solicitar Comprovativo já pago
                [4] Pagar Matrícula
                [5] Pagar Recurso
                [6] Pagar Declaração
                [7] Falar com a DCR
                """).formatted(greeting()).trim();
    }

    private String askStudent(String intro) {
        return ("""
                %s

                Para continuar, informe um dado cadastrado no sistema:

                - Número de matrícula
                - Número do BI
                - Telefone cadastrado

                Exemplo:
                IMETRO-2026-TESTE-002
                """).formatted(intro).trim();
    }

    private String buildOutOfScope() {
        return """
                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Posso ajudar com propinas, situação financeira, comprovativos, matrícula, recurso, declaração e atendimento da DCR.

                Para começar, responda menu ou escolha uma opção de 1 a 7.
                """.trim();
    }

    private String buildStudentNotFound() {
        return """
                ⚠️ Não encontrei nenhum cadastro com os dados informados.

                Verifique se digitou corretamente a matrícula, BI ou telefone cadastrado.

                Exemplo:
                IMETRO-2026-TESTE-002

                Escolha uma opção:

                [1] Tentar novamente
                [2] Falar com a DCR
                [3] Voltar ao menu principal
                """.trim();
    }

    private String buildStudentFoundAndAskMonth(AppyPaySession session) {
        List<Charge> openCharges = findPayablePropinaCharges(session);
        List<Charge> paidCharges = findPaidPropinaCharges(session);

        if (openCharges.isEmpty()) {
            return buildNoOpenPropinaReply(session);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("✅ Cadastro encontrado.\n\n")
                .append("Estudante: ").append(session.studentName()).append("\n")
                .append("Matrícula: ").append(session.studentNumber()).append("\n")
                .append("Ano académico: 2026\n")
                .append("Estado financeiro: Existem propinas em aberto.\n\n");

        if (!paidCharges.isEmpty()) {
            builder.append("Meses já liquidados: ")
                    .append(summarizeChargeReferences(paidCharges, 6))
                    .append("\n\n");
        }

        builder.append("Meses disponíveis para pagamento:\n\n");
        for (int i = 0; i < openCharges.size(); i++) {
            Charge charge = openCharges.get(i);
            builder.append("[").append(i + 1).append("] ")
                    .append(displayReference(charge))
                    .append(" — Total: ").append(money(charge.getTotalAmount()));
            if (isOverdueCharge(charge)) {
                builder.append(" (em atraso)");
            }
            builder.append("\n");
        }

        int overdueOption = openCharges.size() + 1;
        int backOption = openCharges.size() + 2;
        builder.append("\n[").append(overdueOption).append("] Ver apenas meses em atraso\n")
                .append("[").append(backOption).append("] Voltar ao menu principal\n\n")
                .append("Também pode responder escrevendo o mês, por exemplo: Outubro/2026");

        return builder.toString().trim();
    }

    private String buildMonthSelectionRetry(AppyPaySession session) {
        return "⚠️ Não consegui identificar um mês em aberto com a sua resposta.\n\n" + buildStudentFoundAndAskMonth(session);
    }

    private String buildAlreadyPaidReply(AppyPaySession session, Charge paidCharge) {
        return ("""
                ✅ %s já está liquidado para este estudante.

                Não vou gerar uma nova guia para evitar duplicidade de pagamento.

                Se precisar, posso reenviar o comprovativo desse pagamento.

                %s
                """).formatted(displayReference(paidCharge), buildStudentFoundAndAskMonth(session)).trim();
    }

    private String buildNoOpenPropinaReply(AppyPaySession session) {
        List<Charge> paidCharges = findPaidPropinaCharges(session);
        String paidSummary = paidCharges.isEmpty() ? "Nenhuma propina liquidada encontrada." : summarizeChargeReferences(paidCharges, 8);
        return ("""
                ✅ Cadastro encontrado.

                Estudante: %s
                Matrícula: %s
                Estado financeiro: Não existem propinas em aberto neste momento.
                Meses já liquidados: %s

                Se desejar um comprovativo, responda menu e escolha a opção de comprovativo já pago.
                """).formatted(session.studentName(), session.studentNumber(), paidSummary).trim();
    }

    private String buildGuidePrepared(AppyPaySession session, boolean overdue) {
        String warning = overdue ? "\n⚠️ Foram aplicadas regras de atraso, multa e juros conforme política DCR configurada.\n" : "";
        return ("""
                📌

                📄 Guia preparada.%s

                Estudante: %s
                Matrícula: %s
                Mês/Serviço de referência: %s
                Valor base: %s
                Multa: %s
                Juros: %s
                Total a pagar: %s
                Dias em atraso: %d
                Vencimento: %s

                Escolha a forma de pagamento:

                [1] Multicaixa Express - AppyPay GPO
                [2] Pagamento por Referência - AppyPay REF
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [8] Teste real Brasil - InfinitePay
                [5] Voltar
                """).formatted(
                warning,
                session.studentName(),
                session.studentNumber(),
                session.referenceMonth(),
                money(session.baseAmount()),
                money(session.fineAmount()),
                money(session.interestAmount()),
                money(session.amount()),
                session.daysLate(),
                formatDate(session.dueDate())
        ).trim();
    }

    private String buildOverdueList(AppyPaySession session) {
        List<Charge> overdueCharges = findOverduePropinaCharges(session);
        if (overdueCharges.isEmpty()) {
            return buildNoOverdueReply(session);
        }

        BigDecimal totalOverdue = overdueCharges.stream().map(Charge::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder builder = new StringBuilder();
        builder.append("📌 Propinas em atraso encontradas.\n\n")
                .append("Estudante: ").append(session.studentName()).append("\n")
                .append("Matrícula: ").append(session.studentNumber()).append("\n\n")
                .append("Pendências em atraso:\n\n");

        for (int i = 0; i < overdueCharges.size(); i++) {
            Charge charge = overdueCharges.get(i);
            builder.append(i + 1).append(". ").append(displayReference(charge))
                    .append(" — Base: ").append(money(charge.getAmount())).append("\n")
                    .append("   Multa: ").append(money(charge.getFineAmount())).append("\n")
                    .append("   Juros: ").append(money(charge.getInterestAmount())).append("\n")
                    .append("   Dias em atraso: ").append(daysLate(charge)).append("\n")
                    .append("   Total: ").append(money(charge.getTotalAmount())).append("\n\n");
        }

        int payAllOption = overdueCharges.size() + 1;
        int backOption = overdueCharges.size() + 2;
        builder.append("Total em atraso: ").append(money(totalOverdue)).append("\n\n")
                .append("Escolha uma opção:\n\n")
                .append("[1..").append(overdueCharges.size()).append("] Escolher um mês específico em atraso\n")
                .append("[").append(payAllOption).append("] Pagar todos os meses em atraso\n")
                .append("[").append(backOption).append("] Voltar ao menu anterior\n\n")
                .append("Também pode responder escrevendo o mês, por exemplo: Outubro/2026");

        return builder.toString().trim();
    }

    private String buildNoOverdueReply(AppyPaySession session) {
        return ("""
                ✅ Não encontrei propinas vencidas para este estudante.

                %s
                """).formatted(buildStudentFoundAndAskMonth(session)).trim();
    }

    private String buildServiceGuide(AppyPaySession session) {
        return ("""
                📄 Pagamento de %s

                Estudante: %s
                Matrícula: %s
                Serviço: %s
                Valor: %s

                Escolha a forma de pagamento:

                [1] Multicaixa Express - AppyPay GPO
                [2] Pagamento por Referência - AppyPay REF
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [8] Teste real Brasil - InfinitePay
                [5] Voltar
                """).formatted(session.serviceName(), session.studentName(), session.studentNumber(), session.serviceName(), money(session.amount())).trim();
    }

    private String buildFinancialSummary(AppyPaySession session) {
        List<Charge> allCharges = chargeRepository.findByStudentIdOrderByDueDateDesc(loadStudent(session).getId());
        List<Charge> propinaCharges = allCharges.stream().filter(this::isPropinaCharge).sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        List<Charge> paidCharges = propinaCharges.stream().filter(charge -> charge.getStatus() == ChargeStatus.PAID).toList();
        List<Charge> openCharges = propinaCharges.stream().filter(this::isPayableCharge).toList();
        BigDecimal totalOpen = openCharges.stream().map(Charge::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        String paidSummary = paidCharges.isEmpty() ? "Nenhuma propina liquidada." : summarizeChargeReferences(paidCharges, 8);
        String openSummary = openCharges.isEmpty() ? "Nenhuma propina em aberto." : summarizeChargeReferences(openCharges, 8);

        return ("""
                📊 Situação Financeira Académica

                Estudante: %s
                Matrícula: %s
                Ano académico: 2026

                Propinas pagas: %s
                Propinas em aberto: %s

                Total em aberto: %s
                Estado financeiro: %s

                Escolha uma opção:

                [1] Escolher uma propina em aberto para pagar
                [2] Gerar guia do primeiro mês em aberto
                [3] Solicitar comprovativo já pago
                [4] Voltar
                """).formatted(
                session.studentName(),
                session.studentNumber(),
                paidSummary,
                openSummary,
                money(totalOpen),
                openCharges.isEmpty() ? "Regularizado" : "Com pendências"
        ).trim();
    }

    private String buildBordereauList(AppyPaySession session) {
        List<Receipt> receipts = findAvailableReceipts(session);
        if (receipts.isEmpty()) {
            return "⚠️ Não encontrei comprovativos oficiais emitidos para este estudante.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("📄 Comprovativos encontrados.\n\n")
                .append("Estudante: ").append(session.studentName()).append("\n")
                .append("Matrícula: ").append(session.studentNumber()).append("\n\n")
                .append("Escolha qual comprovativo deseja receber:\n\n");

        for (int i = 0; i < receipts.size(); i++) {
            Receipt receipt = receipts.get(i);
            Charge charge = receipt.getCharge();
            builder.append("[").append(i + 1).append("] ")
                    .append(displayReference(charge))
                    .append(" — ").append(money(charge.getTotalAmount()))
                    .append(" — ").append(receipt.getReceiptCode())
                    .append("\n");
        }

        builder.append("[").append(receipts.size() + 1).append("] Voltar");
        return builder.toString().trim();
    }

    private String buildBankPaymentInstructions(AppyPaySession session) {
        return ("""
                🏦 Dados para pagamento bancário

                Banco: Banco Angolano de Investimento
                IBAN: AO06 0040 0000 6014 4677 1017 1
                Titular: OMNEN INTELENGENDA
                Valor: %s

                Após o pagamento, envie o comprovativo neste canal para validação pela DCR.
                """).formatted(money(session.amount())).trim();
    }

    private String sendReceiptAndBuildReply(String phone, AppyPaySession session, String intro) {
        try {
            Charge charge = ensureOfficialCharge(session);
            Optional<Receipt> existingReceipt = receiptRepository.findByChargeId(charge.getId());
            if (existingReceipt.isPresent()) {
                return sendOfficialReceiptAndBuildReply(phone, session.withCharge(charge), existingReceipt.get(), intro);
            }
        } catch (Exception ignored) {
        }

        return ("""
                ✅ %s

                📎 O comprovativo foi recebido e seguirá para validação manual da DCR.

                Assim que a validação for concluída, o comprovativo oficial será enviado neste WhatsApp e por e-mail.
                """).formatted(intro).trim();
    }

    private String sendOfficialReceiptAndBuildReply(String phone, AppyPaySession session, Receipt receipt, String intro) {
        Charge charge = receipt.getCharge();
        String pdfUrl = API_BASE_URL + "/api/v1/public/receipts/" + encode(receipt.getReceiptCode()) + "/pdf?v=" + System.currentTimeMillis();
        String publicPdfUrl = API_BASE_URL + "/api/v1/public/receipts/" + encode(receipt.getReceiptCode()) + "/pdf";
        String fileName = "Comprovativo_Pagamentos_" + sanitizeFilePart(session.studentNumber()) + "_" + sanitizeFilePart(receipt.getReceiptCode()) + ".pdf";
        String caption = ("""
                SecretáriaPay Académico: comprovativo oficial emitido.

                Comprovativo: %s
                Estudante: %s
                Matrícula: %s
                Referência: %s
                Valor: %s
                Link público: %s
                """).formatted(
                receipt.getReceiptCode(),
                session.studentName(),
                session.studentNumber(),
                displayReference(charge),
                money(charge.getTotalAmount()),
                publicPdfUrl
        ).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, caption);
        sendReceiptEmail(session.withCharge(charge), receipt.getReceiptCode(), publicPdfUrl);

        return ("""
                ✅ %s

                📄 O comprovativo oficial foi emitido em PDF.

                Forma de pagamento: %s
                Referência: %s
                Valor pago: %s
                Comprovativo: %s

                Enviei o PDF neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Obrigado. Foi um prazer atender.
                """).formatted(
                intro,
                firstNonBlank(session.paymentMethod(), "Pagamento confirmado"),
                displayReference(charge),
                money(charge.getTotalAmount()),
                receipt.getReceiptCode()
        ).trim();
    }

    private void sendOfficialGuidePdf(String phone, AppyPaySession session) {
        Charge charge = ensureOfficialCharge(session);
        String pdfUrl = API_BASE_URL + "/api/v1/public/payment-guides/" + encode(charge.getChargeCode()) + "/pdf?v=" + System.currentTimeMillis();
        String publicPdfUrl = API_BASE_URL + "/api/v1/public/payment-guides/" + encode(charge.getChargeCode()) + "/pdf";
        String fileName = "Guia_Pagamento_Academico_" + sanitizeFilePart(session.studentNumber()) + "_" + sanitizeFilePart(charge.getChargeCode()) + ".pdf";
        String caption = ("""
                SecretáriaPay Académico: guia de pagamento oficial em PDF.

                Estudante: %s
                Matrícula: %s
                Referência: %s
                Serviço: %s
                Valor base: %s
                Multa: %s
                Juros: %s
                Total a pagar: %s

                Guia oficial: %s
                """).formatted(
                session.studentName(),
                session.studentNumber(),
                safe(charge.getReferenceMonth()),
                safe(charge.getDescription()),
                money(charge.getAmount()),
                money(charge.getFineAmount()),
                money(charge.getInterestAmount()),
                money(charge.getTotalAmount()),
                publicPdfUrl
        ).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, caption);
        sendGuideEmail(session.withCharge(charge), charge, publicPdfUrl);
    }

    private void sendGuideEmail(AppyPaySession session, Charge charge, String guideUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(session.studentName());
        request.setStudentNumber(session.studentNumber());
        request.setEmail(firstNonBlank(session.email(), demoEmail));
        request.setGuideCode(charge.getChargeCode());
        request.setGuideUrl(guideUrl);
        request.setAmount(charge.getTotalAmount());
        request.setCurrency("AOA");
        request.setDueDate(charge.getDueDate());
        request.setMessage("Guia oficial emitida automaticamente pelo atendimento financeiro académico do IMETRO via SecretáriaPay. Valor base: "
                + money(charge.getAmount()) + ". Multa: " + money(charge.getFineAmount()) + ". Juros: " + money(charge.getInterestAmount()) + ". Total: " + money(charge.getTotalAmount()) + ".");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private void sendReceiptEmail(AppyPaySession session, String receiptCode, String pdfUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(session.studentName());
        request.setStudentNumber(session.studentNumber());
        request.setEmail(firstNonBlank(session.email(), demoEmail));
        request.setGuideCode(receiptCode);
        request.setGuideUrl(pdfUrl);
        request.setAmount(session.amount());
        request.setCurrency("AOA");
        request.setDueDate(session.dueDate());
        request.setMessage("Comprovativo oficial emitido automaticamente pelo SecretáriaPay após confirmação do pagamento.");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private AppyPaySession syncWithOfficialCharge(AppyPaySession session) {
        try {
            return session.withCharge(ensureOfficialCharge(session));
        } catch (Exception ignored) {
            return session;
        }
    }

    private Charge ensureOfficialCharge(AppyPaySession session) {
        Student student = loadStudent(session);
        if (session.referenceMonth() != null && !session.referenceMonth().isBlank()) {
            List<Charge> existing = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId()).stream()
                    .filter(this::isPayableCharge)
                    .filter(charge -> normalize(charge.getReferenceMonth()).equals(normalize(session.referenceMonth())))
                    .toList();
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
        }

        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(buildChargeCode(session))
                .setDescription(resolveChargeDescription(session))
                .setReferenceMonth(session.referenceMonth())
                .setDueDate(session.dueDate())
                .setAmount(session.baseAmount())
                .setFineAmount(session.fineAmount())
                .setInterestAmount(session.interestAmount())
                .setDiscountAmount(BigDecimal.ZERO)
                .setCurrency("AOA")
                .setStatus(session.dueDate() != null && session.dueDate().isBefore(LocalDate.now()) ? ChargeStatus.OVERDUE : ChargeStatus.PENDING);
        return chargeRepository.save(charge);
    }

    private AppyPaySession combineCharges(AppyPaySession session, List<Charge> charges, String serviceName) {
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal fine = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        long totalDaysLate = 0;
        LocalDate dueDate = null;
        List<String> references = new ArrayList<>();

        for (Charge charge : charges) {
            base = base.add(safeMoney(charge.getAmount()));
            fine = fine.add(safeMoney(charge.getFineAmount()));
            interest = interest.add(safeMoney(charge.getInterestAmount()));
            total = total.add(safeMoney(charge.getTotalAmount()));
            totalDaysLate += daysLate(charge);
            references.add(displayReference(charge));
            if (dueDate == null || (charge.getDueDate() != null && charge.getDueDate().isBefore(dueDate))) {
                dueDate = charge.getDueDate();
            }
        }

        return new AppyPaySession(
                session.step(),
                session.action(),
                session.studentNumber(),
                session.studentName(),
                session.email(),
                session.phone(),
                String.join(" + ", references),
                session.paymentMethod(),
                serviceName,
                base,
                fine,
                interest,
                total,
                dueDate == null ? LocalDate.now() : dueDate,
                totalDaysLate,
                LocalDateTime.now().plusMinutes(SESSION_MINUTES)
        );
    }

    private List<Charge> findPayablePropinaCharges(AppyPaySession session) {
        return chargeRepository.findByStudentIdOrderByDueDateDesc(loadStudent(session).getId()).stream()
                .filter(this::isPropinaCharge)
                .filter(this::isPayableCharge)
                .sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Charge> findPaidPropinaCharges(AppyPaySession session) {
        return chargeRepository.findByStudentIdOrderByDueDateDesc(loadStudent(session).getId()).stream()
                .filter(this::isPropinaCharge)
                .filter(charge -> charge.getStatus() == ChargeStatus.PAID)
                .sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Charge> findOverduePropinaCharges(AppyPaySession session) {
        return findPayablePropinaCharges(session).stream().filter(this::isOverdueCharge).toList();
    }

    private Optional<Charge> findPreferredOpenPropinaCharge(AppyPaySession session) {
        return findPayablePropinaCharges(session).stream().findFirst();
    }

    private Optional<Charge> resolveChargeSelection(String rawInput, List<Charge> charges) {
        if (charges == null || charges.isEmpty()) {
            return Optional.empty();
        }

        Integer option = parseOption(rawInput);
        if (option != null && option >= 1 && option <= charges.size()) {
            return Optional.of(charges.get(option - 1));
        }

        String normalizedInput = normalize(rawInput);
        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }

        return charges.stream()
                .filter(charge -> normalizedInput.equals(normalize(charge.getReferenceMonth()))
                        || normalizedInput.contains(normalize(charge.getReferenceMonth()))
                        || normalizedInput.equals(normalize(displayReference(charge))))
                .findFirst();
    }

    private List<Receipt> findAvailableReceipts(AppyPaySession session) {
        return receiptRepository.findByChargeStudentIdAndStatusOrderByChargePaidAtAsc(loadStudent(session).getId(), ReceiptStatus.VALID).stream()
                .sorted(Comparator.comparing((Receipt receipt) -> receipt.getCharge().getPaidAt(), Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .toList();
    }

    private Optional<Receipt> resolveReceiptSelection(String rawInput, List<Receipt> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            return Optional.empty();
        }

        Integer option = parseOption(rawInput);
        if (option != null && option >= 1 && option <= receipts.size()) {
            return Optional.of(receipts.get(option - 1));
        }

        String normalizedInput = normalize(rawInput);
        return receipts.stream()
                .filter((Receipt receipt) -> normalizedInput.equals(normalize(receipt.getReceiptCode()))
                        || normalizedInput.contains(normalize(receipt.getReceiptCode()))
                        || normalizedInput.equals(normalize(displayReference(receipt.getCharge()))))
                .findFirst();
    }

    private Student loadStudent(AppyPaySession session) {
        return studentRepository.findByStudentNumber(session.studentNumber())
                .orElseThrow(() -> new IllegalStateException("Estudante não encontrado para este atendimento."));
    }

    private boolean isPropinaCharge(Charge charge) {
        return charge != null && (normalize(charge.getDescription()).contains("propina") || normalize(charge.getChargeCode()).contains("imt-propina"));
    }

    private boolean isPayableCharge(Charge charge) {
        return charge != null
                && charge.getStatus() != ChargeStatus.PAID
                && charge.getStatus() != ChargeStatus.CANCELLED
                && charge.getStatus() != ChargeStatus.RENEGOTIATED;
    }

    private boolean isOverdueCharge(Charge charge) {
        return charge != null && (charge.getStatus() == ChargeStatus.OVERDUE || (charge.getDueDate() != null && charge.getDueDate().isBefore(LocalDate.now()) && isPayableCharge(charge)));
    }

    private long daysLate(Charge charge) {
        if (charge == null || charge.getDueDate() == null || !charge.getDueDate().isBefore(LocalDate.now())) {
            return 0;
        }
        return ChronoUnit.DAYS.between(charge.getDueDate(), LocalDate.now());
    }

    private String displayReference(Charge charge) {
        return firstNonBlank(charge.getReferenceMonth(), charge.getDescription(), charge.getChargeCode());
    }

    private String summarizeChargeReferences(List<Charge> charges, int maxItems) {
        if (charges == null || charges.isEmpty()) {
            return "Nenhum";
        }
        return charges.stream().limit(Math.max(1, maxItems)).map(this::displayReference).reduce((a, b) -> a + ", " + b).orElse("Nenhum");
    }

    private Integer parseOption(String rawInput) {
        try {
            return Integer.parseInt(safe(rawInput).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildChargeCode(AppyPaySession session) {
        String prefix = resolveChargeCodePrefix(session.serviceName());
        String studentCode = sanitizeFilePart(session.studentNumber());
        String datePart = (session.dueDate() == null ? LocalDate.now() : session.dueDate()).format(CHARGE_CODE_DATE_FORMAT);
        String candidate = prefix + "-" + datePart + "-" + studentCode;
        if (!chargeRepository.existsByChargeCode(candidate)) {
            return candidate;
        }
        return candidate + "-" + shortId().substring(0, 4);
    }

    private String resolveChargeCodePrefix(String serviceName) {
        String normalized = normalize(serviceName);
        if (normalized.contains("propina")) return "IMT-PROPINA";
        if (normalized.contains("matricula") || normalized.contains("matrícula")) return "IMT-MATRICULA";
        if (normalized.contains("recurso")) return "IMT-RECURSO";
        if (normalized.contains("declaracao") || normalized.contains("declaração")) return "IMT-DECLARACAO";
        return "IMT-SERVICO";
    }

    private String resolveChargeDescription(AppyPaySession session) {
        String description = firstNonBlank(session.serviceName(), "Cobrança académica");
        if (session.referenceMonth() != null && !session.referenceMonth().isBlank()) {
            return description + " referente a " + session.referenceMonth();
        }
        return description;
    }

    private boolean chargeIsOverdue(AppyPaySession session) {
        return session.dueDate() != null && session.dueDate().isBefore(LocalDate.now()) && session.amount().compareTo(BigDecimal.ZERO) > 0;
    }

    private String sanitizeFilePart(String value) {
        String sanitized = safe(value).trim().replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " Kz";
    }

    private String moneyBrl(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return "R$ " + String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", safeValue);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMAT);
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private boolean isMedia(String type) {
        return "image".equalsIgnoreCase(type) || "document".equalsIgnoreCase(type);
    }

    private boolean isPropinaIntent(String value) {
        return containsAny(value, "propina", "propinas", "mensalidade", "pagar propina", "pagar mensalidade", "mes atual", "mês atual", "propina atual", "outro mes", "outro mês");
    }

    private boolean isBordereauIntent(String value) {
        return containsAny(value, "bordereau", "bordero", "borderô", "comprovativo", "recibo", "comprovante", "guia paga");
    }

    private boolean isFinancialSummaryIntent(String value) {
        return containsAny(value, "situacao financeira", "situação financeira", "estado financeiro", "minhas dividas", "minhas dívidas", "pendencias", "pendências", "divida", "dívida", "saldo");
    }

    private boolean isMatriculaIntent(String value) {
        return containsAny(value, "matricula", "matrícula", "inscricao", "inscrição", "confirmacao de matricula", "confirmação de matrícula");
    }

    private boolean isRecursoIntent(String value) {
        return containsAny(value, "recurso", "exame de recurso", "pagar recurso");
    }

    private boolean isDeclaracaoIntent(String value) {
        return containsAny(value, "declaracao", "declaração", "declaracao escolar", "declaração escolar", "documento", "solicitar declaracao", "solicitar declaração");
    }

    private boolean isHumanIntent(String value) {
        return containsAny(value, "dcr", "atendente", "humano", "secretaria", "secretária");
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
        for (String term : terms) {
            if (value.contains(normalize(term))) return true;
        }
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

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Optional<Student> findRegisteredStudent(String input, String fromPhone) {
        String clean = safe(input).trim();
        if (!clean.isBlank()) {
            Optional<Student> byNumber = studentRepository.findByStudentNumber(clean);
            if (byNumber.isPresent()) return byNumber;
            Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(clean);
            if (byDocument.isPresent()) return byDocument;
            Optional<Student> byEmail = studentRepository.findByEmailIgnoreCase(clean);
            if (byEmail.isPresent()) return byEmail;
            Optional<Student> byPhone = findByPhoneVariants(clean);
            if (byPhone.isPresent()) return byPhone;
        }
        return findByPhoneVariants(fromPhone);
    }

    private Optional<Student> findByPhoneVariants(String rawPhone) {
        String digits = sanitizePhone(rawPhone);
        if (digits.isBlank()) return Optional.empty();
        List<String> variants = List.of(digits, "+" + digits, digits.replaceFirst("^244", "0"), digits.replaceFirst("^55", "0"));
        for (String variant : variants) {
            Optional<Student> byWhatsapp = studentRepository.findByWhatsapp(variant);
            if (byWhatsapp.isPresent()) return byWhatsapp;
            Optional<Student> byPhone = studentRepository.findByPhone(variant);
            if (byPhone.isPresent()) return byPhone;
            Optional<Student> byGuardian = studentRepository.findByGuardianPhone(variant);
            if (byGuardian.isPresent()) return byGuardian;
        }
        return Optional.empty();
    }

    private void clearExpired(String phone) {
        AppyPaySession session = sessions.get(phone);
        if (session != null && session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(phone);
        }
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Bom dia";
        if (hour >= 12 && hour < 18) return "Boa tarde";
        return "Boa noite";
    }

    private record AppyPaySession(
            String step,
            String action,
            String studentNumber,
            String studentName,
            String email,
            String phone,
            String referenceMonth,
            String paymentMethod,
            String serviceName,
            BigDecimal baseAmount,
            BigDecimal fineAmount,
            BigDecimal interestAmount,
            BigDecimal amount,
            LocalDate dueDate,
            long daysLate,
            LocalDateTime expiresAt
    ) {
        static AppyPaySession waitingStudent(String action) {
            return new AppyPaySession("WAITING_STUDENT", action, "", "", "", "", "", "", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), 0, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withStudent(Student student) {
            return new AppyPaySession(step, action, student.getStudentNumber(), student.getFullName(), student.getEmail(), firstNonBlankStatic(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone()), referenceMonth, paymentMethod, serviceName, baseAmount, fineAmount, interestAmount, amount, dueDate, daysLate, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withStep(String newStep) {
            return new AppyPaySession(newStep, action, studentNumber, studentName, email, phone, referenceMonth, paymentMethod, serviceName, baseAmount, fineAmount, interestAmount, amount, dueDate, daysLate, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withSimplePayment(String newReferenceMonth, String newServiceName, BigDecimal newAmount) {
            BigDecimal safeAmount = newAmount == null ? BigDecimal.ZERO : newAmount;
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, newReferenceMonth, paymentMethod, newServiceName, safeAmount, BigDecimal.ZERO, BigDecimal.ZERO, safeAmount, LocalDate.now(), 0, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withPaymentMethod(String newPaymentMethod) {
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, referenceMonth, newPaymentMethod, serviceName, baseAmount, fineAmount, interestAmount, amount, dueDate, daysLate, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withCharge(Charge charge) {
            long recalculatedDaysLate = charge.getDueDate() != null && charge.getDueDate().isBefore(LocalDate.now()) ? Math.max(0, ChronoUnit.DAYS.between(charge.getDueDate(), LocalDate.now())) : daysLate;
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, charge.getReferenceMonth(), paymentMethod, charge.getDescription(), charge.getAmount(), charge.getFineAmount(), charge.getInterestAmount(), charge.getTotalAmount(), charge.getDueDate(), recalculatedDaysLate, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        private static String firstNonBlankStatic(String... values) {
            if (values == null) return "";
            for (String value : values) {
                if (value != null && !value.isBlank()) return value.trim();
            }
            return "";
        }
    }
}
