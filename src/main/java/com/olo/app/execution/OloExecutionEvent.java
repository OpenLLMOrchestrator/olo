package com.olo.app.execution;

import java.util.Map;

/**
 * Core execution model. Emit this for: workflow start, planner decision,
 * model call, tool call, human wait/completion, retry, failure.
 * Required for replay/diff.
 */
public class OloExecutionEvent {

    private String runId;
    private String nodeId;
    private String parentNodeId;

    private NodeType nodeType;
    private NodeStatus status;

    private long timestamp;

    private Map<String, Object> input;
    private Map<String, Object> output;
    private Map<String, Object> metadata;

    public OloExecutionEvent() {}

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) { this.parentNodeId = parentNodeId; }

    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
