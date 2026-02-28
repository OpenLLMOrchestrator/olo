package com.olo.app.store;

import com.olo.app.domain.OloExecutionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for execution events per run. Demo only; replace with DB for production.
 */
public class ExecutionEventStore {

    private final Map<String, List<OloExecutionEvent>> eventsByRunId = new ConcurrentHashMap<>();

    public void append(String runId, OloExecutionEvent event) {
        event.setRunId(runId);
        eventsByRunId.computeIfAbsent(runId, k -> new ArrayList<>()).add(event);
    }

    public List<OloExecutionEvent> getEvents(String runId) {
        List<OloExecutionEvent> list = eventsByRunId.get(runId);
        return list != null ? new ArrayList<>(list) : List.of();
    }
}
