package com.secretariapay.api.controller;

import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicServiceOrderControllerPermissionsTest {

    @Test
    void leituraDeveAtenderDcrSecretariaDirecaoEAuditoria() throws Exception {
        String list = authorization("list", com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus.class, UUID.class);
        String archive = authorization("archive");
        String findById = authorization("findById", UUID.class);

        for (String expression : new String[]{list, archive, findById}) {
            assertThat(expression)
                    .contains("DCR_COORDENACAO", "DCR_OPERADOR", "SECRETARIA", "DIRECAO", "AUDITORIA");
        }
    }

    @Test
    void dcrDeveCriarPedidoEmitirCobrancaEConfirmarPagamentoSemExecutarEtapasDaSecretariaOuDirecao() throws Exception {
        String create = authorization("create", AcademicServiceOrderDto.CreateRequest.class);
        String requestPayment = authorization("requestPayment", UUID.class, AcademicServiceOrderDto.RequestPaymentRequest.class);
        String confirmPayment = authorization("confirmPayment", UUID.class);

        for (String expression : new String[]{create, requestPayment, confirmPayment}) {
            assertThat(expression)
                    .contains("DCR_COORDENACAO", "DCR_OPERADOR")
                    .doesNotContain("'SECRETARIA'", "'DIRECAO'");
        }
    }

    @Test
    void secretariaDeveProcessarDocumentoFisicoSemPoderAssinar() throws Exception {
        String[] secretariaMethods = {
                "generateDocument",
                "markReadyForPrint",
                "markPrinted",
                "submitForSignature",
                "sendPickupWhatsapp"
        };

        for (String methodName : secretariaMethods) {
            String expression = authorization(methodName, UUID.class);
            assertThat(expression)
                    .contains("SECRETARIA")
                    .doesNotContain("'DCR_OPERADOR'", "'DIRECAO'");
        }

        assertThat(authorization("markReadyForPickup", UUID.class, AcademicServiceOrderDto.ActionRequest.class))
                .contains("SECRETARIA")
                .doesNotContain("'DIRECAO'");
        assertThat(authorization("deliver", UUID.class, AcademicServiceOrderDto.ActionRequest.class))
                .contains("SECRETARIA")
                .doesNotContain("'DIRECAO'");
    }

    @Test
    void assinaturaDeveSerExclusivaDaDirecaoEAdministradores() throws Exception {
        assertThat(authorization("sign", UUID.class))
                .contains("DIRECAO")
                .doesNotContain("'SECRETARIA'", "'DCR_OPERADOR'", "'DCR_COORDENACAO'");
    }

    private String authorization(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AcademicServiceOrderController.class.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("O método %s deve possuir @PreAuthorize", methodName)
                .isNotNull();
        return annotation.value();
    }
}
