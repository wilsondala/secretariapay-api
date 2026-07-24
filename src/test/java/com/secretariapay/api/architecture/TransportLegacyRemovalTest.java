package com.secretariapay.api.architecture;

import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransportLegacyRemovalTest {

    @Test
    void shouldExposeOnlyInstitutionalUserRoles() {
        Set<String> roles = Arrays.stream(UserRole.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertFalse(roles.contains("ADMIN"));
        assertFalse(roles.contains("COMPANY_ADMIN"));
        assertFalse(roles.contains("OPERATOR"));
    }

    @Test
    void shouldNotLoadTransportDomainClasses() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.secretariapay.api.entity.Booking"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.secretariapay.api.entity.Passenger"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.secretariapay.api.entity.TransportCompany"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.secretariapay.api.entity.Trip"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.secretariapay.api.entity.Ticket"));
    }

    @Test
    void shouldKeepWhatsappContextExclusivelyAcademic() {
        assertEquals(
                Set.of("SECRETARIAPAY_ACADEMICO"),
                Arrays.stream(WhatsappSessionType.values()).map(Enum::name).collect(Collectors.toSet())
        );
        assertEquals(
                Set.of(
                        "SECRETARIAPAY_START",
                        "SECRETARIAPAY_WAITING_IDENTIFIER",
                        "SECRETARIAPAY_STUDENT_FOUND",
                        "SECRETARIAPAY_CHARGE_FOUND",
                        "SECRETARIAPAY_WAITING_PAYMENT_PROOF",
                        "SECRETARIAPAY_WAITING_HUMAN_SUPPORT",
                        "SECRETARIAPAY_FINISHED"
                ),
                Arrays.stream(WhatsappConversationStep.values()).map(Enum::name).collect(Collectors.toSet())
        );
    }
}
