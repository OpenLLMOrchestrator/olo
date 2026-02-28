package com.olo.app.api.request;

import jakarta.validation.constraints.NotBlank;

public class CreateSessionRequest {

    @NotBlank
    private String tenantId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
