package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import com.secretariapay.api.service.payment.AppyPayChargeResponse;
import com.secretariapay.api.service.payment.AppyPayPaymentGatewayService;
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
    private static final String PANEL_BASE_URL = "https://painel-secretariapay.paixaoangola.com";
    private static final BigDecimal PROPINA_AMOUNT = new BigDecimal("45000.00");
    private static final BigDecimal MATRICULA_AMOUNT = new BigDecimal("30000.00");
    private static final BigDecimal RECURSO_AMOUNT = new BigDecimal("15000.00");
    private static final BigDecimal DECLARACAO_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal TOTAL_EM_ABERTO = new BigDecimal("145000.00");

    private final Map<String, AppyPaySession> sessions = new ConcurrentHashMap<>();
    private final StudentRepository studentRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final AppyPayPaymentGatewayService appyPayPaymentGatewayService;
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
            @Value("${secretariapay.demo.email:dalawilson1244@gmail.com}") String demoEmail
    ) {
        super(studentRepository, chargeRepository, receiptRepository, sessionRepository, whatsAppCloudApiClient, mockAutomaticPaymentService);
        this.studentRepository = studentRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.appyPayPaymentGatewayService = appyPayPaymentGatewayService;
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
            return Optional.of("📎 Comprovativo recebido.\n\nO seu pagamento será analisado pela DCR. Após validação, o bordereau será enviado neste WhatsApp e por e-mail.");
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
            return Optional.of(askStudent("📄 Para localizar o bordereau/comprovativo já pago, preciso identificar o estudante."));
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

            Student student = studentOptional.get();
            AppyPaySession withStudent = session.withStudent(student);

            if ("SUMMARY".equals(session.action())) {
                sessions.put(phone, withStudent.withStep("WAITING_SUMMARY_ACTION"));
                return buildFinancialSummary(withStudent);
            }

            if ("BORDEREAU".equals(session.action())) {
                sessions.put(phone, withStudent.withStep("WAITING_RECEIPT_CHOICE"));
                return buildBordereauList(withStudent);
            }

            if ("MATRICULA".equals(session.action())) {
                AppyPaySession payment = withStudent.withPayment("Matrícula 2026", "Matrícula", MATRICULA_AMOUNT);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildServiceGuide(payment);
            }

            if ("RECURSO".equals(session.action())) {
                AppyPaySession payment = withStudent.withPayment("Recurso", "Exame de recurso", RECURSO_AMOUNT);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildServiceGuide(payment);
            }

            if ("DECLARACAO".equals(session.action())) {
                AppyPaySession payment = withStudent.withPayment("Declaração", "Declaração", DECLARACAO_AMOUNT);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildServiceGuide(payment);
            }

            sessions.put(phone, withStudent.withStep("WAITING_MONTH"));
            return buildStudentFoundAndAskMonth(withStudent);
        }

        if ("WAITING_SUMMARY_ACTION".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "pagar pendencias", "pagar pendências", "pendencias", "pendências")) {
                AppyPaySession payment = session.withPayment("Julho/2026", "Propinas pendentes", TOTAL_EM_ABERTO);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildGuidePrepared(payment, true);
            }
            if ("2".equals(normalized) || containsAny(normalized, "guia do mes", "guia do mês", "mes atual", "mês atual")) {
                AppyPaySession payment = session.withPayment("Julho/2026", "Propina mensal", PROPINA_AMOUNT);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildGuidePrepared(payment, false);
            }
            if ("3".equals(normalized) || isBordereauIntent(normalized)) {
                sessions.put(phone, session.withStep("WAITING_RECEIPT_CHOICE"));
                return buildBordereauList(session);
            }
            sessions.remove(phone);
            return buildMainMenu();
        }

        if ("WAITING_MONTH".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "mes atual", "mês atual", "propina atual")) {
                AppyPaySession payment = session.withPayment("Julho/2026", "Propina mensal", PROPINA_AMOUNT);
                sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
                return buildGuidePrepared(payment, false);
            }
            if ("3".equals(normalized) || containsAny(normalized, "atraso", "multa")) {
                sessions.put(phone, session.withStep("WAITING_OVERDUE_CHOICE"));
                return buildOverdueList(session);
            }
            if ("4".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }
            AppyPaySession payment = session.withPayment("Julho/2026", "Propina mensal", PROPINA_AMOUNT);
            sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
            return buildGuidePrepared(payment, false);
        }

        if ("WAITING_OVERDUE_CHOICE".equals(session.step())) {
            AppyPaySession payment = session.withPayment("Maio/2026 + Junho/2026", "Propinas em atraso", new BigDecimal("100000.00"));
            sessions.put(phone, payment.withStep("WAITING_PAYMENT"));
            return buildGuidePrepared(payment, true);
        }

        if ("WAITING_RECEIPT_CHOICE".equals(session.step())) {
            sessions.remove(phone);
            return sendReceiptAndBuildReply(phone, session.withPayment(resolveReceiptReference(normalized), "Reenvio de bordereau", PROPINA_AMOUNT), "Bordereau reenviado com sucesso.");
        }

        if ("WAITING_PAYMENT".equals(session.step())) {
            if ("5".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                sessions.remove(phone);
                return buildMainMenu();
            }
            if ("1".equals(normalized) || containsAny(normalized, "multicaixa", "express")) {
                AppyPaySession next = session.withPaymentMethod("Multicaixa Express").withStep("WAITING_APPYPAY_CONFIRM");
                sessions.put(phone, next);
                return createAppyPayChargeAndSendGuide(phone, next, true);
            }
            if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
                AppyPaySession next = session.withPaymentMethod("Pagamento por Referência").withStep("WAITING_APPYPAY_CONFIRM");
                sessions.put(phone, next);
                return createAppyPayChargeAndSendGuide(phone, next, false);
            }
            if ("3".equals(normalized) || containsAny(normalized, "transferencia", "transferência", "mesmo banco")) {
                AppyPaySession next = session.withPaymentMethod("Transferência mesmo banco").withStep("WAITING_APPYPAY_CONFIRM");
                sessions.put(phone, next);
                sendDemoGuidePdf(phone, next);
                return buildTransferSameBankTest(next);
            }
            if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "outro banco")) {
                AppyPaySession next = session.withPaymentMethod("Depósito bancário / transferência de outro banco").withStep("WAITING_PROOF");
                sessions.put(phone, next);
                sendDemoGuidePdf(phone, next);
                return buildBankPaymentInstructions(next);
            }
            return buildGuidePrepared(session, false);
        }

        if ("WAITING_APPYPAY_CONFIRM".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "confirmar", "pago", "paguei", "aprovado", "callback")) {
                sessions.remove(phone);
                return sendReceiptAndBuildReply(phone, session, "Pagamento confirmado com sucesso pelo fluxo de teste AppyPay.");
            }
            return "A cobrança AppyPay foi criada.\n\nPara concluir o teste sandbox, responda:\n[1] Confirmar retorno AppyPay aprovado";
        }

        if ("WAITING_PROOF".equals(session.step())) {
            return "📎 Por favor, envie o comprovativo de pagamento neste canal.\n\nA DCR fará a validação.";
        }

        sessions.remove(phone);
        return buildMainMenu();
    }

    private String createAppyPayChargeAndSendGuide(String phone, AppyPaySession session, boolean gpo) {
        String merchantTransactionId = "SPAY-" + session.studentNumber() + "-" + shortId();
        AppyPayChargeResponse appyPay = gpo
                ? appyPayPaymentGatewayService.createMulticaixaExpressCharge(session.amount(), session.serviceName(), merchantTransactionId, session.phone())
                : appyPayPaymentGatewayService.createReferenceCharge(session.amount(), session.serviceName(), merchantTransactionId);

        sendDemoGuidePdf(phone, session);

        String providerBlock = buildAppyPayBlock(appyPay);
        return ("""
                ✅ Guia de pagamento criada.

                Integração: AppyPay Sandbox
                Forma de pagamento: %s
                Valor: %s
                Referência: %s
                Vencimento: 10/07/2026

                %s

                O PDF da guia foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Para concluir o teste, responda:
                [1] Confirmar retorno AppyPay aprovado
                [2] Voltar
                """).formatted(session.paymentMethod(), money(session.amount()), session.referenceMonth(), providerBlock).trim();
    }

    private String buildAppyPayBlock(AppyPayChargeResponse response) {
        return ("""
                Estado AppyPay: %s
                MerchantTransactionId: %s
                Mensagem: %s
                """).formatted(
                safe(response.getStatus()),
                safe(response.getMerchantTransactionId()),
                safe(response.getMessage())
        ).trim();
    }

    private String buildTransferSameBankTest(AppyPaySession session) {
        return ("""
                ✅ Guia de pagamento criada.

                Forma de pagamento: Transferência mesmo banco
                Valor: %s
                Referência: %s
                Vencimento: 10/07/2026

                O PDF da guia foi enviado neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Para concluir o teste automático, responda:
                [1] Confirmar pagamento aprovado
                """).formatted(money(session.amount()), session.referenceMonth()).trim();
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

                Para continuar, informe um dado cadastrado no sistema:

                - Número de matrícula
                - Número do BI
                - Telefone cadastrado

                Exemplo:
                IMETRO-2026-TESTE-002
                """).formatted(intro).trim();
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
        return ("""
                ✅ Cadastro encontrado.

                Estudante: %s
                Matrícula: %s
                Ano académico: 2026
                Estado financeiro: Com propina disponível para pagamento

                Informe o mês de referência da propina:

                [1] Propina do mês atual
                [2] Escolher outro mês
                [3] Propinas em atraso e multas
                [4] Voltar ao menu principal
                """).formatted(session.studentName(), session.studentNumber()).trim();
    }

    private String buildGuidePrepared(AppyPaySession session, boolean overdue) {
        String warning = overdue ? "\n⚠️ Identificámos pendência, multa ou atraso associado a este cadastro.\n" : "";
        return ("""
                📌

                📄 Guia preparada.%s

                Estudante: %s
                Matrícula: %s
                Mês de referência: %s
                Valor da propina: %s
                Vencimento: 10/07/2026

                Escolha a forma de pagamento:

                [1] Multicaixa Express - AppyPay GPO
                [2] Pagamento por Referência - AppyPay REF
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [5] Voltar
                """).formatted(warning, session.studentName(), session.studentNumber(), session.referenceMonth(), money(session.amount())).trim();
    }

    private String buildOverdueList(AppyPaySession session) {
        return ("""
                📌 Propinas em atraso encontradas.

                Estudante: %s
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
                """).formatted(session.studentName(), session.studentNumber()).trim();
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
                [5] Voltar
                """).formatted(session.serviceName(), session.studentName(), session.studentNumber(), session.serviceName(), money(session.amount())).trim();
    }

    private String buildFinancialSummary(AppyPaySession session) {
        return ("""
                📊 Situação Financeira Académica

                Estudante: %s
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
                """).formatted(session.studentName(), session.studentNumber()).trim();
    }

    private String buildBordereauList(AppyPaySession session) {
        return ("""
                📄 Bordereaux encontrados.

                Estudante: %s
                Matrícula: %s

                Escolha qual comprovativo deseja receber:

                [1] Propina Julho/2026 — 45.000,00 Kz
                [2] Matrícula 2026 — valor cadastrado
                [3] Recurso — valor cadastrado
                [4] Voltar
                """).formatted(session.studentName(), session.studentNumber()).trim();
    }

    private String buildBankPaymentInstructions(AppyPaySession session) {
        return ("""
                🏦 Dados para pagamento bancário

                Banco: Banco de exemplo
                IBAN: AO06 0000 0000 0000 0000 0000 0
                Titular: Instituto Superior Politécnico Metropolitano de Angola
                Valor: %s

                Após o pagamento, envie o comprovativo neste canal para validação pela DCR.
                """).formatted(money(session.amount())).trim();
    }

    private String sendReceiptAndBuildReply(String phone, AppyPaySession session, String intro) {
        String receipt = "BORD-DEMO-" + shortId();
        sendDemoReceiptPdf(phone, session, receipt);
        return ("""
                ✅ %s

                📄 O bordereau/comprovativo foi emitido em PDF.

                Forma de pagamento: %s
                Referência: %s
                Valor pago: %s
                Bordereau: %s

                Enviei o PDF neste WhatsApp.
                📧 Também enviei uma cópia para o e-mail cadastrado.

                Obrigado. Foi um prazer atender.
                """).formatted(intro, firstNonBlank(session.paymentMethod(), "AppyPay"), session.referenceMonth(), money(session.amount()), receipt).trim();
    }

    private void sendDemoGuidePdf(String phone, AppyPaySession session) {
        String code = "GUIA-WPP-" + shortId();
        String pdfUrl = API_BASE_URL + "/api/v1/public/guides/" + encode(code) + "/pdf";
        String guideUrl = PANEL_BASE_URL + "/guias/" + encode(code);
        String caption = ("""
                SecretáriaPay Académico: guia de pagamento em PDF.

                Estudante: %s
                Matrícula: %s
                Referência: %s
                Serviço: %s
                Valor: %s

                Guia online: %s
                """).formatted(session.studentName(), session.studentNumber(), session.referenceMonth(), session.serviceName(), money(session.amount()), guideUrl).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, "guia-" + code + ".pdf", caption);
        sendGuideEmail(session, code, guideUrl);
    }

    private void sendDemoReceiptPdf(String phone, AppyPaySession session, String receipt) {
        String pdfUrl = API_BASE_URL + "/api/v1/public/demo/receipts/" + encode(receipt) + "/pdf"
                + "?student=" + encode(session.studentName())
                + "&month=" + encode(session.referenceMonth())
                + "&method=" + encode(firstNonBlank(session.paymentMethod(), "AppyPay"));
        String caption = ("""
                SecretáriaPay Académico: bordereau/comprovativo emitido.

                Bordereau: %s
                Estudante: %s
                Matrícula: %s
                Referência: %s
                Valor: %s
                """).formatted(receipt, session.studentName(), session.studentNumber(), session.referenceMonth(), money(session.amount())).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, "bordereau-" + receipt + ".pdf", caption);
        sendReceiptEmail(session, receipt, pdfUrl);
    }

    private void sendGuideEmail(AppyPaySession session, String code, String guideUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(session.studentName());
        request.setStudentNumber(session.studentNumber());
        request.setEmail(firstNonBlank(session.email(), demoEmail));
        request.setGuideCode(code);
        request.setGuideUrl(guideUrl);
        request.setAmount(session.amount());
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 10));
        request.setMessage("Guia emitida automaticamente pelo atendimento financeiro académico do IMETRO via SecretáriaPay.");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private void sendReceiptEmail(AppyPaySession session, String receipt, String pdfUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(session.studentName());
        request.setStudentNumber(session.studentNumber());
        request.setEmail(firstNonBlank(session.email(), demoEmail));
        request.setGuideCode(receipt);
        request.setGuideUrl(pdfUrl);
        request.setAmount(session.amount());
        request.setCurrency("AOA");
        request.setDueDate(LocalDate.of(2026, 7, 10));
        request.setMessage("Bordereau/comprovativo emitido automaticamente pelo SecretáriaPay após confirmação do pagamento.");
        fallbackNotificationService.sendGuideByEmail(request);
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

    private String resolveReceiptReference(String choice) {
        if ("2".equals(choice)) return "Matrícula 2026";
        if ("3".equals(choice)) return "Recurso";
        return "Propina Julho/2026";
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Bom dia";
        if (hour >= 12 && hour < 18) return "Boa tarde";
        return "Boa noite";
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " Kz";
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private boolean isMedia(String type) {
        return "image".equalsIgnoreCase(type) || "document".equalsIgnoreCase(type);
    }

    private boolean isPropinaIntent(String value) {
        return containsAny(value, "propina", "propinas", "mensalidade", "pagar propina", "pagar mensalidade", "mes atual", "mês atual", "propina atual");
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

    private void clearExpired(String phone) {
        AppyPaySession session = sessions.get(phone);
        if (session != null && session.expiresAt().isBefore(LocalDateTime.now())) sessions.remove(phone);
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
            BigDecimal amount,
            LocalDateTime expiresAt
    ) {
        static AppyPaySession waitingStudent(String action) {
            return new AppyPaySession("WAITING_STUDENT", action, "", "", "", "", "", "", "", BigDecimal.ZERO, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withStudent(Student student) {
            return new AppyPaySession(
                    step,
                    action,
                    student.getStudentNumber(),
                    student.getFullName(),
                    student.getEmail(),
                    firstNonBlankStatic(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone()),
                    referenceMonth,
                    paymentMethod,
                    serviceName,
                    amount,
                    LocalDateTime.now().plusMinutes(SESSION_MINUTES)
            );
        }

        AppyPaySession withStep(String step) {
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, referenceMonth, paymentMethod, serviceName, amount, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withPayment(String referenceMonth, String serviceName, BigDecimal amount) {
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, referenceMonth, paymentMethod, serviceName, amount, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AppyPaySession withPaymentMethod(String paymentMethod) {
            return new AppyPaySession(step, action, studentNumber, studentName, email, phone, referenceMonth, paymentMethod, serviceName, amount, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
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
