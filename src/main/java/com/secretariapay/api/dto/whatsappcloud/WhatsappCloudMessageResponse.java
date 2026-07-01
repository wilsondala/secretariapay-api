package com.secretariapay.api.dto.whatsappcloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappCloudMessageResponse {

    @JsonProperty("messaging_product")
    private String messagingProduct;

    private List<ContactResponse> contacts;

    private List<MessageResponse> messages;

    public String getMessagingProduct() {
        return messagingProduct;
    }

    public WhatsappCloudMessageResponse setMessagingProduct(String messagingProduct) {
        this.messagingProduct = messagingProduct;
        return this;
    }

    public List<ContactResponse> getContacts() {
        return contacts;
    }

    public WhatsappCloudMessageResponse setContacts(List<ContactResponse> contacts) {
        this.contacts = contacts;
        return this;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public WhatsappCloudMessageResponse setMessages(List<MessageResponse> messages) {
        this.messages = messages;
        return this;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactResponse {

        private String input;

        @JsonProperty("wa_id")
        private String waId;

        public String getInput() {
            return input;
        }

        public ContactResponse setInput(String input) {
            this.input = input;
            return this;
        }

        public String getWaId() {
            return waId;
        }

        public ContactResponse setWaId(String waId) {
            this.waId = waId;
            return this;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageResponse {

        private String id;

        @JsonProperty("message_status")
        private String messageStatus;

        public String getId() {
            return id;
        }

        public MessageResponse setId(String id) {
            this.id = id;
            return this;
        }

        public String getMessageStatus() {
            return messageStatus;
        }

        public MessageResponse setMessageStatus(String messageStatus) {
            this.messageStatus = messageStatus;
            return this;
        }
    }
}
