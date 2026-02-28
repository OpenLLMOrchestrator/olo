package com.olo.app.workflow;

import com.olo.input.model.Context;
import com.olo.input.model.InputItem;
import com.olo.input.model.InputType;
import com.olo.input.model.Metadata;
import com.olo.input.model.Routing;
import com.olo.input.model.Storage;
import com.olo.input.model.StorageMode;
import com.olo.input.model.TransactionType;
import com.olo.input.model.WorkflowInput;

import java.util.Collections;
import java.util.List;

/**
 * Builds and serializes {@link WorkflowInput} (olo-worker-input format) for the chat workflow.
 * Use this before starting the workflow so the worker receives the standard input format.
 */
public final class WorkflowInputSerializer {

    private static final String VERSION = "2.0";
    private static final String USER_QUERY_INPUT_NAME = "userQuery";
    private static final String USER_QUERY_DISPLAY_NAME = "User query";

    /**
     * Builds and returns the WorkflowInput object (JSON type).
     * Pass this to the workflow so Temporal serializes it as a JSON object, not as a string.
     *
     * @param tenantId        tenant id (required)
     * @param sessionId       session id (optional, use "" if none)
     * @param messageId       message id (optional, use "" if none)
     * @param userMessage     user message content (e.g. "Search news about Tesla.")
     * @param pipeline        task queue / pipeline name (e.g. "olo-chat")
     * @param transactionId   transaction id (e.g. runId)
     * @param runId           run id (included in context)
     * @param callbackBaseUrl callback base URL (included in context)
     */
    public static WorkflowInput build(String tenantId,
                                     String sessionId,
                                     String messageId,
                                     String userMessage,
                                     String pipeline,
                                     String transactionId,
                                     String runId,
                                     String callbackBaseUrl) {
        String userMessageSafe = userMessage != null ? userMessage : "";

        InputItem userQueryInput = new InputItem(
                USER_QUERY_INPUT_NAME,
                USER_QUERY_DISPLAY_NAME,
                InputType.STRING,
                new Storage(StorageMode.LOCAL, null, null),
                userMessageSafe
        );

        Context context = new Context(
                tenantId != null ? tenantId : "",
                "",
                List.of("PUBLIC"),
                Collections.emptyList(),
                sessionId != null ? sessionId : "",
                runId != null ? runId : "",
                callbackBaseUrl != null ? callbackBaseUrl : ""
        );

        Routing routing = new Routing(
                pipeline != null ? pipeline : "olo-chat",
                TransactionType.QUESTION_ANSWER,
                transactionId != null ? transactionId : ""
        );

        Metadata metadata = new Metadata(null, 0L);

        return WorkflowInput.builder()
                .version(VERSION)
                .addInput(userQueryInput)
                .context(context)
                .routing(routing)
                .metadata(metadata)
                .build();
    }

}
