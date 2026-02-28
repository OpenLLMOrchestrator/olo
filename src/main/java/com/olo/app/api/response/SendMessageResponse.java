package com.olo.app.api.response;

public class SendMessageResponse {

    private String messageId;
    private String runId;

    public SendMessageResponse() {}
    public SendMessageResponse(String messageId, String runId) {
        this.messageId = messageId;
        this.runId = runId;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
}
