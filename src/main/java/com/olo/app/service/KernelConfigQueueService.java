package com.olo.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads kernel config queue names from Redis.
 * Key pattern: <tenantId>:olo:kernel:config:<queueName>
 * Requires Redis: do not exclude Redis autoconfig; ensure Redis is running on spring.data.redis.host/port.
 */
@Service
public class KernelConfigQueueService {

    private static final Logger log = LoggerFactory.getLogger(KernelConfigQueueService.class);
    private static final String SUFFIX = ":olo:kernel:config:";
    private static final String SCAN_PATTERN = "*" + SUFFIX + "*";

    private final StringRedisTemplate redisTemplate;

    public KernelConfigQueueService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.warn("KernelConfigQueueService: Redis not available (StringRedisTemplate is null). Ensure Redis is running and spring.autoconfigure.exclude does NOT contain RedisAutoConfiguration.");
        }
    }

    /**
     * Returns queue names for keys matching <tenantId>:olo:kernel:config:<queueName>.
     */
    public List<String> getQueueNames(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Collections.emptyList();
        }
        if (redisTemplate == null) {
            log.debug("getQueueNames: Redis not available, returning empty list for tenantId={}", tenantId);
            return Collections.emptyList();
        }
        Set<String> queueNames = new LinkedHashSet<>();
        String tenantPrefix = tenantId + SUFFIX;
        try {
            Set<String> keys = keysViaExecute(SCAN_PATTERN);
            if (log.isDebugEnabled()) {
                log.debug("getQueueNames tenantId={} keysFound={} sample={}", tenantId, keys.size(), keys.isEmpty() ? null : keys.iterator().next());
            }
            for (String k : keys) {
                if (k != null && k.startsWith(tenantPrefix)) {
                    String name = k.substring(tenantPrefix.length());
                    if (!name.isEmpty()) queueNames.add(name);
                }
            }
            if (queueNames.isEmpty() && "default".equalsIgnoreCase(tenantId.trim())) {
                String legacyPrefix = "olo:kernel:config:";
                for (String k : keys) {
                    if (k != null && k.startsWith(legacyPrefix)) {
                        String name = k.substring(legacyPrefix.length());
                        if (!name.isEmpty()) queueNames.add(name);
                    }
                }
            }
            return new ArrayList<>(queueNames).stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Run KEYS pattern using the template's connection and key serializer so encoding/DB match.
     */
    @SuppressWarnings("unchecked")
    private Set<String> keysViaExecute(String pattern) {
        if (redisTemplate == null) return Collections.emptySet();
        byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
        Set<String> result = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Iterable<byte[]> rawKeys = connection.keys(patternBytes);
            if (rawKeys == null) {
                log.debug("keysViaExecute: Redis KEYS pattern='{}' returned null", pattern);
                return Collections.emptySet();
            }
            Set<String> out = new LinkedHashSet<>();
            for (byte[] keyBytes : rawKeys) {
                if (keyBytes != null && keyBytes.length > 0) {
                    out.add(new String(keyBytes, StandardCharsets.UTF_8));
                }
            }
            if (out.isEmpty()) {
                log.debug("keysViaExecute: Redis KEYS pattern='{}' returned 0 keys (check DB index and that keys exist)", pattern);
            }
            return out;
        });
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Returns the queue configuration JSON for key <tenantId>:olo:kernel:config:<queueName>.
     * Value is expected to be JSON (e.g. contains "pipelines" array). Returns null if key missing or Redis unavailable.
     */
    public String getQueueConfig(String tenantId, String queueName) {
        if (redisTemplate == null || tenantId == null || tenantId.isBlank() || queueName == null || queueName.isBlank()) {
            return null;
        }
        try {
            String key = tenantId + SUFFIX + queueName;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("getQueueConfig failed for tenantId={} queueName={}", tenantId, queueName, e);
            return null;
        }
    }

    /**
     * Returns tenant ids that have at least one key matching <tenantId>:olo:kernel:config:*
     */
    public List<String> getTenantIdsFromRedis() {
        if (redisTemplate == null) return List.of();
        try {
            Set<String> keys = keysViaExecute(SCAN_PATTERN);
            Set<String> ids = new LinkedHashSet<>();
            for (String k : keys) {
                if (k == null) continue;
                int idx = k.indexOf(SUFFIX);
                if (idx > 0) {
                    String id = k.substring(0, idx);
                    if (!id.isEmpty()) ids.add(id);
                }
            }
            return new ArrayList<>(ids).stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
