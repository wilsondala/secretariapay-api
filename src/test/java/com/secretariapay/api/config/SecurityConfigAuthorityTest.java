package com.secretariapay.api.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigAuthorityTest {

    @Test
    void direcaoMustNotReceiveGlobalAdminAuthority() throws Exception {
        List<String> authorities = readAuthorities("ADMIN_AUTHORITIES");

        assertFalse(authorities.contains("DIRECAO"));
        assertFalse(authorities.contains("ROLE_DIRECAO"));
    }

    @Test
    void direcaoMayAccessOnlyTheDedicatedAdminUserMatcher() throws Exception {
        List<String> authorities = readAuthorities("ADMIN_USER_AUTHORITIES");

        assertTrue(authorities.contains("DIRECAO"));
        assertTrue(authorities.contains("ROLE_DIRECAO"));
    }

    private List<String> readAuthorities(String fieldName) throws Exception {
        Field field = SecurityConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return Arrays.asList((String[]) field.get(null));
    }
}
