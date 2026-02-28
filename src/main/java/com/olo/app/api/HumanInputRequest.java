package com.olo.app.api;

/**
 * Request body for POST /api/runs/{runId}/human-input (user approval or text).
 */
public class HumanInputRequest {

    private boolean approved = true;
    private String message;  // optional text response

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
