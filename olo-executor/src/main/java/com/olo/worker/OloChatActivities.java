package com.olo.worker;

import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * Activities for Olo chat: report event to Chat BE, planner, tool, model.
 */
public interface OloChatActivities {

    /** POST event to Chat BE so it can persist and SSE. */
    @ActivityMethod
    void reportEvent(String runId, String callbackBaseUrl, String nodeId, String parentNodeId,
                     String nodeType, String status,
                     Map<String, Object> input, Map<String, Object> output, Map<String, Object> metadata);

    /** Returns: TOOL | MODEL | HUMAN | DONE */
    @ActivityMethod
    String planner(String userMessage, String stepsDone);

    /** Tool invocation; returns result map. */
    @ActivityMethod
    Map<String, Object> runTool(String toolName, Map<String, Object> params);

    /** Model generates response from context. */
    @ActivityMethod
    String runModel(String userMessage, String toolResultOrNull);
}
