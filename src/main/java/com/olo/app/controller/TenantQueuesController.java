package com.olo.app.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.app.service.KernelConfigQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Kernel config queues for a tenant from Redis keys <tenantId>:olo:kernel:config:*
 */
@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenant queues", description = "Kernel config queue list from Redis (<tenantId>:olo:kernel:config:*)")
public class TenantQueuesController {

    private static final Logger log = LoggerFactory.getLogger(TenantQueuesController.class);

    @Autowired(required = false)
    private KernelConfigQueueService queueService;

    @Operation(summary = "List queues", description = "Returns queue names for keys matching <tenantId>:olo:kernel:config:* in Redis. Shown under Chat and RAG.")
    @GetMapping("/{tenantId}/queues")
    public ResponseEntity<List<String>> listQueues(@PathVariable String tenantId) {
        List<String> queues = queueService != null ? queueService.getQueueNames(tenantId) : Collections.emptyList();
        if (queues.isEmpty()) {
            log.debug("/api/tenants/{}/queues: no queues (Redis not available or no keys for this tenant)", tenantId);
        }
        return ResponseEntity.ok(queues);
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Get queue config", description = "Returns queue configuration from Redis key <tenantId>:olo:kernel:config:<queueName>. Config may contain 'pipelines' array for the Conversation pipeline dropdown.")
    @GetMapping("/{tenantId}/queues/{queueName}/config")
    public ResponseEntity<Map<String, Object>> getQueueConfig(
            @PathVariable String tenantId,
            @PathVariable String queueName) {
        if (queueService == null) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
        String raw = queueService.getQueueConfig(tenantId, queueName);
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
        try {
            Map<String, Object> config = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return ResponseEntity.ok(config != null ? config : Collections.emptyMap());
        } catch (Exception e) {
            log.debug("Failed to parse queue config JSON for tenantId={} queueName={}", tenantId, queueName, e);
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }
}
