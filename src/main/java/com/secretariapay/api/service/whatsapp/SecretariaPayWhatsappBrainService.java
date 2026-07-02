package com.secretariapay.api.service.whatsapp;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecretariaPayWhatsappBrainService {

    private static final Pattern ANGOLA_BI_PATTERN =
            Pattern.compile(".*\\b\\d{8,9}[a-zA-Z]{2}\\d{3}\\b.*");

    private static final Pattern CHARGE_CODE_PATTERN =
            Pattern.compile(".*\\bCHG\\d{6,}\\b.*", Pattern.CASE_INSENSITIVE);

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
                    De nada. Estou aqui para ajudar.

                    Quando precisar, pode escolher uma opção:
                    1. Consultar propina
                    2. Enviar comprovativo
                    3. Ver recibos
                    4. Falar com a secretaria
                    """.trim();
        }

        if (looksLikeBiOrStudentNumber(message)) {
            return """
                    Recebi os seus dados.

                    Vou encaminhar a consulta da sua situação financeira. Para uma validação mais segura, envie também o seu nome completo.

                    Se deseja continuar pelo menu, responda:
                    1. Consultar propina
                    2. Enviar comprovativo
                    3. Ver recibos
                    4. Falar com a secretaria
                    """.trim();
        }

        if (looksLikeChargeCode(message)) {
            return """
                    Recebi o código da cobrança.

                    A secretaria financeira poderá validar este código e confirmar a situação do pagamento. Caso já tenha pago, envie o comprovativo por aqui em imagem ou PDF.
                    """.trim();
        }

        if (isPropinaIntent(normalized)) {
            return """
                    Claro. Para consultar a sua propina, envie por favor um destes dados:

                    • Número de estudante
                    • BI
                    • Nome completo

                    Exemplo:
                    BI 000000000LA000
                    """.trim();
        }

        if (isComprovativoIntent(normalized)) {
            return """
                    Pode enviar o comprovativo por aqui em imagem ou PDF.

                    Depois do envio, a tesouraria fará a validação. Assim que aprovado, o sistema enviará o recibo digital e atualizará a sua situação académica.
                    """.trim();
        }

        if (isReceiptIntent(normalized)) {
            return """
                    Para localizar o seu recibo, envie um destes dados:

                    • Número de estudante
                    • BI
                    • Código da cobrança

                    Exemplo:
                    CHG1783012061065
                    """.trim();
        }

        if (isHumanIntent(normalized)) {
            return """
                    Entendido. Vou encaminhar o seu pedido para a secretaria.

                    Para agilizar o atendimento, envie por favor:
                    • Nome completo
                    • Número de estudante ou BI
                    • Motivo do contacto
                    """.trim();
        }

        if (isPaymentMethodIntent(normalized)) {
            return """
                    Os meios de pagamento dependem da configuração da instituição.

                    Normalmente o SecretáriaPay pode trabalhar com:
                    • Referência de pagamento
                    • Transferência bancária
                    • Multicaixa Express
                    • Unitel Money
                    • Afrimoney
                    • Upload de comprovativo pelo WhatsApp

                    Para este atendimento, envie o seu número de estudante ou BI para localizar a cobrança.
                    """.trim();
        }

        if (isDelayIntent(normalized)) {
            return """
                    Entendi. Para verificar atraso, multa ou juros, envie o seu número de estudante ou BI.

                    A secretaria poderá confirmar o valor atualizado e orientar a regularização.
                    """.trim();
        }

        if (isScheduleIntent(normalized)) {
            return """
                    O horário de atendimento pode variar conforme a secretaria da instituição.

                    Para este canal, pode deixar a sua solicitação por aqui. Envie o seu nome completo, BI ou número de estudante e o motivo do contacto.
                    """.trim();
        }

        if (isMatriculaIntent(normalized)) {
            return """
                    Para assuntos de matrícula, inscrição ou situação académica, envie:

                    • Nome completo
                    • BI
                    • Curso
                    • Ano académico ou turma, se souber

                    A secretaria analisará a sua solicitação.
                    """.trim();
        }

        if (isNumericOption(normalized)) {
            return switch (normalized) {
                case "1" -> """
                        Claro. Para consultar a sua propina, envie por favor o seu número de estudante ou BI.

                        Exemplo:
                        BI 000000000LA000
                        """.trim();
                case "2" -> """
                        Pode enviar o comprovativo por aqui em imagem ou PDF.

                        Depois do envio, a tesouraria fará a validação e o sistema enviará o recibo digital.
                        """.trim();
                case "3" -> """
                        Para localizar recibos, envie o seu número de estudante, BI ou código da cobrança.

                        Exemplo:
                        CHG1783012061065
                        """.trim();
                case "4" -> """
                        Certo. Vou encaminhar para a secretaria.

                        Envie por favor o seu nome completo, número de estudante ou BI e descreva o que precisa.
                        """.trim();
                default -> fallback();
            };
        }

        return fallback();
    }

    private String greetingMenu() {
        String greeting = greetingByTime();

        return (greeting + """
                ! Aqui é a SecretáriaPay Académico.

                Posso ajudar com:
                1. Consultar propina
                2. Enviar comprovativo
                3. Ver recibos
                4. Falar com a secretaria

                Pode responder com o número da opção ou escrever o que precisa.
                """).trim();
    }

    private String comprovativoRecebido() {
        return """
                Comprovativo recebido.

                A tesouraria fará a validação. Assim que aprovado, o sistema enviará o recibo digital e atualizará a sua situação académica.

                Se quiser, envie também o seu nome completo e número de estudante ou BI para facilitar a identificação.
                """.trim();
    }

    private String fallback() {
        return """
                Entendi.

                Para te ajudar melhor, escolha uma opção:
                1. Consultar propina
                2. Enviar comprovativo
                3. Ver recibos
                4. Falar com a secretaria

                Também pode enviar o seu BI, número de estudante ou código da cobrança.
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
                "ola",
                "oi",
                "bom dia",
                "boa tarde",
                "boa noite",
                "saudacoes",
                "saudacao",
                "hello",
                "menu",
                "inicio",
                "comecar",
                "começar"
        ));
    }

    private boolean isThanks(String value) {
        return containsAny(value, List.of(
                "obrigado",
                "obrigada",
                "valeu",
                "muito obrigado",
                "agradeco",
                "agradeço"
        ));
    }

    private boolean isPropinaIntent(String value) {
        return containsAny(value, List.of(
                "propina",
                "mensalidade",
                "consultar",
                "cobranca",
                "cobrança",
                "divida",
                "dívida",
                "quanto devo",
                "valor em aberto",
                "pagamento em aberto",
                "situacao financeira",
                "situação financeira"
        ));
    }

    private boolean isComprovativoIntent(String value) {
        return containsAny(value, List.of(
                "comprovativo",
                "comprovante",
                "paguei",
                "pagamento",
                "transferencia",
                "transferência",
                "recibo do banco",
                "enviei dinheiro",
                "multicaixa",
                "unitel money",
                "afrimoney"
        ));
    }

    private boolean isReceiptIntent(String value) {
        return containsAny(value, List.of(
                "recibo",
                "recibos",
                "segunda via",
                "2 via",
                "comprovante institucional",
                "confirmacao de pagamento",
                "confirmação de pagamento"
        ));
    }

    private boolean isHumanIntent(String value) {
        return containsAny(value, List.of(
                "falar",
                "secretaria",
                "atendente",
                "humano",
                "tesouraria",
                "financeiro",
                "operador",
                "pessoa",
                "ajuda"
        ));
    }

    private boolean isPaymentMethodIntent(String value) {
        return containsAny(value, List.of(
                "como pagar",
                "forma de pagamento",
                "meio de pagamento",
                "referencia",
                "referência",
                "iban",
                "banco",
                "multicaixa express",
                "unitel money",
                "afrimoney"
        ));
    }

    private boolean isDelayIntent(String value) {
        return containsAny(value, List.of(
                "atraso",
                "atrasado",
                "multa",
                "juros",
                "bloqueado",
                "bloqueio",
                "restricao",
                "restrição",
                "regularizar",
                "regularizacao",
                "regularização"
        ));
    }

    private boolean isScheduleIntent(String value) {
        return containsAny(value, List.of(
                "horario",
                "horário",
                "quando abre",
                "quando fecha",
                "atendimento",
                "expediente"
        ));
    }

    private boolean isMatriculaIntent(String value) {
        return containsAny(value, List.of(
                "matricula",
                "matrícula",
                "inscricao",
                "inscrição",
                "curso",
                "turma",
                "classe",
                "ano academico",
                "ano académico",
                "declaracao",
                "declaração",
                "certificado"
        ));
    }

    private boolean isNumericOption(String value) {
        return value != null && value.matches("[1-4]");
    }

    private boolean looksLikeBiOrStudentNumber(String value) {
        String clean = safe(value).trim();

        if (clean.isBlank()) {
            return false;
        }

        String normalized = normalize(clean);

        return ANGOLA_BI_PATTERN.matcher(clean).matches()
                || normalized.startsWith("bi ")
                || normalized.startsWith("bi:")
                || normalized.startsWith("estudante ")
                || normalized.startsWith("numero ")
                || normalized.startsWith("n ")
                || normalized.startsWith("nº ")
                || normalized.startsWith("n. ");
    }

    private boolean looksLikeChargeCode(String value) {
        return CHARGE_CODE_PATTERN.matcher(safe(value)).matches();
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