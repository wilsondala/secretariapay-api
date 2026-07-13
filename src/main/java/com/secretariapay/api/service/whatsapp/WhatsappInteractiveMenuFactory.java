package com.secretariapay.api.service.whatsapp;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WhatsappInteractiveMenuFactory {

    private static final Pattern OPTION_PATTERN = Pattern.compile("(?m)^\\s*\\[(\\d{1,3})]\\s+(.+?)\\s*$");
    private static final Pattern STUDENT_PATTERN = Pattern.compile("(?im)^\\s*Estudante:\\s*(.+?)\\s*$");
    private static final Pattern REGISTRATION_PATTERN = Pattern.compile("(?im)^\\s*Matrícula:\\s*(.+?)\\s*$");
    private static final int MAX_ROWS = 10;
    private static final int MAX_TITLE = 24;
    private static final int MAX_DESCRIPTION = 72;
    private static final int MAX_BODY = 1024;

    public Optional<WhatsappInteractiveListMessage> fromReplyText(String replyText) {
        if (replyText == null || replyText.isBlank()) return Optional.empty();

        Matcher matcher = OPTION_PATTERN.matcher(replyText);
        List<ParsedOption> parsedOptions = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            String label = matcher.group(2) == null ? "" : matcher.group(2).trim();
            if (label.isBlank()) continue;
            parsedOptions.add(new ParsedOption(id, label));
            if (parsedOptions.size() > MAX_ROWS) return Optional.empty();
        }

        if (parsedOptions.size() < 2) return Optional.empty();

        boolean consolidatedBordereau = isConsolidatedBordereauMenu(replyText);
        List<WhatsappInteractiveListRow> rows = consolidatedBordereau
                ? buildConsolidatedBordereauRows(parsedOptions)
                : parsedOptions.stream().map(option -> toRow(option.id(), option.label(), replyText)).toList();

        if (rows.size() < 2) return Optional.empty();

        String body = consolidatedBordereau
                ? buildConsolidatedBordereauBody(replyText, parsedOptions)
                : buildDefaultBody(replyText);

        MenuPresentation presentation = resolvePresentation(replyText);
        if (body.isBlank()) body = presentation.defaultBody();
        body = limit(body, MAX_BODY);

        WhatsappInteractiveListSection section = new WhatsappInteractiveListSection(
                presentation.sectionTitle(),
                List.copyOf(rows)
        );

        return Optional.of(new WhatsappInteractiveListMessage(
                presentation.header(),
                body,
                "Toque para selecionar uma opção.",
                presentation.buttonLabel(),
                List.of(section),
                replyText
        ));
    }

    private String buildDefaultBody(String replyText) {
        return OPTION_PATTERN.matcher(replyText).replaceAll("")
                .replaceAll("(?m)^\\s*Responda com o número ou escreva o nome da opção:\\s*$", "")
                .replaceAll("(?m)^\\s*Escolha uma opção:\\s*$", "")
                .replaceAll("(?m)^\\s*Escolha a forma de pagamento:\\s*$", "")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private boolean isConsolidatedBordereauMenu(String replyText) {
        String normalized = normalize(replyText);
        return normalized.contains("comprovativos encontrados")
                && normalized.contains("escolha qual comprovativo deseja receber");
    }

    private List<WhatsappInteractiveListRow> buildConsolidatedBordereauRows(List<ParsedOption> parsedOptions) {
        ParsedOption firstPayment = parsedOptions.stream()
                .filter(option -> !normalize(option.label()).contains("voltar"))
                .findFirst()
                .orElse(null);
        ParsedOption back = parsedOptions.stream()
                .filter(option -> normalize(option.label()).contains("voltar"))
                .findFirst()
                .orElse(null);

        if (firstPayment == null || back == null) return List.of();

        return List.of(
                new WhatsappInteractiveListRow(
                        firstPayment.id(),
                        "Emitir borderô",
                        "Inclui todos os pagamentos validados"
                ),
                new WhatsappInteractiveListRow(
                        back.id(),
                        "Voltar",
                        "Retornar ao menu principal"
                )
        );
    }

    private String buildConsolidatedBordereauBody(String replyText, List<ParsedOption> parsedOptions) {
        String student = extractFirstGroup(STUDENT_PATTERN, replyText);
        String registration = extractFirstGroup(REGISTRATION_PATTERN, replyText);
        List<String> references = parsedOptions.stream()
                .filter(option -> !normalize(option.label()).contains("voltar"))
                .map(option -> extractReferenceLabel(option.label()))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        StringBuilder body = new StringBuilder("📄 Borderô financeiro consolidado.\n\n");
        if (!student.isBlank()) body.append("Estudante: ").append(student).append("\n");
        if (!registration.isBlank()) body.append("Matrícula: ").append(registration).append("\n");
        if (!student.isBlank() || !registration.isBlank()) body.append("\n");

        body.append("Pagamentos validados: ")
                .append(references.isEmpty() ? "registos disponíveis" : String.join(", ", references))
                .append(".\n\n")
                .append("O borderô reúne todo o histórico de pagamentos confirmados. Cada novo pagamento será acrescentado automaticamente.");

        return body.toString().trim();
    }

    private String extractReferenceLabel(String label) {
        if (label == null || label.isBlank()) return "";
        int separator = label.indexOf(" — ");
        if (separator < 0) separator = label.indexOf(" - ");
        return separator > 0 ? label.substring(0, separator).trim() : label.trim();
    }

    private String extractFirstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private WhatsappInteractiveListRow toRow(String id, String label, String fullMessage) {
        String title = label;
        String description = defaultDescription(id, label, fullMessage);

        int separator = label.indexOf(" — ");
        int separatorLength = 3;
        if (separator < 0) {
            separator = label.indexOf(" - ");
            separatorLength = 3;
        }

        if (separator > 0) {
            title = label.substring(0, separator).trim();
            String extracted = label.substring(separator + separatorLength).trim();
            if (!extracted.isBlank()) description = extracted;
        }

        if (title.length() > MAX_TITLE) {
            String original = title;
            title = abbreviate(title, MAX_TITLE);
            if (description == null || description.isBlank()) description = original;
        }

        return new WhatsappInteractiveListRow(
                id,
                limit(title, MAX_TITLE),
                description == null || description.isBlank() ? null : limit(description, MAX_DESCRIPTION)
        );
    }

    private String defaultDescription(String id, String label, String fullMessage) {
        String normalizedMessage = normalize(fullMessage);
        String normalizedLabel = normalize(label);

        if (normalizedMessage.contains("como posso ajudar")) {
            return switch (id) {
                case "1" -> "Consultar e pagar propinas";
                case "2" -> "Ver pagamentos e valores pendentes";
                case "3" -> "Consultar borderô de pagamentos";
                case "4" -> "Gerar guia de matrícula";
                case "5" -> "Gerar guia de recurso";
                case "6" -> "Gerar guia de declaração";
                case "7" -> "Solicitar atendimento humano";
                default -> null;
            };
        }

        if (normalizedMessage.contains("forma de pagamento")) {
            if (normalizedLabel.contains("multicaixa express")) return "Pagamento imediato pela AppyPay";
            if (normalizedLabel.contains("pagamento por referencia")) return "Entidade e referência AppyPay";
            if (normalizedLabel.contains("mesmo banco")) return "Transferência entre contas BAI";
            if (normalizedLabel.contains("outro banco") || normalizedLabel.contains("deposito")) return "Envio posterior do comprovativo";
            if (normalizedLabel.contains("infinitepay")) return "Disponível apenas para testes";
            if (normalizedLabel.contains("voltar")) return "Retornar ao menu anterior";
        }

        if (normalizedLabel.contains("voltar")) return "Retornar ao menu anterior";
        if (normalizedLabel.contains("falar com a dcr")) return "Solicitar atendimento humano";
        if (normalizedLabel.contains("tentar novamente")) return "Informar novamente os dados";
        if (normalizedLabel.contains("meses em atraso")) return "Consultar multas e juros";
        return null;
    }

    private MenuPresentation resolvePresentation(String message) {
        String normalized = normalize(message);
        if (normalized.contains("como posso ajudar")) {
            return new MenuPresentation("SecretáriaPay — IMETRO", "Atendimento financeiro académico", "Ver opções", "Serviços");
        }
        if (normalized.contains("meses disponiveis para pagamento")) {
            return new MenuPresentation("Propinas disponíveis", "Selecione a propina que deseja pagar", "Escolher propina", "Propinas");
        }
        if (normalized.contains("forma de pagamento")) {
            return new MenuPresentation("Forma de pagamento", "Escolha como deseja efetuar o pagamento", "Ver pagamentos", "Opções");
        }
        if (normalized.contains("situacao financeira academica")) {
            return new MenuPresentation("Situação financeira", "Escolha a próxima ação", "Ver ações", "Ações");
        }
        if (normalized.contains("comprovativos encontrados")) {
            return new MenuPresentation("Borderô financeiro", "Consultar histórico de pagamentos", "Ver borderô", "Pagamentos");
        }
        if (normalized.contains("nao encontrei nenhum cadastro")) {
            return new MenuPresentation("Cadastro não encontrado", "Escolha como deseja continuar", "Ver opções", "Próximos passos");
        }
        return new MenuPresentation("SecretáriaPay — IMETRO", "Escolha uma opção", "Ver opções", "Opções");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        String candidate = value.substring(0, maxLength).trim();
        int lastSpace = candidate.lastIndexOf(' ');
        if (lastSpace >= Math.max(8, maxLength / 2)) candidate = candidate.substring(0, lastSpace);
        return candidate.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength).trim();
    }

    private record ParsedOption(String id, String label) {
    }

    private record MenuPresentation(
            String header,
            String defaultBody,
            String buttonLabel,
            String sectionTitle
    ) {
    }
}
