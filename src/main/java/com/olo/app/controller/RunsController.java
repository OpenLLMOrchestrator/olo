package com.olo.app.controller;

import com.olo.app.api.request.AppendEventRequest;
import com.olo.app.api.request.CreateRunRequest;
import com.olo.app.api.request.HumanInputRequest;
import com.olo.app.api.response.CreateRunResponse;
import com.olo.app.domain.EventType;
import com.olo.app.domain.NodeStatus;
import com.olo.app.domain.NodeType;
import com.olo.app.domain.OloExecutionEvent;
import com.olo.app.service.RunService;
import com.olo.app.store.ChatRunStore;
import com.olo.app.workflow.impl.WorkflowInputSerializer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/runs")
@Tag(name = "Runs", description = "Chat runs, execution events (SSE), and human approval")
public class RunsController {

    private final RunService runService;
    private final ChatRunStore runStore;
    private final String taskQueue;
    private final String callbackBaseUrl;

    public RunsController(RunService runService, ChatRunStore runStore,
                          @Qualifier("oloTaskQueue") String taskQueue,
                          @Qualifier("oloCallbackBaseUrl") String callbackBaseUrl) {
        this.runService = runService;
        this.runStore = runStore;
        this.taskQueue = taskQueue;
        this.callbackBaseUrl = callbackBaseUrl;
    }

    @Operation(summary = "Create run", description = "Create a run and start the chat workflow. No session/message record; use POST /api/sessions/{id}/messages for session-bound flow.")
    @PostMapping
    public ResponseEntity<CreateRunResponse> createRun(@Valid @RequestBody CreateRunRequest request) {
        String runId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        runService.appendEvent(runId, "root", null, "SYSTEM", "STARTED",
                Map.of(
                        "type", request.getInput().getType(),
                        "message", request.getInput().getMessage() != null ? request.getInput().getMessage() : ""
                ),
                null,
                Map.of("tenantId", request.getTenantId()),
                null, null, EventType.NODE_STARTED, correlationId);

        runStore.put(new ChatRunStore.RunRecord(runId, "", "", request.getTenantId(), correlationId,
                request.getWorkflowVersion(), request.getModelVersion(), request.getPlannerVersion()));
        String userMessage = request.getInput().getMessage() != null ? request.getInput().getMessage() : "";
        String pipeline = (request.getTaskQueue() != null && !request.getTaskQueue().isBlank())
                ? request.getTaskQueue().trim()
                : taskQueue;
        com.olo.input.model.WorkflowInput workflowInput = WorkflowInputSerializer.build(
                request.getTenantId(), "", "", userMessage, pipeline, runId, runId, callbackBaseUrl, correlationId);
        runService.startWorkflow(runId, workflowInput, request.getTaskQueue());

        return ResponseEntity.ok(new CreateRunResponse(runId));
    }

    @Operation(summary = "Stream run events (SSE)", description = "Server-sent events stream: catch-up then live execution events (PLANNER, TOOL, MODEL, HUMAN)")
    @GetMapping(value = "/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String runId) {
        SseEmitter emitter = new SseEmitter(0L);
        runService.getBroadcaster().subscribeWithCatchUp(runId, emitter);
        return emitter;
    }

    @Operation(summary = "Append event (executor)", description = "Executor callback: append one execution event. sequenceNumber required; duplicate returns 409.")
    @PostMapping("/{runId}/events")
    public ResponseEntity<Void> appendEvent(@PathVariable String runId, @RequestBody AppendEventRequest body) {
        if (body.getSequenceNumber() == null) {
            return ResponseEntity.badRequest().build();
        }
        String correlationId = body.getCorrelationId();
        if (correlationId == null) {
            ChatRunStore.RunRecord run = runStore.get(runId);
            if (run != null) correlationId = run.correlationId;
        }
        try {
            runService.appendEvent(
                    runId,
                    body.getNodeId(),
                    body.getParentNodeId(),
                    body.getNodeType() != null ? body.getNodeType().name() : "SYSTEM",
                    body.getStatus() != null ? body.getStatus().name() : "STARTED",
                    body.getInput(),
                    body.getOutput(),
                    body.getMetadata(),
                    body.getSequenceNumber(),
                    body.getEventVersion(),
                    body.getEventType(),
                    correlationId
            );
            return ResponseEntity.noContent().build();
        } catch (com.olo.app.store.DuplicateSequenceException e) {
            return ResponseEntity.status(409).build();
        }
    }

    @Operation(summary = "Human input", description = "User approval or text for a HUMAN step; signals the workflow and resumes execution")
    @PostMapping("/{runId}/human-input")
    public ResponseEntity<Void> humanInput(@PathVariable String runId, @RequestBody(required = false) HumanInputRequest body) {
        boolean approved = body != null && body.isApproved();
        String message = body != null ? body.getMessage() : null;
        runService.signalHumanInput(runId, approved, message);
        runStore.setStatus(runId, "running");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get run", description = "Get run status and metadata")
    @GetMapping("/{runId}")
    public ResponseEntity<Map<String, Object>> getRun(@PathVariable String runId) {
        ChatRunStore.RunRecord run = runStore.get(runId);
        if (run == null) return ResponseEntity.notFound().build();
        Map<String, Object> map = new java.util.HashMap<>(Map.of(
                "runId", run.runId,
                "sessionId", run.sessionId,
                "messageId", run.messageId,
                "status", run.status,
                "createdAt", run.createdAt
        ));
        if (run.correlationId != null) map.put("correlationId", run.correlationId);
        if (run.workflowVersion != null) map.put("workflowVersion", run.workflowVersion);
        if (run.modelVersion != null) map.put("modelVersion", run.modelVersion);
        if (run.plannerVersion != null) map.put("plannerVersion", run.plannerVersion);
        return ResponseEntity.ok(map);
    }
}
