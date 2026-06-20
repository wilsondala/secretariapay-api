package com.vairapido.api.dto.whatsappcommand;

public class WhatsappCommandResult {

    private Boolean processed;
    private Boolean allowed;
    private String commandName;
    private String replyMessage;

    public Boolean getProcessed() {
        return processed;
    }

    public WhatsappCommandResult setProcessed(Boolean processed) {
        this.processed = processed;
        return this;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public WhatsappCommandResult setAllowed(Boolean allowed) {
        this.allowed = allowed;
        return this;
    }

    public String getCommandName() {
        return commandName;
    }

    public WhatsappCommandResult setCommandName(String commandName) {
        this.commandName = commandName;
        return this;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public WhatsappCommandResult setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
        return this;
    }
}