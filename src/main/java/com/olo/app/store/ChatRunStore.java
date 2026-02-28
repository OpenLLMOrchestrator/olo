package com.olo.app.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for run metadata. Demo only.
 */
public class ChatRunStore {

    public static class RunRecord {
        public final String runId;
        public final String sessionId;
        public final String messageId;
        public volatile String status; // running | completed | failed | waiting_human
        public final long createdAt;

        public RunRecord(String runId, String sessionId, String messageId) {
            this.runId = runId;
            this.sessionId = sessionId;
            this.messageId = messageId;
            this.status = "running";
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();

    public void put(RunRecord run) {
        runs.put(run.runId, run);
    }

    public RunRecord get(String runId) {
        return runs.get(runId);
    }

    public void setStatus(String runId, String status) {
        RunRecord r = runs.get(runId);
        if (r != null) r.status = status;
    }
}
