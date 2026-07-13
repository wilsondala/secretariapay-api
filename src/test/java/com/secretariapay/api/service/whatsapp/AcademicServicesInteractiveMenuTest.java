package com.secretariapay.api.service.whatsapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicServicesInteractiveMenuTest {

    private final WhatsappInteractiveMenuFactory factory = new WhatsappInteractiveMenuFactory();

    @Test
    void deveConverterTabelaDePrecosEmCardInterativo() {
        String reply = """
                📚 Serviços académicos disponíveis.

                Escolha o serviço que deseja solicitar:

                [1] Matrícula — 30.800,00 Kz
                [2] Confirmação de matrícula — 28.000,00 Kz
                [3] Inscrição — 5.000,00 Kz
                [4] Recurso — 7.800,00 Kz
                [5] Exame especial — 17.000,00 Kz
                [6] Declaração com nota — 8.625,00 Kz
                [7] Declaração sem nota — 4.400,00 Kz
                [8] Certificado — 39.000,00 Kz
                [9] Diploma — 67.000,00 Kz
                [10] Voltar ao menu principal

                Os valores são consultados diretamente na tabela institucional do IMETRO.
                """.trim();

        WhatsappInteractiveListMessage menu = factory.fromReplyText(reply).orElseThrow();

        assertThat(menu.header()).isEqualTo("Serviços académicos");
        assertThat(menu.buttonLabel()).isEqualTo("Escolher serviço");
        assertThat(menu.sections()).hasSize(1);
        assertThat(menu.sections().getFirst().rows()).hasSize(10);
        assertThat(menu.sections().getFirst().rows().getFirst().title()).isEqualTo("Matrícula");
        assertThat(menu.sections().getFirst().rows().getFirst().description()).isEqualTo("30.800,00 Kz");
        assertThat(menu.sections().getFirst().rows().get(8).title()).isEqualTo("Diploma");
        assertThat(menu.sections().getFirst().rows().get(8).description()).isEqualTo("67.000,00 Kz");
    }
}
