package com.secretariapay.api.service;

import com.secretariapay.api.entity.enums.WhatsappSessionType;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * LEGADO TEMPORÁRIO.
 * FAQ legado do fluxo antigo de passagens/VaiRápido. Não usar para o SecretáriaPay Académico. Manter temporariamente até remoção controlada.
 */
@Service
@Deprecated(since = "2026-07-02", forRemoval = false)
public class WhatsappFaqAnswerService {

    public Optional<String> answer(
            String messageText,
            WhatsappSessionType sessionType
    ) {
        String normalizedMessage = normalizeText(messageText);

        if (normalizedMessage.isBlank()) {
            return Optional.empty();
        }

        if (isGreeting(normalizedMessage)) {
            return Optional.of(greetingAnswer(sessionType));
        }

        if (containsAny(
                normalizedMessage,
                "forma de pagamento",
                "formas de pagamento",
                "como pagar",
                "pagar como",
                "aceita pix",
                "pix",
                "cartao",
                "cartão",
                "transferencia",
                "transferência",
                "multicaixa",
                "unitel money",
                "afrimoney",
                "aoa",
                "kwanza",
                "kz",
                "pagamento angola"
        )) {
            return Optional.of(paymentAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "angola",
                "brasil",
                "pais",
                "país",
                "bi",
                "cpf",
                "passaporte",
                "documento",
                "documentos"
        )) {
            return Optional.of(countryAndDocumentAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "preco",
                "preço",
                "valor",
                "quanto custa",
                "custa quanto",
                "tarifa"
        )) {
            return Optional.of(priceAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "horario",
                "horário",
                "hora",
                "rotas",
                "rota",
                "viagens disponiveis",
                "viagens disponíveis",
                "destinos",
                "tem viagem"
        )) {
            return Optional.of(tripSearchAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "como comprar",
                "comprar passagem",
                "comprar bilhete",
                "quero viajar",
                "como funciona a compra",
                "passo a passo"
        )) {
            return Optional.of(howToBuyAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "bilhete",
                "ticket",
                "pdf",
                "qr code",
                "qrcode",
                "codigo qr",
                "código qr",
                "validacao",
                "validação"
        )) {
            return Optional.of(ticketAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "cancelar",
                "cancelamento",
                "reembolso",
                "devolver dinheiro",
                "desistir"
        )) {
            return Optional.of(cancelAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "suporte",
                "atendente",
                "humano",
                "falar com alguem",
                "falar com alguém",
                "ajuda urgente",
                "problema"
        )) {
            return Optional.of(supportAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "api",
                "endpoint",
                "integracao",
                "integração",
                "webhook",
                "desenvolvedor",
                "documentacao",
                "documentação"
        )) {
            return Optional.of(apiAnswer());
        }

        if (containsAny(
                normalizedMessage,
                "menu",
                "opcoes",
                "opções",
                "ajuda"
        )) {
            return Optional.of(menuAnswer(sessionType));
        }

        return Optional.empty();
    }

    private String greetingAnswer(WhatsappSessionType sessionType) {
        if (WhatsappSessionType.USER.equals(sessionType)) {
            return """
                    Olá. Bem-vindo ao VaiRápido Operacional.

                    Posso ajudar com:
                    1. Validar bilhete
                    2. Consultar resumo
                    3. Ver comandos disponíveis

                    Exemplos:
                    - Validar bilhete VRTK-...
                    - Resumo de hoje
                    - Menu
                    """.trim();
        }

        return """
                Olá. Bem-vindo ao VaiRápido.

                Posso ajudar você a comprar passagem pelo WhatsApp no Brasil e em Angola.

                🌍 Como funciona:
                Eu identifico o país automaticamente pela cidade de origem e destino.

                🇦🇴 Angola
                Documento: BI ou Passaporte
                Moeda: AOA
                Pagamento: Multicaixa, Unitel Money, Afrimoney ou dinheiro

                🇧🇷 Brasil
                Documento: CPF ou Passaporte
                Moeda: BRL
                Pagamento: Pix ou dinheiro

                Para começar, envie:
                Comprar passagem

                Ou envie direto:

                🇦🇴 Exemplo Angola:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Exemplo Brasil:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026
                """.trim();
    }

    private String paymentAnswer() {
        return """
                Formas de pagamento disponíveis no VaiRápido:

                🇦🇴 Angola
                - Multicaixa
                - Unitel Money
                - Afrimoney
                - Dinheiro

                🇧🇷 Brasil
                - Pix
                - Dinheiro

                No ambiente atual, o pagamento ainda é confirmado de forma simulada para testes.
                Depois da reserva, envie:
                Pagar reserva

                Em seguida, o sistema libera a emissão do bilhete digital.
                """.trim();
    }

