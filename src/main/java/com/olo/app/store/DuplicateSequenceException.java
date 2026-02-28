package com.olo.app.store;

/**
 * Thrown when an event is appended with a sequenceNumber that already exists for the run.
 * Enforces UNIQUE(runId, sequenceNumber). Caller (e.g. controller) should respond 409 Conflict.
 */
public class DuplicateSequenceException extends RuntimeException {

    private final String runId;
    private final Long sequenceNumber;

    public DuplicateSequenceException(String runId, Long sequenceNumber) {
        super("Duplicate sequenceNumber " + sequenceNumber + " for runId " + runId);
        this.runId = runId;
        this.sequenceNumber = sequenceNumber;
    }

    public String getRunId() { return runId; }
    public Long getSequenceNumber() { return sequenceNumber; }
}
