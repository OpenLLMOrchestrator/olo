package com.olo.app.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

import java.util.Map;

/**
 * Reads Authorization: Bearer &lt;JWT&gt;, decodes payload (Base64), returns tenantId from
 * "tenantId" or "sub" claim. No signature verification (demo only; production must verify JWT).
 */
public class DefaultJwtTenantExtractor implements JwtTenantExtractor {

    private final ObjectMapper objectMapper;

    public DefaultJwtTenantExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String extractTenantId(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7).trim();
        if (token.isEmpty()) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            if (payload == null) return null;
            Object tenantId = payload.get("tenantId");
            if (tenantId != null && !tenantId.toString().isBlank()) return tenantId.toString();
            Object sub = payload.get("sub");
            if (sub != null && !sub.toString().isBlank()) return sub.toString();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
