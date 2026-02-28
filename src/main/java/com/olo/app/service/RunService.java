package com.olo.app.service;

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

    void appendEvent(String runId, String nodeId, String parentNodeId,
                    String nodeType, String status,
                    Map<String, Object> input, Map<String, Object> output, Map<String, Object> metadata);

    RunEventBroadcaster getBroadcaster();

    ExecutionEventStore getEventStore();

    ChatRunStore getRunStore();
}
