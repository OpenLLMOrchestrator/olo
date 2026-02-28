package com.olo.app.store;

import com.olo.app.execution.OloExecutionEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts execution events to SSE subscribers for a run. When an event is appended,
 * all subscribers for that runId receive it.
 */
public class RunEventBroadcaster {

    private final ExecutionEventStore eventStore;

    /** runId -> list of active emitters */
    private final java.util.Map<String, List<SseEmitter>> emittersByRunId = new java.util.concurrent.ConcurrentHashMap<>();

    public RunEventBroadcaster(ExecutionEventStore eventStore) {
        this.eventStore = eventStore;
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
                emitter.send(SseEmitter.event().data(event));
            } catch (IOException e) {
                return;
            }
        }
        emittersByRunId.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
    }

    public void broadcast(String runId, OloExecutionEvent event) {
        List<SseEmitter> emitters = emittersByRunId.get(runId);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(event));
            } catch (IOException e) {
                emitter.completeWithError(e);
                removeEmitter(runId, emitter);
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
