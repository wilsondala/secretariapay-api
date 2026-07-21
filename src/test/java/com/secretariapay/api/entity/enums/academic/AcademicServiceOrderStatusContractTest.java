package com.secretariapay.api.entity.enums.academic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicServiceOrderStatusContractTest {

    @Test
    void devePreservarASequenciaInstitucionalObrigatoria() {
        assertThat(AcademicServiceOrderStatus.values()).containsExactly(
                AcademicServiceOrderStatus.SOLICITADO,
                AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO,
                AcademicServiceOrderStatus.PAGO,
                AcademicServiceOrderStatus.DOCUMENTO_GERADO,
                AcademicServiceOrderStatus.PRONTO_PARA_IMPRESSAO,
                AcademicServiceOrderStatus.IMPRESSO,
                AcademicServiceOrderStatus.AGUARDANDO_ASSINATURA,
                AcademicServiceOrderStatus.ASSINADO,
                AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO,
                AcademicServiceOrderStatus.WHATSAPP_ENVIADO,
                AcademicServiceOrderStatus.ENTREGUE
        );
    }

    @Test
    void arquivoDocumentalDeveComecarDepoisDaAssinatura() {
        assertThat(AcademicServiceOrderStatus.SOLICITADO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.PAGO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.DOCUMENTO_GERADO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.PRONTO_PARA_IMPRESSAO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.IMPRESSO.isArchived()).isFalse();
        assertThat(AcademicServiceOrderStatus.AGUARDANDO_ASSINATURA.isArchived()).isFalse();

        assertThat(AcademicServiceOrderStatus.ASSINADO.isArchived()).isTrue();
        assertThat(AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO.isArchived()).isTrue();
        assertThat(AcademicServiceOrderStatus.WHATSAPP_ENVIADO.isArchived()).isTrue();
        assertThat(AcademicServiceOrderStatus.ENTREGUE.isArchived()).isTrue();
    }
}
