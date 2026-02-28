package com.olo.app.api;

public class CreateSessionResponse {

    private String sessionId;

    public CreateSessionResponse() {}
    public CreateSessionResponse(String sessionId) { this.sessionId = sessionId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
