package com.secretariapay.api.service.whatsapp;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecretariaPayWhatsappBrainService {

    private static final String IMETRO_NAME = "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";

    private static final Pattern ANGOLA_BI_PATTERN =
            Pattern.compile(".*\\b\\d{8,9}[a-zA-Z]{2}\\d{3}\\b.*");

    private static final Pattern CHARGE_CODE_PATTERN =
            Pattern.compile(".*\\b(?:CHG\\d{6,}|IMT-[A-Z0-9_\\-]+)\\b.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(".*\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(".*(?:\\+?244|\\+?55)?[0-9][0-9\\s().-]{7,}.*");

    public String buildReply(String messageType, String rawMessage) {
        String type = safe(messageType).toLowerCase();
        String message = safe(rawMessage);
        String normalized = normalize(message);

        if ("image".equals(type) || "document".equals(type)) {
            return comprovativoRecebido();
        }

        if (isGreeting(normalized)) {
            return greetingMenu();
        }

        if (isThanks(normalized)) {
            return """
                    De nada. Estou aqui para ajudar no atendimento académico-financeiro do IMETRO.

                    Pode escolher uma opção:
                    1. Consultar propina ou dívida
                    2. Enviar comprovativo
                    3. Ver recibos
                    4. Falar com a DCR/Secretaria
                    """.trim();
        }

        if (looksLikeRegisteredIdentifier(message)) {
            return """
                    Recebi os dados informados.

                    Por segurança, o SecretáriaPay só envia guias, situação financeira e recibos para os contactos cadastrados oficialmente no IMETRO.

                    Dados aceites para localização:
                    • Número de estudante/carteira
                    • E-mail cadastrado
                    • Telefone cadastrado
                    • Código da cobrança

                    Se o cadastro existir, a informação será enviada apenas para o WhatsApp, telefone/SMS ou e-mail registado na universidade.
                    """.trim();
        }

        if (isPropinaIntent(normalized)) {
            return """
                    Claro. Para consultar propina, dívida ou guia de pagamento, envie um destes dados cadastrados no IMETRO:

                    • Número de estudante/carteira
                    • E-mail cadastrado
                    • Telefone cadastrado
                    • Código da cobrança

                    Regra de segurança: mesmo que a solicitação venha de outro telefone, a informação será enviada apenas para os contactos oficiais cadastrados na universidade.
                    """.trim();
        }

        if (isComprovativoIntent(normalized)) {
            return """
                    Pode enviar o comprovativo por aqui em imagem ou PDF.

                    A DCR — Divisão de Cobranças e Recebimentos fará a validação manual. O recibo institucional só será emitido após confirmação da DCR.

                    Para facilitar a identificação, envie também o número de estudante/carteira ou o código da cobrança.
                    """.trim();
        }

        if (isReceiptIntent(normalized)) {
            return """
                    Para localizar recibos, envie um destes dados:

                    • Número de estudante/carteira
                    • E-mail cadastrado
                    • Telefone cadastrado
                    • Código da cobrança

                    Por segurança, o recibo será enviado apenas para os contactos cadastrados oficialmente no IMETRO.
                    """.trim();
        }

        if (isHumanIntent(normalized)) {
            return """
                    Entendido. Vou orientar o atendimento com a DCR/Secretaria do IMETRO.

                    Envie por favor:
                    • Número de estudante/carteira
                    • Nome completo
                    • Motivo do contacto

                    Não envie dados sensíveis de terceiros. O sistema valida as informações com o cadastro oficial da universidade.
                    """.trim();
        }

        if (isPaymentMethodIntent(normalized)) {
            return """
                    As formas de pagamento permanecem conforme a guia oficial gerada pelo IMETRO.

                    A guia pode trazer dados bancários, IBAN, Multicaixa Express e instruções definidas pela instituição.

                    Envie o número de estudante/carteira, e-mail cadastrado, telefone cadastrado ou código da cobrança para localizar a guia.
                    """.trim();
        }

        if (isDelayIntent(normalized)) {
            return """
                    Para verificar atraso, multa, dívida ou inadimplência, envie um destes dados cadastrados no IMETRO:

                    • Número de estudante/carteira
                    • E-mail cadastrado
                    • Telefone cadastrado
                    • Código da cobrança

                    A DCR valida os pagamentos e regularizações manualmente.
                    """.trim();
        }

        if (isScheduleIntent(normalized)) {
            return """
                    Este é o canal digital académico-financeiro do IMETRO.

                    As solicitações podem ser recebidas por aqui, mas cobranças, guias e recibos só são enviados para contactos cadastrados oficialmente na universidade.
                    """.trim();
        }

        if (isMatriculaIntent(normalized)) {
            return """
                    Para matrícula, inscrição, declaração, certificado ou situação académica, envie:

                    • Número de estudante/carteira
                    • Nome completo
                    • Curso/turma, se souber
                    • Motivo da solicitação

                    A secretaria do IMETRO analisará conforme o cadastro académico oficial.
                    """.trim();
        }

        if (isNumericOption(normalized)) {
            return switch (normalized) {
                case "1" -> """
                        Para consultar propina ou dívida, envie o número de estudante/carteira, e-mail cadastrado, telefone cadastrado ou código da cobrança.

                        A resposta financeira será enviada apenas para os contactos oficiais cadastrados no IMETRO.
                        """.trim();
                case "2" -> """
                        Pode enviar o comprovativo por aqui em imagem ou PDF.

                        A DCR fará a validação manual. O recibo só será emitido após confirmação.
                        """.trim();
                case "3" -> """
                        Para localizar recibos, envie o número de estudante/carteira, e-mail cadastrado, telefone cadastrado ou código da cobrança.
                        """.trim();
                case "4" -> """
                        Certo. Para encaminhar corretamente, envie o número de estudante/carteira, nome completo e motivo do contacto.
                        """.trim();
                default -> fallback();
            };
        }

        return fallback();
    }

    private String greetingMenu() {
        String greeting = greetingByTime();

        return (greeting + """
                ! Aqui é o atendimento digital do SecretáriaPay Académico para o IMETRO.

                Instituição: Instituto Superior Politécnico Metropolitano de Angola (IMETRO)
                Área: DCR — Divisão de Cobranças e Recebimentos

                Posso ajudar com:
                1. Consultar propina ou dívida
                2. Enviar comprovativo
                3. Ver recibos
                4. Falar com a DCR/Secretaria

                Regra de segurança: guias, recibos e situação financeira só são enviados para os contactos cadastrados oficialmente na universidade.
                """).trim();
    }

    private String comprovativoRecebido() {
        return """
                Comprovativo recebido.

                A DCR — Divisão de Cobranças e Recebimentos fará a validação manual. O recibo institucional só será emitido após confirmação da DCR.

                Envie também o número de estudante/carteira ou o código da cobrança para facilitar a identificação.
                """.trim();
    }

    private String fallback() {
        return """
                Entendi.

                Para te ajudar melhor, escolha uma opção:
                1. Consultar propina ou dívida
                2. Enviar comprovativo
                3. Ver recibos
                4. Falar com a DCR/Secretaria

                Também pode enviar número de estudante/carteira, e-mail cadastrado, telefone cadastrado ou código da cobrança.

                Por segurança, as informações financeiras só são enviadas para contactos cadastrados oficialmente no IMETRO.
                """.trim();
    }

    private String greetingByTime() {
        int hour = LocalTime.now().getHour();

        if (hour >= 5 && hour < 12) {
            return "Bom dia";
        }

        if (hour >= 12 && hour < 18) {
            return "Boa tarde";
        }

        return "Boa noite";
    }

    private boolean isGreeting(String value) {
        return containsAny(value, List.of(
                "ola", "olá", "oi", "bom dia", "boa tarde", "boa noite", "saudacoes", "saudação", "menu", "inicio", "início", "comecar", "começar"
        ));
    }

    private boolean isThanks(String value) {
        return containsAny(value, List.of("obrigado", "obrigada", "valeu", "muito obrigado", "agradeco", "agradeço"));
    }

    private boolean isPropinaIntent(String value) {
        return containsAny(value, List.of(
                "propina", "mensalidade", "consultar", "cobranca", "cobrança", "divida", "dívida", "quanto devo", "valor em aberto", "pagamento em aberto", "situacao financeira", "situação financeira", "guia", "boleto"
        ));
    }

    private boolean isComprovativoIntent(String value) {
        return containsAny(value, List.of(
                "comprovativo", "comprovante", "paguei", "pagamento", "transferencia", "transferência", "recibo do banco", "enviei dinheiro", "multicaixa", "unitel money", "afrimoney"
        ));
    }

    private boolean isReceiptIntent(String value) {
        return containsAny(value, List.of(
                "recibo", "recibos", "segunda via", "2 via", "comprovante institucional", "confirmacao de pagamento", "confirmação de pagamento"
        ));
    }

    private boolean isHumanIntent(String value) {
        return containsAny(value, List.of(
                "falar", "secretaria", "atendente", "humano", "tesouraria", "financeiro", "operador", "pessoa", "ajuda", "dcr"
        ));
    }

    private boolean isPaymentMethodIntent(String value) {
        return containsAny(value, List.of(
                "como pagar", "forma de pagamento", "meio de pagamento", "referencia", "referência", "iban", "banco", "multicaixa express", "unitel money", "afrimoney"
        ));
    }

    private boolean isDelayIntent(String value) {
        return containsAny(value, List.of(
                "atraso", "atrasado", "multa", "juros", "bloqueado", "bloqueio", "restricao", "restrição", "regularizar", "regularizacao", "regularização", "inadimplencia", "inadimplência"
        ));
    }

    private boolean isScheduleIntent(String value) {
        return containsAny(value, List.of("horario", "horário", "quando abre", "quando fecha", "atendimento", "expediente"));
    }

    private boolean isMatriculaIntent(String value) {
        return containsAny(value, List.of(
                "matricula", "matrícula", "inscricao", "inscrição", "curso", "turma", "classe", "ano academico", "ano académico", "declaracao", "declaração", "certificado", "tfc", "formatura"
        ));
    }

    private boolean isNumericOption(String value) {
        return value != null && value.matches("[1-4]");
    }

    private boolean looksLikeRegisteredIdentifier(String value) {
        String clean = safe(value).trim();

        if (clean.isBlank()) {
            return false;
        }

        String normalized = normalize(clean);

        return ANGOLA_BI_PATTERN.matcher(clean).matches()
                || CHARGE_CODE_PATTERN.matcher(clean).matches()
                || EMAIL_PATTERN.matcher(clean).matches()
                || PHONE_PATTERN.matcher(clean).matches()
                || normalized.startsWith("bi ")
                || normalized.startsWith("bi:")
                || normalized.startsWith("estudante ")
                || normalized.startsWith("numero ")
                || normalized.startsWith("n ")
                || normalized.startsWith("nº ")
                || normalized.startsWith("n. ")
                || normalized.startsWith("carteira ")
                || normalized.startsWith("email ")
                || normalized.startsWith("telefone ")
                || normalized.startsWith("telemovel ")
                || normalized.startsWith("telemóvel ");
    }

    private boolean containsAny(String value, List<String> keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase()
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