    private String countryAndDocumentAnswer() {
        return """
                O VaiRápido trabalha com regras por país.

                🇦🇴 Angola
                Documento principal: BI
                Documento alternativo: Passaporte
                Moeda: AOA
                Pagamentos: Multicaixa, Unitel Money, Afrimoney ou dinheiro

                🇧🇷 Brasil
                Documento principal: CPF
                Documento alternativo: Passaporte
                Moeda: BRL
                Pagamentos: Pix ou dinheiro

                O país é identificado automaticamente pelas cidades informadas.
                Exemplo:
                Luanda → Benguela identifica Angola.
                São Paulo → Rio de Janeiro identifica Brasil.
                """.trim();
    }

    private String priceAnswer() {
        return """
                O valor depende da rota, data, horário e empresa de transporte.

                Para consultar preço e horários, envie origem, destino e data.

                🇦🇴 Exemplo Angola:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Exemplo Brasil:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026

                O sistema vai listar as viagens disponíveis com preço, moeda e lugares.
                """.trim();
    }

    private String tripSearchAnswer() {
        return """
                Para consultar rotas, horários e viagens disponíveis, envie:

                🇦🇴 Exemplo Angola:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Exemplo Brasil:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026

                Depois responda apenas com o número da viagem.
                Exemplo:
                1

                O sistema cria uma reserva provisória para você.
                """.trim();
    }

    private String howToBuyAnswer() {
        return """
                Para comprar sua passagem pelo VaiRápido:

                1. Envie origem, destino e data
                2. O sistema identifica se é Brasil ou Angola
                3. Escolha uma viagem
                4. Informe os dados do passageiro
                5. Confirme o pagamento
                6. Emita o bilhete digital
                7. Receba o PDF no WhatsApp

                🇦🇴 Exemplo Angola:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Exemplo Brasil:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026
                """.trim();
    }

    private String ticketAnswer() {
        return """
                O bilhete digital do VaiRápido é enviado pelo WhatsApp.

                Ele contém:
                - Código do bilhete
                - Código da reserva
                - Passageiro
                - Trecho
                - Data e horário da viagem
                - Poltrona
                - Valor e moeda
                - QR Code de validação
                - PDF do bilhete

                Depois do pagamento, envie:
                Emitir bilhete
                """.trim();
    }

    private String cancelAnswer() {
        return """
                O cancelamento ainda está em fase de estruturação no fluxo WhatsApp.

                Para o MVP atual:
                - Reserva sem pagamento expira automaticamente
                - Reserva paga deve ser tratada pelo suporte
                - Bilhete usado não pode ser cancelado

                Para ajuda, envie:
                Falar com suporte
                """.trim();
    }

    private String supportAnswer() {
        return """
                Entendi. Você quer falar com suporte.

                No MVP atual, o atendimento humano ainda será conectado.

                Enquanto isso, envie uma destas opções:
                - Comprar passagem
                - Consultar horários
                - Formas de pagamento
                - Documentos aceitos
                - Emitir bilhete
                """.trim();
    }

    private String apiAnswer() {
        return """
                A API do VaiRápido já está ativa para testes.

                Recursos já validados:
                - Webhook WhatsApp
                - Busca de viagens
                - Resolução automática de país por cidade
                - Brasil e Angola no mesmo backend
                - Criação de reserva
                - Pagamento simulado
                - Emissão de bilhete
                - PDF do bilhete
                - Envio do PDF pelo WhatsApp
                - Validação pública do bilhete

                Endpoint público do WhatsApp:
                /api/v1/public/whatsapp/webhook

                Endpoint público do PDF:
                /api/v1/public/tickets/{ticketId}/pdf
                """.trim();
    }

    private String menuAnswer(WhatsappSessionType sessionType) {
        if (WhatsappSessionType.USER.equals(sessionType)) {
            return """
                    Menu VaiRápido Operacional

                    Comandos disponíveis:
                    - Validar bilhete VRTK-...
                    - Resumo de hoje
                    - Ajuda
                    """.trim();
        }

        return """
                Menu VaiRápido

                Comandos disponíveis:
                - Comprar passagem
                - Consultar horários
                - Formas de pagamento
                - Documentos aceitos
                - Emitir bilhete
                - Falar com suporte

                Países suportados:
                🇦🇴 Angola
                🇧🇷 Brasil
                """.trim();
    }

    private boolean isGreeting(String normalizedMessage) {
        return normalizedMessage.equals("oi")
                || normalizedMessage.equals("ola")
                || normalizedMessage.equals("olá")
                || normalizedMessage.equals("bom dia")
                || normalizedMessage.equals("boa tarde")
                || normalizedMessage.equals("boa noite")
                || normalizedMessage.equals("hello")
                || normalizedMessage.equals("hi");
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(normalizeText(word))) {
                return true;
            }
        }

        return false;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}

