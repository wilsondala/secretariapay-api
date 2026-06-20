package com.vairapido.api.dto.whatsappcloud;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WhatsappCloudTextMessageRequest {

    @JsonProperty("messaging_product")
    private String messagingProduct = "whatsapp";

    private String to;

    private String type = "text";

    private TextPayload text;

    public static WhatsappCloudTextMessageRequest textMessage(
            String to,
            String body
    ) {
        return new WhatsappCloudTextMessageRequest()
                .setMessagingProduct("whatsapp")
                .setTo(to)
                .setType("text")
                .setText(
                        new TextPayload()
                                .setPreviewUrl(false)
                                .setBody(body)
                );
    }

    public String getMessagingProduct() {
        return messagingProduct;
    }

    public WhatsappCloudTextMessageRequest setMessagingProduct(String messagingProduct) {
        this.messagingProduct = messagingProduct;
        return this;
    }

    public String getTo() {
        return to;
    }

    public WhatsappCloudTextMessageRequest setTo(String to) {
        this.to = to;
        return this;
    }

    public String getType() {
        return type;
    }

    public WhatsappCloudTextMessageRequest setType(String type) {
        this.type = type;
        return this;
    }

    public TextPayload getText() {
        return text;
    }

    public WhatsappCloudTextMessageRequest setText(TextPayload text) {
        this.text = text;
        return this;
    }

    public static class TextPayload {

        @JsonProperty("preview_url")
        private Boolean previewUrl = false;

        private String body;

        public Boolean getPreviewUrl() {
            return previewUrl;
        }

        public TextPayload setPreviewUrl(Boolean previewUrl) {
            this.previewUrl = previewUrl;
            return this;
        }

        public String getBody() {
            return body;
        }

        public TextPayload setBody(String body) {
            this.body = body;
            return this;
        }
    }
}