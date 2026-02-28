# How to Test Using Swagger UI

Use Swagger UI to exercise the Olo chat backend APIs from your browser. The backend and executor (olo-executor) must be running (see [DEMO.md](DEMO.md)).

---

## 1. Open Swagger UI

1. Start the backend (e.g. `start.bat` or `./gradlew bootRun`).
2. In your browser go to: **http://localhost:7080/swagger-ui.html**
3. You’ll see the API grouped by **Sessions** and **Runs**.

---

## 2. Prerequisites

- **Backend** running on port 7080.
- **Temporal** running (e.g. `docker run -d -p 7233:7233 temporalio/auto-setup`).
- **Executor** (olo-executor) running in a separate terminal:  
  `cd olo-executor && mvn exec:java -Dexec.mainClass="com.olo.worker.OloExecutorMain"`

Without the executor, sending a message will start a workflow but you won’t see PLANNER/TOOL/MODEL/HUMAN events.

---

## 3. Test Flow (Full Chat Demo)

### Step 1: Create a session

1. Expand **Sessions** → **POST /api/sessions**.
2. Click **Try it out**.
3. In **Request body** use:

```json
{
  "tenantId": "demo"
}
```

4. Click **Execute**.
5. In the response, copy **sessionId** (e.g. `a1b2c3d4-e5f6-...`).

---

### Step 2: Send first message ("Search news about Tesla")

1. Expand **Sessions** → **POST /api/sessions/{sessionId}/messages**.
2. Click **Try it out**.
3. **sessionId**: paste the session ID from Step 1.
4. **Request body**:

```json
{
  "content": "Search news about Tesla."
}
```

5. Click **Execute**.
6. In the response, copy **runId** (you need it for the event stream and, later, for human input).

---

### Step 3: Stream run events (SSE)

Swagger UI does **not** show live SSE streams in the browser. To see events:

- **Option A:** Open a new browser tab and go to:  
  `http://localhost:7080/api/runs/{runId}/events`  
  (replace `{runId}` with the runId from Step 2). You’ll see event stream lines as the workflow runs.
- **Option B:** Use curl in a terminal:  
  `curl -N http://localhost:7080/api/runs/{runId}/events`

You should see **SYSTEM** STARTED → **PLANNER** COMPLETED → **TOOL** COMPLETED → **MODEL** COMPLETED → **SYSTEM** COMPLETED (if the executor is running).

---

### Step 4: Send second message ("Send this for approval")

1. **POST /api/sessions/{sessionId}/messages** again with the **same sessionId**.
2. **Request body**:

```json
{
  "content": "Send this for approval."
}
```

3. Click **Execute** and copy the new **runId** from the response.

---

### Step 5: Stream events for the second run

Open `http://localhost:7080/api/runs/{runId}/events` (or use curl) with the **new runId**. You should see **PLANNER** → **HUMAN** WAITING (workflow is waiting for approval).

---

### Step 6: Send human approval (unblock HUMAN step)

1. Expand **Runs** → **POST /api/runs/{runId}/human-input**.
2. Click **Try it out**.
3. **runId**: paste the runId from Step 4.
4. **Request body** (approve):

```json
{
  "approved": true,
  "message": ""
}
```

Or with a message:

```json
{
  "approved": true,
  "message": "Approved with comments."
}
```

5. Click **Execute** (response is 204 No Content).
6. In the SSE tab for that runId, you should see **HUMAN** COMPLETED → **MODEL** COMPLETED → **SYSTEM** COMPLETED.

---

## 4. Other Endpoints in Swagger UI

| Endpoint | Use |
|----------|-----|
| **GET /api/sessions/{sessionId}** | Get session by ID. |
| **GET /api/sessions/{sessionId}/messages** | List messages in the session. |
| **GET /api/runs/{runId}** | Get run status and metadata. |
| **POST /api/runs** | Create run (legacy; prefer POST session + send message for full flow). |

---

## 5. Tips

- **SSE in Swagger:** The **GET /api/runs/{runId}/events** endpoint is listed in Swagger, but “Execute” there won’t show a live stream in the response panel. Use a separate browser tab or curl for live events.
- **Run IDs:** Each “Send message” creates a new run. Use the runId from the send-message response for that run’s events and human-input.
- **Human step:** Only the second message in the demo triggers a HUMAN step. Use the runId of that run when calling **POST /api/runs/{runId}/human-input**.
- **Validation:** `tenantId` and `content` are required (non-empty). Swagger shows the schema; invalid bodies return 400.

---

## 6. Quick Reference – Request Bodies

**Create session**

```json
{ "tenantId": "demo" }
```

**Send message** (optional `taskQueue` from frontend)

```json
{ "content": "Your message here.", "taskQueue": "olo-chat-queue-oolama-debug" }
```

**Create run** (optional `taskQueue` from frontend)

```json
{
  "tenantId": "2a2a91fb-f5b4-4cf0-b917-524d242b2e3d",
  "input": { "type": "string", "message": "Search news about Tesla." },
  "taskQueue": "olo-chat-queue-oolama-debug"
}
```

**Human input (approve)**

```json
{ "approved": true, "message": "" }
```

**Human input (approve with text)**

```json
{ "approved": true, "message": "Optional comment." }
```

---

## Related docs

- **[SDK_AND_BACKEND.md](SDK_AND_BACKEND.md)** — How the backend uses the SDK, WorkflowInput, task queue, and config.
- **[WORKFLOW_INPUT.md](WORKFLOW_INPUT.md)** — Workflow payload format (version 2.0, inputs, context, routing).
