package com.area.server.scheduler;

import com.area.server.dto.WorkflowData;
import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.ServiceConnection;
import com.area.server.model.Workflow;
import com.area.server.model.WorkflowExecutionLog;
import com.area.server.model.WorkflowTriggerState;
import com.area.server.repository.ServiceConnectionRepository;
import com.area.server.repository.WorkflowExecutionLogRepository;
import com.area.server.repository.WorkflowRepository;
import com.area.server.service.WorkflowTriggerStateService;
import com.area.server.service.integration.executor.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler that polls active workflows and executes them based on trigger
 * conditions.
 * This replaces the hardcoded AreaPollingScheduler with a flexible, JSON-driven
 * approach.
 *
 * Key features:
 * - Parses workflow JSON to determine trigger and actions
 * - Uses executor registries to dispatch to correct service implementations
 * - Comprehensive logging using ExternalApiLogger
 * - Circuit breaker pattern to prevent infinite failure loops
 * - Tracks execution state to prevent duplicate processing
 */
@Service
@EnableScheduling
@ConditionalOnProperty(name = "workflow.polling.enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowPollingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowPollingScheduler.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowTriggerStateService stateService;
    private final WorkflowExecutionLogRepository logRepository;
    private final ServiceConnectionRepository connectionRepository;
    private final ActionExecutorRegistry actionExecutorRegistry;
    private final ReactionExecutorRegistry reactionExecutorRegistry;
    private final ExternalApiLogger apiLogger;
    private final ObjectMapper objectMapper;

    private long lastExecutionTime = 0;

    // Default 60s, will be updated by property injection if possible, but hard to
    // capture from @Scheduled
    // We will hardcode to match the 60000 default or use a value injection
    @org.springframework.beans.factory.annotation.Value("${workflow.polling.interval:60000}")
    private long pollingInterval;

    public WorkflowPollingScheduler(WorkflowRepository workflowRepository,
            WorkflowTriggerStateService stateService,
            WorkflowExecutionLogRepository logRepository,
            ServiceConnectionRepository connectionRepository,
            ActionExecutorRegistry actionExecutorRegistry,
            ReactionExecutorRegistry reactionExecutorRegistry,
            ExternalApiLogger apiLogger,
            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.stateService = stateService;
        this.logRepository = logRepository;
        this.connectionRepository = connectionRepository;
        this.actionExecutorRegistry = actionExecutorRegistry;
        this.reactionExecutorRegistry = reactionExecutorRegistry;
        this.apiLogger = apiLogger;
        this.objectMapper = objectMapper;
    }

    public long getLastExecutionTime() {
        return lastExecutionTime;
    }

    public long getPollingInterval() {
        return pollingInterval;
    }

    @Scheduled(fixedDelayString = "${workflow.polling.interval:60000}", initialDelayString = "${workflow.polling.initial-delay:30000}")
    public void pollActiveWorkflows() {
        this.lastExecutionTime = System.currentTimeMillis();
        logger.info("=== Starting WORKFLOW polling cycle (Timestamp: {}) ===", this.lastExecutionTime);
        long startTime = System.currentTimeMillis();

        List<Workflow> activeWorkflows = workflowRepository.findByActive(true);
        logger.info("Found {} active workflow(s) to process", activeWorkflows.size());

        if (activeWorkflows.isEmpty()) {
            logger.info("No active workflows to process");
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        Flux.fromIterable(activeWorkflows)
                .flatMap(workflow -> processWorkflow(workflow)
                        .doOnSuccess(result -> {
                            switch (result.status) {
                                case SUCCESS -> successCount.incrementAndGet();
                                case FAILURE -> failureCount.incrementAndGet();
                                case SKIPPED -> skippedCount.incrementAndGet();
                            }
                        })
                        .onErrorResume(error -> {
                            logger.error("Unexpected error processing workflow {}: {}",
                                    workflow.getId(), error.getMessage(), error);
                            failureCount.incrementAndGet();
                            return Mono.empty();
                        }),
                        5 // Process up to 5 workflows concurrently
                )
                .collectList()
                .block(Duration.ofMinutes(2));

        long duration = System.currentTimeMillis() - startTime;
        logger.info("=== Polling cycle completed in {}ms - Success: {}, Failed: {}, Skipped: {} ===",
                duration, successCount.get(), failureCount.get(), skippedCount.get());
    }

    private Mono<ProcessingResult> processWorkflow(Workflow workflow) {
        long startTime = System.currentTimeMillis();
        logger.debug("Processing workflow {} ({})", workflow.getId(), workflow.getName());

        // Check circuit breaker
        if (stateService.shouldSkipDueToFailures(workflow)) {
            logger.warn("Skipping workflow {} due to circuit breaker (too many consecutive failures)",
                    workflow.getId());
            logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.SKIPPED, null, null,
                    null, "Circuit breaker open - too many consecutive failures",
                    System.currentTimeMillis() - startTime);
            return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.SKIPPED));
        }

        // Parse workflow data
        WorkflowData workflowData;
        try {
            workflowData = parseWorkflowData(workflow);
            if (workflowData == null || workflowData.getTrigger() == null) {
                String error = "Invalid workflow data: missing trigger configuration";
                logger.error("Workflow {} has invalid data: {}", workflow.getId(), error);
                stateService.recordFailure(workflow, error);
                logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.FAILURE, null, null,
                        null, error, System.currentTimeMillis() - startTime);
                return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.FAILURE));
            }
        } catch (Exception e) {
            String error = "Failed to parse workflow data: " + e.getMessage();
            logger.error("Workflow {} parse error: {}", workflow.getId(), error, e);
            stateService.recordFailure(workflow, error);
            logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.FAILURE, null, null,
                    null, error, System.currentTimeMillis() - startTime);
            return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.FAILURE));
        }

        WorkflowData.TriggerConfig trigger = workflowData.getTrigger();
        String triggerType = trigger.getFullType();

        apiLogger.logOperation("WORKFLOW", "CHECK_TRIGGER",
                String.format("Workflow %d - Checking trigger %s", workflow.getId(), triggerType));

        // Get the action executor for this trigger
        ActionExecutor actionExecutor;
        try {
            actionExecutor = actionExecutorRegistry.getExecutor(triggerType);
        } catch (IllegalArgumentException e) {
            String error = "No action executor found for trigger type: " + triggerType;
            logger.error("Workflow {} executor error: {}", workflow.getId(), error);
            stateService.recordFailure(workflow, error);
            logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.FAILURE,
                    trigger.getService(), trigger.getType(), null, error,
                    System.currentTimeMillis() - startTime);
            return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.FAILURE));
        }

        // Create a wrapper that makes Workflow compatible with executor interfaces
        WorkflowWrapper wrapper = new WorkflowWrapper(workflow, workflowData, stateService, connectionRepository);

        return actionExecutor.getTriggerContext(wrapper)
                .flatMap(context -> {
                    // Check if trigger fired
                    if (!hasTriggerFired(context, trigger)) {
                        stateService.updateCheckedTime(workflow);
                        logger.debug("No trigger for workflow {} ({})", workflow.getId(), triggerType);
                        return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.SKIPPED));
                    }

                    Integer triggerCount = context.getInteger("messageCount") != null
                            ? context.getInteger("messageCount")
                            : (context.getInteger("issueCount") != null
                                    ? context.getInteger("issueCount")
                                    : (context.getInteger("prCount") != null ? context.getInteger("prCount") : 1));

                    logger.info("Workflow {} triggered by {} with {} item(s)",
                            workflow.getId(), triggerType, triggerCount);

                    apiLogger.logOperation("WORKFLOW", "TRIGGER_FIRED",
                            String.format("Workflow %d - %s triggered with %d items",
                                    workflow.getId(), triggerType, triggerCount));

                    // Execute all actions/reactions
                    return executeActions(workflow, workflowData, context, wrapper)
                            .then(Mono.fromRunnable(() -> {
                                // Update state after successful execution
                                String lastItemId = extractLastItemId(context, trigger);
                                stateService.updateStateAfterSuccess(workflow, lastItemId, triggerCount);

                                long execTime = System.currentTimeMillis() - startTime;
                                String details = buildExecutionDetails(workflowData, context);

                                logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.SUCCESS,
                                        trigger.getService(), trigger.getType(),
                                        workflowData.getActions().size(), details, execTime);

                                logger.info("Successfully processed workflow {} in {}ms", workflow.getId(), execTime);

                                apiLogger.logOperation("WORKFLOW", "SUCCESS",
                                        String.format("Workflow %d completed in %dms", workflow.getId(), execTime));
                            }))
                            .thenReturn(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.SUCCESS));
                })
                .onErrorResume(error -> {
                    String errorMsg = error.getMessage();
                    logger.error("Failed to process workflow {}: {}", workflow.getId(), errorMsg, error);

                    stateService.recordFailure(workflow, errorMsg);

                    long execTime = System.currentTimeMillis() - startTime;
                    logExecution(workflow, WorkflowExecutionLog.ExecutionStatus.FAILURE,
                            trigger.getService(), trigger.getType(), null, errorMsg, execTime);

                    apiLogger.logError("WORKFLOW", workflow.getId().toString(), error, execTime);

                    return Mono.just(new ProcessingResult(WorkflowExecutionLog.ExecutionStatus.FAILURE));
                });
    }

    /**
     * Parse workflow JSON data into WorkflowData DTO.
     */
    private WorkflowData parseWorkflowData(Workflow workflow) throws JsonProcessingException {
        if (workflow.getWorkflowData() == null || workflow.getWorkflowData().isBlank()) {
            return null;
        }
        return objectMapper.readValue(workflow.getWorkflowData(), WorkflowData.class);
    }

    /**
     * Check if the trigger has fired based on the context.
     */
    private boolean hasTriggerFired(TriggerContext context, WorkflowData.TriggerConfig trigger) {
        // Check for various trigger indicators
        if (context.has("newMessages") && context.getInteger("messageCount") != null) {
            return context.getInteger("messageCount") > 0;
        }
        if (context.has("newIssues") && context.getInteger("issueCount") != null) {
            return context.getInteger("issueCount") > 0;
        }
        if (context.has("newPRs") && context.getInteger("prCount") != null) {
            return context.getInteger("prCount") > 0;
        }
        return false;
    }

    /**
     * Execute all actions/reactions defined in the workflow.
     */
    private Mono<Void> executeActions(Workflow workflow, WorkflowData workflowData,
            TriggerContext context, WorkflowWrapper wrapper) {
        if (workflowData.getActions() == null || workflowData.getActions().isEmpty()) {
            logger.warn("Workflow {} has no actions defined", workflow.getId());
            return Mono.empty();
        }

        return Flux.fromIterable(workflowData.getActions())
                .concatMap(action -> {
                    String reactionType = action.getFullType();

                    apiLogger.logOperation("WORKFLOW", "EXECUTE_ACTION",
                            String.format("Workflow %d - Executing action %s", workflow.getId(), reactionType));

                    try {
                        ReactionExecutor reactionExecutor = reactionExecutorRegistry.getExecutor(reactionType);

                        // Create a wrapper for this specific action
                        ActionWrapper actionWrapper = new ActionWrapper(wrapper, action);

                        return reactionExecutor.execute(actionWrapper, context)
                                .doOnSuccess(v -> {
                                    logger.info("Workflow {} - Action {} executed successfully",
                                            workflow.getId(), reactionType);
                                })
                                .doOnError(error -> {
                                    logger.error("Workflow {} - Action {} failed: {}",
                                            workflow.getId(), reactionType, error.getMessage());
                                });
                    } catch (IllegalArgumentException e) {
                        logger.error("No reaction executor found for type: {}", reactionType);
                        return Mono.error(new RuntimeException("No executor for reaction: " + reactionType));
                    }
                })
                .then();
    }

    /**
     * Extract the last item ID from context for state tracking.
     */
    private String extractLastItemId(TriggerContext context, WorkflowData.TriggerConfig trigger) {
        if (context.has("messageId")) {
            return context.getString("messageId");
        }
        if (context.has("issueNumber")) {
            return "issue:" + context.get("issueNumber");
        }
        if (context.has("prNumber")) {
            return "pr:" + context.get("prNumber");
        }
        return null;
    }

    /**
     * Build execution details string for logging.
     */
    private String buildExecutionDetails(WorkflowData workflowData, TriggerContext context) {
        StringBuilder details = new StringBuilder();

        WorkflowData.TriggerConfig trigger = workflowData.getTrigger();
        details.append("Trigger: ").append(trigger.getFullType()).append(" | ");

        if (context.has("subject")) {
            details.append("Subject: ").append(context.getString("subject")).append(" | ");
        }
        if (context.has("issueTitle")) {
            details.append("Issue: ").append(context.getString("issueTitle")).append(" | ");
        }
        if (context.has("prTitle")) {
            details.append("PR: ").append(context.getString("prTitle")).append(" | ");
        }

        details.append("Actions executed: ").append(workflowData.getActions().size());

        return details.toString();
    }

    /**
     * Log workflow execution to database.
     */
    private void logExecution(Workflow workflow, WorkflowExecutionLog.ExecutionStatus status,
            String triggerService, String triggerAction,
            Integer actionsExecuted, String message, long executionTimeMs) {
        try {
            WorkflowExecutionLog log = new WorkflowExecutionLog();
            log.setWorkflow(workflow);
            log.setExecutedAt(Instant.now());
            log.setStatus(status);
            log.setTriggerService(triggerService);
            log.setTriggerAction(triggerAction);
            log.setActionsExecuted(actionsExecuted);
            log.setExecutionTimeMs(executionTimeMs);

            if (status == WorkflowExecutionLog.ExecutionStatus.SUCCESS) {
                log.setExecutionDetails(message);
            } else if (status == WorkflowExecutionLog.ExecutionStatus.FAILURE) {
                log.setErrorMessage(message);
            }

            logRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log execution for workflow {}: {}", workflow.getId(), e.getMessage());
        }
    }

    static class ProcessingResult {
        final WorkflowExecutionLog.ExecutionStatus status;

        ProcessingResult(WorkflowExecutionLog.ExecutionStatus status) {
            this.status = status;
        }
    }
}
