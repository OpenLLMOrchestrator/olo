package com.olo.app.domain;

/**
 * Explicit event type for analytics and filtering. Prefer over deriving from nodeType + status.
 */
public enum EventType {
    NODE_STARTED,
    NODE_COMPLETED,
    NODE_FAILED,
    NODE_WAITING
}
