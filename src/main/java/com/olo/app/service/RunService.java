package com.olo.app.service;

import com.olo.app.execution.NodeStatus;
import com.olo.app.execution.NodeType;
import com.olo.app.execution.OloExecutionEvent;
import com.olo.app.store.*;
import com.olo.input.model.WorkflowInput;
import com.olo.sdk.TemporalClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RunService {

    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final ExecutionEventStore eventStore;
    private final RunEventBroadcaster broadcaster;
    private final ChatRunStore runStore;
    private final TemporalClient temporalClient;
    private final WorkflowClient workflowClient;
    private final String callbackBaseUrl;
    private final String taskQueue;

    public RunService(ExecutionEventStore eventStore,
                      RunEventBroadcaster broadcaster,
                      ChatRunStore runStore,
                      TemporalClient temporalClient,
                      @Qualifier("oloCallbackBaseUrl") String callbackBaseUrl,
                      @Qualifier("oloTaskQueue") String taskQueue) {
        this.eventStore = eventStore;
        this.broadcaster = broadcaster;
        this.runStore = runStore;
        this.temporalClient = temporalClient;
        this.workflowClient = temporalClient.getWorkflowClient();
        this.callbackBaseUrl = callbackBaseUrl;
        this.taskQueue = taskQueue;
    }

    /** Start the Olo chat workflow with WorkflowInput (JSON type). Temporal serializes it as a JSON object, not a string. */
    public void startWorkflow(String runId, WorkflowInput workflowInput, String taskQueueFromFrontend) {
        String effectiveTaskQueue = (taskQueueFromFrontend != null && !taskQueueFromFrontend.isBlank())
                ? taskQueueFromFrontend.trim()
                : taskQueue;
        log.info("Starting workflow runId={} taskQueue={} callbackBaseUrl={}", runId, effectiveTaskQueue, callbackBaseUrl);
        log.info("Workflow input payload (JSON): {}", workflowInput != null ? workflowInput.toJson() : "null");

        try {
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId("run-" + runId)
                    .setTaskQueue(effectiveTaskQueue)
                    .build();
            WorkflowStub stub = temporalClient.newChatWorkflowStub(options);
            stub.start(workflowInput);
            log.info("Workflow start requested successfully for runId={}", runId);
        } catch (Exception e) {
            log.error("Failed to start workflow for runId={}: {}", runId, e.getMessage(), e);
            throw e;
        }
    }

    /** Signal the workflow with human input (approval or message). */
    public void signalHumanInput(String runId, boolean approved, String message) {
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub("run-" + runId);
        stub.signal("humanInput", approved, message != null ? message : "");
    }

    /** Append event (from worker callback) and broadcast to SSE subscribers. */
    public void appendEvent(String runId, String nodeId, String parentNodeId,
                            String nodeType, String status,
                            Map<String, Object> input, Map<String, Object> output, Map<String, Object> metadata) {
        OloExecutionEvent event = new OloExecutionEvent();
        event.setRunId(runId);
        event.setNodeId(nodeId);
        event.setParentNodeId(parentNodeId);
        event.setNodeType(NodeType.valueOf(nodeType));
        event.setStatus(NodeStatus.valueOf(status));
        event.setTimestamp(System.currentTimeMillis());
        event.setInput(input);
        event.setOutput(output);
        event.setMetadata(metadata);
        eventStore.append(runId, event);
        broadcaster.broadcast(runId, event);
        if ("COMPLETED".equals(status) && "SYSTEM".equals(nodeType)) {
            runStore.setStatus(runId, "completed");
        } else if ("WAITING".equals(status) && "HUMAN".equals(nodeType)) {
            runStore.setStatus(runId, "waiting_human");
        }
    }

    public RunEventBroadcaster getBroadcaster() {
        return broadcaster;
    }

    public ExecutionEventStore getEventStore() {
        return eventStore;
    }

    public ChatRunStore getRunStore() {
        return runStore;
    }
}
