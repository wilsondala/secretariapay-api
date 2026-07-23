package com.secretariapay.api.controller.admission;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionEnrollmentDocumentFileControllerSecurityTest {

    @Test
    void shouldKeepSensitiveDocumentReadingRestrictedToOperationalProfiles() throws Exception {
        assertRestrictedReadPolicy(
                AdmissionEnrollmentDocumentFileController.class.getMethod("list", UUID.class)
        );
        assertRestrictedReadPolicy(
                AdmissionEnrollmentDocumentFileController.class.getMethod("content", UUID.class)
        );
    }

    private void assertRestrictedReadPolicy(Method method) {
        PreAuthorize policy = method.getAnnotation(PreAuthorize.class);

        assertNotNull(policy);
        assertFalse(policy.value().contains("MARKETING"));
        assertTrue(policy.value().contains("ADMISSOES"));
        assertTrue(policy.value().contains("SECRETARIA"));
        assertTrue(policy.value().contains("DCR_COORDENACAO"));
        assertTrue(policy.value().contains("AUDITORIA"));
    }
}
