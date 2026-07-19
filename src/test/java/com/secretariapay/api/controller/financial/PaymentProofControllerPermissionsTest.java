package com.secretariapay.api.controller.financial;

import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProofControllerPermissionsTest {

    @Test
    void dcrDeveConsultarComprovativosEAnexos() throws Exception {
        String[] expressions = {
                authorization("findAll"),
                authorization("findById", UUID.class),
                authorization("openAttachment", UUID.class),
                authorization("findByStatus", PaymentProofStatus.class),
                authorization("findByCharge", UUID.class)
        };

        for (String expression : expressions) {
            assertThat(expression)
                    .contains("DCR_COORDENACAO", "DCR_OPERADOR");
        }
    }

    @Test
    void dcrDeveAprovarERejeitarComprovativos() throws Exception {
        String approve = authorization("approve", UUID.class, PaymentProofReviewRequest.class);
        String reject = authorization("reject", UUID.class, PaymentProofReviewRequest.class);

        assertThat(approve).contains("DCR_COORDENACAO", "DCR_OPERADOR");
        assertThat(reject).contains("DCR_COORDENACAO", "DCR_OPERADOR");
    }

    private String authorization(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = PaymentProofController.class.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("O método %s deve possuir @PreAuthorize", methodName)
                .isNotNull();
        return annotation.value();
    }
}
