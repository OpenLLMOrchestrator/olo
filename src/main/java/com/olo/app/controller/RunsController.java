package com.olo.app.controller;

import com.olo.app.api.request.AppendEventRequest;
import com.olo.app.api.request.CreateRunRequest;
import com.olo.app.api.request.HumanInputRequest;
import com.olo.app.api.response.CreateRunResponse;
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
        OloExecutionEvent startEvent = new OloExecutionEvent();
        startEvent.setRunId(runId);
        startEvent.setNodeId("root");
        startEvent.setParentNodeId(null);
        startEvent.setNodeType(NodeType.SYSTEM);
        startEvent.setStatus(NodeStatus.STARTED);
        startEvent.setTimestamp(System.currentTimeMillis());
        startEvent.setInput(Map.of(
                "type", request.getInput().getType(),
                "message", request.getInput().getMessage() != null ? request.getInput().getMessage() : ""
        ));
        startEvent.setOutput(null);
        startEvent.setMetadata(Map.of("tenantId", request.getTenantId()));

        runService.appendEvent(runId, "root", null, "SYSTEM", "STARTED",
                startEvent.getInput(), null, startEvent.getMetadata());

        runStore.put(new ChatRunStore.RunRecord(runId, "", ""));
        String userMessage = request.getInput().getMessage() != null ? request.getInput().getMessage() : "";
        String pipeline = (request.getTaskQueue() != null && !request.getTaskQueue().isBlank())
                ? request.getTaskQueue().trim()
                : taskQueue;
        com.olo.input.model.WorkflowInput workflowInput = WorkflowInputSerializer.build(
                request.getTenantId(), "", "", userMessage, pipeline, runId, runId, callbackBaseUrl);
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

    @Operation(summary = "Append event (worker)", description = "Worker callback: append one execution event and broadcast to SSE subscribers")
    @PostMapping("/{runId}/events")
    public ResponseEntity<Void> appendEvent(@PathVariable String runId, @RequestBody AppendEventRequest body) {
        runService.appendEvent(
                runId,
                body.getNodeId(),
                body.getParentNodeId(),
                body.getNodeType() != null ? body.getNodeType().name() : "SYSTEM",
                body.getStatus() != null ? body.getStatus().name() : "STARTED",
                body.getInput(),
                body.getOutput(),
                body.getMetadata()
        );
        return ResponseEntity.noContent().build();
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
        return ResponseEntity.ok(Map.of(
                "runId", run.runId,
                "sessionId", run.sessionId,
                "messageId", run.messageId,
                "status", run.status,
                "createdAt", run.createdAt
        ));
    }
}
