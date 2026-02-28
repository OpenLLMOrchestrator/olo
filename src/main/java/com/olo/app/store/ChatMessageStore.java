package com.olo.app.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for chat messages. Demo only.
 */
public class ChatMessageStore {

    public static class MessageRecord {
        public final String messageId;
        public final String sessionId;
        public final String role;   // user | assistant | system
        public final String content;
        public final String runId;  // null until run is created for this message
        public final long createdAt;

        public MessageRecord(String messageId, String sessionId, String role, String content, String runId) {
            this.messageId = messageId;
            this.sessionId = sessionId;
            this.role = role;
            this.content = content;
            this.runId = runId;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private final Map<String, MessageRecord> byId = new ConcurrentHashMap<>();
    /** sessionId -> ordered message ids */
    private final Map<String, List<String>> bySession = new ConcurrentHashMap<>();

    public void put(MessageRecord msg) {
        byId.put(msg.messageId, msg);
        bySession.computeIfAbsent(msg.sessionId, k -> new ArrayList<>()).add(msg.messageId);
    }

    public MessageRecord get(String messageId) {
        return byId.get(messageId);
    }

    public List<MessageRecord> listBySession(String sessionId) {
        List<String> ids = bySession.get(sessionId);
        if (ids == null) return List.of();
        return ids.stream().map(byId::get).filter(m -> m != null).toList();
    }
}
