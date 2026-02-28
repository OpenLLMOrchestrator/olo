package com.olo.worker;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowMethod;

/**
 * Olo chat workflow: planner → optional tool → model → optional human → final answer.
 * Receives WorkflowInput (JSON type); Temporal serializes it as a JSON object.
 */
public interface OloChatWorkflow {

    @WorkflowMethod
    void execute(com.olo.input.model.WorkflowInput workflowInput);

    @SignalMethod
    void humanInput(boolean approved, String message);
}
