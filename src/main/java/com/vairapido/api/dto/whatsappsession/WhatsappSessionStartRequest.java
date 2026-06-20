package com.vairapido.api.dto.whatsappsession;

import com.vairapido.api.entity.enums.WhatsappSessionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WhatsappSessionStartRequest {

    @NotBlank(message = "Número do WhatsApp é obrigatório.")
    private String phoneNumber;

    @NotNull(message = "Tipo da sessão é obrigatório.")
    private WhatsappSessionType sessionType;

    private String messageText;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public WhatsappSessionStartRequest setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public WhatsappSessionType getSessionType() {
        return sessionType;
    }

    public WhatsappSessionStartRequest setSessionType(WhatsappSessionType sessionType) {
        this.sessionType = sessionType;
        return this;
    }

    public String getMessageText() {
        return messageText;
    }

    public WhatsappSessionStartRequest setMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }
}