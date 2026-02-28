package com.olo.app.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for chat sessions. Demo only.
 */
public class ChatSessionStore {

    public static class SessionRecord {
        public final String sessionId;
        public final String tenantId;
        public final long createdAt;

        public SessionRecord(String sessionId, String tenantId) {
            this.sessionId = sessionId;
            this.tenantId = tenantId;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    public void put(SessionRecord session) {
        sessions.put(session.sessionId, session);
    }

    public SessionRecord get(String sessionId) {
        return sessions.get(sessionId);
    }
}
