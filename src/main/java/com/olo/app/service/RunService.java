package com.olo.app.service;

import com.olo.app.domain.EventType;
import com.olo.app.store.ChatRunStore;
import com.olo.app.store.ExecutionEventStore;
import com.olo.app.store.RunEventBroadcaster;
import com.olo.input.model.WorkflowInput;

import java.util.Map;

/**
 * Service for run lifecycle: start workflow, signal human input, append and broadcast events.
 */
public interface RunService {

    void startWorkflow(String runId, WorkflowInput workflowInput, String taskQueueFromFrontend);

    void signalHumanInput(String runId, boolean approved, String message);

    /** Idempotency key: (runId, sequenceNumber). eventType/correlationId optional; correlationId falls back to run's. */
    void appendEvent(String runId, String nodeId, String parentNodeId,
                    String nodeType, String status,
                    Map<String, Object> input, Map<String, Object> output, Map<String, Object> metadata,
                    Long sequenceNumber, Integer eventVersion, EventType eventType, String correlationId);

    RunEventBroadcaster getBroadcaster();

    ExecutionEventStore getEventStore();

    ChatRunStore getRunStore();

    /** Current assistant response for the run from event store (last MODEL or SYSTEM COMPLETED with output). */
    String getRunResponse(String runId);
}
