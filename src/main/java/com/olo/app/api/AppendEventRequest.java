package com.olo.app.api;

import com.olo.app.execution.NodeStatus;
import com.olo.app.execution.NodeType;

import java.util.Map;

/**
 * Request body for POST /api/runs/{runId}/events (worker callback).
 */
public class AppendEventRequest {

    private String nodeId;
    private String parentNodeId;
    private NodeType nodeType;
    private NodeStatus status;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private Map<String, Object> metadata;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) { this.parentNodeId = parentNodeId; }

    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
