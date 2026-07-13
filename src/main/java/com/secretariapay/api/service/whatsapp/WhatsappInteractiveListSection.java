package com.secretariapay.api.service.whatsapp;

import java.util.List;

public record WhatsappInteractiveListSection(
        String title,
        List<WhatsappInteractiveListRow> rows
) {
}
