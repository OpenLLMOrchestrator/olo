package com.olo.app.service.impl;

import com.olo.app.domain.EventType;
import com.olo.app.domain.NodeStatus;
import com.olo.app.domain.NodeType;
import com.olo.app.domain.OloExecutionEvent;
import com.olo.app.service.RunService;
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

import java.util.List;
import java.util.Map;

@Service
public class RunServiceImpl implements RunService {

    private static final Logger log = LoggerFactory.getLogger(RunServiceImpl.class);

    private final ExecutionEventStore eventStore;
    private final RunEventBroadcaster broadcaster;
    private final ChatRunStore runStore;
    private final TemporalClient temporalClient;
    private final WorkflowClient workflowClient;
    private final String callbackBaseUrl;
    private final String taskQueue;

    public RunServiceImpl(ExecutionEventStore eventStore,
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

    @Override
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

    @Override
    public void signalHumanInput(String runId, boolean approved, String message) {
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub("run-" + runId);
        stub.signal("humanInput", approved, message != null ? message : "");
    }

    @Override
    public void appendEvent(String runId, String nodeId, String parentNodeId,
                            String nodeType, String status,
                            Map<String, Object> input, Map<String, Object> output, Map<String, Object> metadata,
                            Long sequenceNumber, Integer eventVersion, EventType eventType, String correlationId) {
        OloExecutionEvent event = new OloExecutionEvent();
        event.setRunId(runId);
        event.setNodeId(nodeId);
        event.setParentNodeId(parentNodeId);
        event.setNodeType(NodeType.valueOf(nodeType));
        event.setStatus(NodeStatus.valueOf(status));
        event.setEventType(eventType != null ? eventType : eventTypeFromStatus(NodeStatus.valueOf(status)));
        event.setTimestamp(System.currentTimeMillis());
        if (sequenceNumber != null) event.setSequenceNumber(sequenceNumber);
        if (eventVersion != null) event.setEventVersion(eventVersion);
        String effectiveCorrelationId = correlationId != null ? correlationId : getCorrelationIdFromRun(runId);
        event.setCorrelationId(effectiveCorrelationId);
        event.setInput(input);
        event.setOutput(output);
        event.setMetadata(metadata);
        eventStore.append(runId, event);
        broadcaster.broadcast(runId, event);
        String derivedStatus = deriveRunStatus(eventStore.getEvents(runId));
        runStore.setStatus(runId, derivedStatus);
    }

    private static EventType eventTypeFromStatus(NodeStatus status) {
        if (status == null) return EventType.NODE_STARTED;
        return switch (status) {
            case STARTED -> EventType.NODE_STARTED;
            case COMPLETED -> EventType.NODE_COMPLETED;
            case FAILED -> EventType.NODE_FAILED;
            case WAITING -> EventType.NODE_WAITING;
        };
    }

    private String getCorrelationIdFromRun(String runId) {
        ChatRunStore.RunRecord run = runStore.get(runId);
        return run != null ? run.correlationId : null;
    }

    /** Minimal run status derivation from event stream. Nothing more. */
    private static String deriveRunStatus(List<OloExecutionEvent> events) {
        if (events == null || events.isEmpty()) return "running";
        for (OloExecutionEvent e : events) {
            if (e.getStatus() == NodeStatus.FAILED) return "failed";
        }
        OloExecutionEvent last = events.get(events.size() - 1);
        if (last.getNodeType() == NodeType.SYSTEM && last.getStatus() == NodeStatus.COMPLETED) return "completed";
        if (last.getNodeType() == NodeType.HUMAN && last.getStatus() == NodeStatus.WAITING) return "waiting_human";
        return "running";
    }

    @Override
    public RunEventBroadcaster getBroadcaster() {
        return broadcaster;
    }

    @Override
    public ExecutionEventStore getEventStore() {
        return eventStore;
    }

    @Override
    public ChatRunStore getRunStore() {
        return runStore;
    }
}
