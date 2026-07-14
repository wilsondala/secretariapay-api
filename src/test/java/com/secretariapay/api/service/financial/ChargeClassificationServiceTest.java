package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.financial.Charge;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeClassificationServiceTest {

    private final ChargeClassificationService service = new ChargeClassificationService();

    @Test
    void deveClassificarPropinaSeparadamente() {
        Charge charge = new Charge()
                .setChargeCode("IMT-PROPINA-2026-09-202301404")
                .setDescription("Propina Setembro/2026")
                .setReferenceMonth("Setembro/2026");

        assertThat(service.resolveCategory(charge)).isEqualTo(ChargeCategory.TUITION);
        assertThat(service.resolveServiceCode(charge)).isEqualTo("TUITION");
    }

    @Test
    void deveClassificarMatriculaComoServicoAcademico() {
        Charge charge = new Charge()
                .setChargeCode("IMT-SERVICO-ENROLLMENT-202301404")
                .setDescription("Matrícula")
                .setReferenceMonth("Matrícula 2026");

        assertThat(service.resolveCategory(charge)).isEqualTo(ChargeCategory.ACADEMIC_SERVICE);
        assertThat(service.resolveServiceCode(charge)).isEqualTo("ENROLLMENT");
    }

    @Test
    void deveClassificarDeclaracaoCertificadoERecurso() {
        assertService("Declaração sem Notas", "DECLARATION_WITHOUT_GRADES");
        assertService("Certificado", "CERTIFICATE");
        assertService("Exame de Recurso", "RESIT_EXAM");
    }

    @Test
    void deveRespeitarCategoriaPersistida() {
        Charge charge = new Charge()
                .setChargeCategory(ChargeCategory.ACADEMIC_SERVICE)
                .setServiceCode("DIPLOMA")
                .setDescription("Documento académico");

        assertThat(service.resolveCategory(charge)).isEqualTo(ChargeCategory.ACADEMIC_SERVICE);
        assertThat(service.resolveServiceCode(charge)).isEqualTo("DIPLOMA");
    }

    private void assertService(String description, String expectedCode) {
        Charge charge = new Charge().setDescription(description);
        assertThat(service.resolveCategory(charge)).isEqualTo(ChargeCategory.ACADEMIC_SERVICE);
        assertThat(service.resolveServiceCode(charge)).isEqualTo(expectedCode);
    }
}
