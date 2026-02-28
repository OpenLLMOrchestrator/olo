package com.olo.app.config;

import com.olo.app.store.*;
import com.olo.sdk.TemporalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoConfig {

    @Value("${olo.temporal.target:localhost:7233}")
    private String temporalTarget;

    @Value("${olo.temporal.namespace:default}")
    private String temporalNamespace;

    @Value("${olo.chat.callback-base-url:http://localhost:7080}")
    private String callbackBaseUrl;

    @Value("${olo.temporal.task-queue:olo-chat}")
    private String taskQueue;

    @Value("${olo.temporal.workflow-type:OloKernelWorkflow}")
    private String workflowTypeDefault;

    @Bean
    public ExecutionEventStore executionEventStore() {
        return new ExecutionEventStore();
    }

    @Bean
    public RunEventBroadcaster runEventBroadcaster(ExecutionEventStore executionEventStore) {
        return new RunEventBroadcaster(executionEventStore);
    }

    @Bean
    public ChatSessionStore chatSessionStore() {
        return new ChatSessionStore();
    }

    @Bean
    public ChatMessageStore chatMessageStore() {
        return new ChatMessageStore();
    }

    @Bean
    public ChatRunStore chatRunStore() {
        return new ChatRunStore();
    }

    @Bean
    public TemporalClient temporalClient() {
        String workflowType = System.getenv("OLO_WORKFLOW_TYPE");
        if (workflowType == null || workflowType.isEmpty()) {
            workflowType = workflowTypeDefault;
        }
        return TemporalClient.newBuilder()
                .target(temporalTarget)
                .namespace(temporalNamespace)
                .workflowType(workflowType)
                .build();
    }

    @Bean(name = "oloCallbackBaseUrl")
    public String callbackBaseUrl() {
        return callbackBaseUrl;
    }

    @Bean(name = "oloTaskQueue")
    public String taskQueue() {
        return taskQueue;
    }
}
