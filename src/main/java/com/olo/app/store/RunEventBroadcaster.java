package com.olo.app.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olo.app.api.ExecutionEventSanitizer;
import com.olo.app.domain.OloExecutionEvent;
import com.olo.app.ws.RunEventWebSocketRegistry;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts execution events to SSE and WebSocket subscribers for a run. When an event is appended,
 * all subscribers for that runId receive it. Events are sanitized before send (product-level fields only).
 */
public class RunEventBroadcaster {

    private final ExecutionEventStore eventStore;
    private final RunEventWebSocketRegistry wsRegistry;
    private final ObjectMapper objectMapper;

    /** runId -> list of active SSE emitters */
    private final java.util.Map<String, List<SseEmitter>> emittersByRunId = new java.util.concurrent.ConcurrentHashMap<>();

    public RunEventBroadcaster(ExecutionEventStore eventStore) {
        this(eventStore, null, null);
    }

    public RunEventBroadcaster(ExecutionEventStore eventStore,
                               RunEventWebSocketRegistry wsRegistry,
                               ObjectMapper objectMapper) {
        this.eventStore = eventStore;
        this.wsRegistry = wsRegistry;
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByRunId.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
        return emitter;
    }

    /** Send all existing events for runId to the given emitter (catch-up), then keep emitter for live. */
    public void subscribeWithCatchUp(String runId, SseEmitter emitter) {
        for (OloExecutionEvent event : eventStore.getEvents(runId)) {
            try {
                emitter.send(SseEmitter.event().data(ExecutionEventSanitizer.forApi(event)));
            } catch (IOException e) {
                return;
            }
        }
        emittersByRunId.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
    }

    public void broadcast(String runId, OloExecutionEvent event) {
        OloExecutionEvent sanitized = ExecutionEventSanitizer.forApi(event);
        List<SseEmitter> emitters = emittersByRunId.get(runId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().data(sanitized));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    removeEmitter(runId, emitter);
                }
            }
        }
        if (wsRegistry != null && objectMapper != null) {
            try {
                String envelope = objectMapper.writeValueAsString(Map.of("type", "RUN_EVENT", "payload", sanitized));
                wsRegistry.sendToRun(runId, envelope);
            } catch (JsonProcessingException e) {
                // log and skip WS push
            }
        }
    }

    private void removeEmitter(String runId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByRunId.get(runId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emittersByRunId.remove(runId);
        }
    }
}
