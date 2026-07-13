package com.secretariapay.api.service.whatsapp;

import java.util.List;

public record WhatsappInteractiveListMessage(
        String header,
        String body,
        String footer,
        String buttonLabel,
        List<WhatsappInteractiveListSection> sections,
        String fallbackText
) {
}
