package com.secretariapay.api.controller.financial;

import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeControllerPermissionsTest {

    @Test
    void leituraDeCobrancasDeveAtenderDcrEAuditoriaSemAlterarPermissoesDeEscrita() throws Exception {
        String[] readExpressions = {
                authorization("findAll"),
                authorization("findById", UUID.class),
                authorization("findByCode", String.class),
                authorization("findByStudent", UUID.class),
                authorization("findByStatus", ChargeStatus.class)
        };

        for (String expression : readExpressions) {
            assertThat(expression)
                    .contains(
                            "DCR_COORDENACAO",
                            "DCR_OPERADOR",
                            "AUDITORIA",
                            "OPERADOR_ATENDIMENTO"
                    );
        }

        assertThat(authorization("cancel", UUID.class))
                .doesNotContain("DCR_COORDENACAO", "DCR_OPERADOR", "AUDITORIA", "OPERADOR_ATENDIMENTO");
    }

    private String authorization(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = ChargeController.class.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("O método %s deve possuir @PreAuthorize", methodName)
                .isNotNull();
        return annotation.value();
    }
}
