package com.secretariapay.api.service.whatsapp;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappInteractiveMenuFactoryTest {

    private final WhatsappInteractiveMenuFactory factory = new WhatsappInteractiveMenuFactory();

    @Test
    void deveConverterMenuPrincipalEmListaInterativa() {
        String reply = """
                Secretaria Pay (IMETRO): Bom dia 👋

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
                """.trim();

        Optional<WhatsappInteractiveListMessage> result = factory.fromReplyText(reply);

        assertThat(result).isPresent();
        WhatsappInteractiveListMessage menu = result.orElseThrow();
        assertThat(menu.header()).isEqualTo("SecretáriaPay — IMETRO");
        assertThat(menu.buttonLabel()).isEqualTo("Ver opções");
        assertThat(menu.sections()).hasSize(1);
        assertThat(menu.sections().getFirst().rows()).hasSize(7);
        assertThat(menu.sections().getFirst().rows().getFirst().id()).isEqualTo("1");
        assertThat(menu.sections().getFirst().rows().getFirst().title()).isEqualTo("Propinas");
        assertThat(menu.sections().getFirst().rows().getFirst().description()).isEqualTo("Consultar e pagar propinas");
        assertThat(menu.fallbackText()).isEqualTo(reply);
    }

    @Test
    void deveConverterFormasDePagamentoComExemplos() {
        String reply = """
                📄 Guia preparada.

                Estudante: Wilson dos Santos Kahango Dala
                Referência: Novembro/2026
                Total a pagar: 5.000,00 Kz

                Escolha a forma de pagamento:

                [1] Multicaixa Express - AppyPay GPO
                [2] Pagamento por Referência - AppyPay REF
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [8] Teste real Brasil - InfinitePay
                [5] Voltar
                """.trim();

        WhatsappInteractiveListMessage menu = factory.fromReplyText(reply).orElseThrow();

        assertThat(menu.header()).isEqualTo("Forma de pagamento");
        assertThat(menu.buttonLabel()).isEqualTo("Ver pagamentos");
        assertThat(menu.sections().getFirst().rows()).hasSize(6);
        assertThat(menu.sections().getFirst().rows())
                .anySatisfy(row -> {
                    assertThat(row.id()).isEqualTo("3");
                    assertThat(row.title()).isEqualTo("Transferência mesmo");
                    assertThat(row.description()).contains("BAI");
                });
        assertThat(menu.body()).doesNotContain("[1]", "[2]", "[3]");
    }

    @Test
    void deveManterTextoQuandoExistiremMaisDeDezOpcoes() {
        StringBuilder reply = new StringBuilder("Escolha uma opção:\n\n");
        IntStream.rangeClosed(1, 11).forEach(index -> reply.append("[").append(index).append("] Opção ").append(index).append("\n"));

        assertThat(factory.fromReplyText(reply.toString())).isEmpty();
    }

    @Test
    void naoDeveConverterMensagemSemOpcoes() {
        assertThat(factory.fromReplyText("Pagamento confirmado com sucesso.")).isEmpty();
    }
}
