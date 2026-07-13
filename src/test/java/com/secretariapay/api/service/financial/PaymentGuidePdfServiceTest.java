package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGuidePdfServiceTest {

    @Test
    void deveGerarDocumentoFinanceiroSemTermosPromocionaisEComReferenciaBai() throws Exception {
        ChargeRepository chargeRepository = mock(ChargeRepository.class);
        Charge charge = new Charge()
                .setChargeCode("PROPINA-202607-ABC123")
                .setDescription("Propina referente a Julho/2026")
                .setReferenceMonth("Julho/2026")
                .setDueDate(LocalDate.of(2026, 7, 31))
                .setAmount(new BigDecimal("45000.00"))
                .setCurrency("AOA");

        when(chargeRepository.findByChargeCode(charge.getChargeCode())).thenReturn(Optional.of(charge));

        PaymentGuidePdfService service = new PaymentGuidePdfService(
                chargeRepository,
                "Banco Angolano de Investimento",
                "OMNEN INTELENGENDA",
                "AO06 0040 0000 6014 4677 1017 1",
                "06014467710001",
                "Multicaixa Express / transferência bancária para a conta AKZ indicada",
                "Unitel Money/Afrimoney quando autorizado pela instituição",
                true,
                "",
                "+244 923 168 085",
                "www.imetroangola.com",
                "secretaria.financeira@imetroangola.com"
        );

        byte[] pdf = service.generateByChargeCode(charge.getChargeCode());

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).doesNotContainIgnoringCase("oficial");
            assertThat(text).contains("Instituto Superior Politécnico Metropolitano de Angola");
            assertThat(text).contains("Secretaria Financeira | Gestão Académica e Financeira");
            assertThat(text).contains("Documento emitido eletronicamente pelo SecretáriaPay");
            assertThat(text).contains("GUIA DE PAGAMENTO ACADÉMICO");
            assertThat(text).contains("DADOS PARA PAGAMENTO");
            assertThat(text).contains("Banco Angolano de Investimento");
            assertThat(text).contains("OMNEN INTELENGENDA");
            assertThat(text).contains("SPAY-BAI-");
            assertThat(text).contains("Documento financeiro");
        }
    }
}
