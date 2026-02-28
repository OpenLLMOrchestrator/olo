package com.olo.app.controller;

import com.olo.app.api.request.CreateSessionRequest;
import com.olo.app.api.request.SendMessageRequest;
import com.olo.app.api.response.CreateSessionResponse;
import com.olo.app.api.response.SendMessageResponse;
import com.olo.app.domain.NodeStatus;
import com.olo.app.domain.NodeType;
import com.olo.app.domain.OloExecutionEvent;
import com.olo.app.service.RunService;
import com.olo.app.store.*;
import com.olo.app.workflow.impl.WorkflowInputSerializer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Sessions", description = "Chat sessions and messages")
public class SessionsController {

    private final ChatSessionStore sessionStore;
    private final ChatMessageStore messageStore;
    private final ChatRunStore runStore;
    private final ExecutionEventStore eventStore;
    private final RunService runService;
    private final String taskQueue;
    private final String callbackBaseUrl;

    public SessionsController(ChatSessionStore sessionStore,
                              ChatMessageStore messageStore,
                              ChatRunStore runStore,
                              ExecutionEventStore eventStore,
                              RunService runService,
                              @Qualifier("oloTaskQueue") String taskQueue,
                              @Qualifier("oloCallbackBaseUrl") String callbackBaseUrl) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.runStore = runStore;
        this.eventStore = eventStore;
        this.runService = runService;
        this.taskQueue = taskQueue;
        this.callbackBaseUrl = callbackBaseUrl;
    }

    @Operation(summary = "Create session", description = "Create a new chat session for a tenant")
    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(new ChatSessionStore.SessionRecord(sessionId, request.getTenantId()));
        return ResponseEntity.ok(new CreateSessionResponse(sessionId));
    }

    @Operation(summary = "Get session", description = "Get session by ID")
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        ChatSessionStore.SessionRecord session = sessionStore.get(sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "sessionId", session.sessionId,
                "tenantId", session.tenantId,
                "createdAt", session.createdAt
        ));
    }

    @Operation(summary = "Send message", description = "Send user message: creates ChatMessage and ChatRun, starts workflow, returns runId for SSE event stream")
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(@PathVariable String sessionId,
                                                          @Valid @RequestBody SendMessageRequest request) {
        ChatSessionStore.SessionRecord session = sessionStore.get(sessionId);
        if (session == null) return ResponseEntity.notFound().build();

        String messageId = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();

        messageStore.put(new ChatMessageStore.MessageRecord(
                messageId, sessionId, "user", request.getContent(), runId));
        runStore.put(new ChatRunStore.RunRecord(runId, sessionId, messageId));

        OloExecutionEvent startEvent = new OloExecutionEvent();
        startEvent.setRunId(runId);
        startEvent.setNodeId("root");
        startEvent.setParentNodeId(null);
        startEvent.setNodeType(NodeType.SYSTEM);
        startEvent.setStatus(NodeStatus.STARTED);
        startEvent.setTimestamp(System.currentTimeMillis());
        startEvent.setInput(Map.of("message", request.getContent(), "type", "chat"));
        startEvent.setOutput(null);
        startEvent.setMetadata(Map.of("tenantId", session.tenantId, "sessionId", sessionId, "messageId", messageId));

        runService.appendEvent(runId, "root", null, "SYSTEM", "STARTED",
                startEvent.getInput(), null, startEvent.getMetadata());

        String pipeline = (request.getTaskQueue() != null && !request.getTaskQueue().isBlank())
                ? request.getTaskQueue().trim()
                : taskQueue;
        com.olo.input.model.WorkflowInput workflowInput = WorkflowInputSerializer.build(
                session.tenantId, sessionId, messageId, request.getContent(), pipeline, runId, runId, callbackBaseUrl);
        runService.startWorkflow(runId, workflowInput, request.getTaskQueue());

        return ResponseEntity.ok(new SendMessageResponse(messageId, runId));
    }

    @Operation(summary = "List messages", description = "List all messages in the session")
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<Map<String, Object>>> listMessages(@PathVariable String sessionId) {
        if (sessionStore.get(sessionId) == null) return ResponseEntity.notFound().build();
        List<ChatMessageStore.MessageRecord> list = messageStore.listBySession(sessionId);
        return ResponseEntity.ok(list.stream()
                .map(m -> Map.<String, Object>of(
                        "messageId", m.messageId,
                        "sessionId", m.sessionId,
                        "role", m.role,
                        "content", m.content,
                        "runId", m.runId != null ? m.runId : "",
                        "createdAt", m.createdAt
                ))
                .collect(Collectors.toList()));
    }
}
