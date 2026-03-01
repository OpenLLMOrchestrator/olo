package com.olo.app.api.request;

import jakarta.validation.constraints.NotBlank;

public class CreateSessionRequest {

    @NotBlank
    private String tenantId;

    private String queueName;
    private String pipelineId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getPipelineId() { return pipelineId; }
    public void setPipelineId(String pipelineId) { this.pipelineId = pipelineId; }
}
