package com.secretariapay.api.dto.whatsappcloud;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WhatsappCloudDocumentMessageRequest {

    @JsonProperty("messaging_product")
    private String messagingProduct = "whatsapp";

    private String to;

    private String type = "document";

    private DocumentPayload document;

    public static WhatsappCloudDocumentMessageRequest documentMessage(
            String to,
            String documentUrl,
            String fileName,
            String caption
    ) {
        return new WhatsappCloudDocumentMessageRequest()
                .setMessagingProduct("whatsapp")
                .setTo(to)
                .setType("document")
                .setDocument(
                        new DocumentPayload()
                                .setLink(documentUrl)
                                .setFilename(fileName)
                                .setCaption(caption)
                );
    }

    public String getMessagingProduct() {
        return messagingProduct;
    }

    public WhatsappCloudDocumentMessageRequest setMessagingProduct(String messagingProduct) {
        this.messagingProduct = messagingProduct;
        return this;
    }

    public String getTo() {
        return to;
    }

    public WhatsappCloudDocumentMessageRequest setTo(String to) {
        this.to = to;
        return this;
    }

    public String getType() {
        return type;
    }

    public WhatsappCloudDocumentMessageRequest setType(String type) {
        this.type = type;
        return this;
    }

    public DocumentPayload getDocument() {
        return document;
    }

    public WhatsappCloudDocumentMessageRequest setDocument(DocumentPayload document) {
        this.document = document;
        return this;
    }

    public static class DocumentPayload {

        private String link;

        private String caption;

        private String filename;

        public String getLink() {
            return link;
        }

        public DocumentPayload setLink(String link) {
            this.link = link;
            return this;
        }

        public String getCaption() {
            return caption;
        }

        public DocumentPayload setCaption(String caption) {
            this.caption = caption;
            return this;
        }

        public String getFilename() {
            return filename;
        }

        public DocumentPayload setFilename(String filename) {
            this.filename = filename;
            return this;
        }
    }
}
