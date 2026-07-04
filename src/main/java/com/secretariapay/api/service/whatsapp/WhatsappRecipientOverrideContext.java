package com.secretariapay.api.service.whatsapp;

import java.util.Optional;

public final class WhatsappRecipientOverrideContext {

    private static final ThreadLocal<String> CURRENT_RECIPIENT = new ThreadLocal<>();

    private WhatsappRecipientOverrideContext() {
    }

    public static void set(String phone) {
        if (phone == null || phone.isBlank()) {
            CURRENT_RECIPIENT.remove();
            return;
        }

        CURRENT_RECIPIENT.set(phone.trim());
    }

    public static Optional<String> current() {
        String phone = CURRENT_RECIPIENT.get();

        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(phone.trim());
    }

    public static void clear() {
        CURRENT_RECIPIENT.remove();
    }
}
