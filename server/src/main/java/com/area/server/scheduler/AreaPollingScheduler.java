package com.area.server.scheduler;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.service.TriggerStateService;
import com.area.server.service.integration.executor.ActionExecutor;
import com.area.server.service.integration.executor.ActionExecutorRegistry;
import com.area.server.service.integration.executor.ReactionExecutor;
import com.area.server.service.integration.executor.ReactionExecutorRegistry;
import com.area.server.service.integration.executor.TriggerContext;
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

@Service
@EnableScheduling
@ConditionalOnProperty(name = "area.polling.enabled", havingValue = "true", matchIfMissing = true)
public class AreaPollingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AreaPollingScheduler.class);

    private final AreaRepository areaRepository;
    private final TriggerStateService stateService;
    private final AreaExecutionLogRepository logRepository;
    private final ActionExecutorRegistry actionExecutorRegistry;
    private final ReactionExecutorRegistry reactionExecutorRegistry;

    public AreaPollingScheduler(AreaRepository areaRepository,
            TriggerStateService stateService,
            AreaExecutionLogRepository logRepository,
            ActionExecutorRegistry actionExecutorRegistry,
            ReactionExecutorRegistry reactionExecutorRegistry) {
        this.areaRepository = areaRepository;
        this.stateService = stateService;
        this.logRepository = logRepository;
        this.actionExecutorRegistry = actionExecutorRegistry;
        this.reactionExecutorRegistry = reactionExecutorRegistry;
    }

    @Scheduled(fixedDelayString = "${area.polling.interval:60000}", initialDelayString = "${area.polling.initial-delay:30000}")
    public void pollActiveAreas() {
        logger.info("=== Starting AREA polling cycle ===");
        long startTime = System.currentTimeMillis();

        // ONLY poll non-timer areas (timers are handled by TimerPollingScheduler)
        List<Area> activeAreas = areaRepository.findActiveNonTimerAreas();
        logger.info("Found {} active area(s) to process", activeAreas.size());

        if (activeAreas.isEmpty()) {
            logger.info("No active areas to process");
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        Flux.fromIterable(activeAreas)
                .flatMap(area -> processArea(area)
                        .doOnSuccess(result -> {
                            switch (result.status) {
                                case SUCCESS -> successCount.incrementAndGet();
                                case FAILURE -> failureCount.incrementAndGet();
                                case SKIPPED -> skippedCount.incrementAndGet();
                            }
                        })
                        .onErrorResume(error -> {
                            logger.error("Unexpected error processing area {}", area.getId(), error);
                            failureCount.incrementAndGet();
                            return Mono.empty();
                        }),
                        5)
                .collectList()
                .block(Duration.ofMinutes(2));

        long duration = System.currentTimeMillis() - startTime;
        logger.info("=== Polling cycle completed in {}ms - Success: {}, Failed: {}, Skipped: {} ===",
                duration, successCount.get(), failureCount.get(), skippedCount.get());
    }

    private Mono<ProcessingResult> processArea(Area area) {
        long startTime = System.currentTimeMillis();
        logger.debug("Processing area {}", area.getId());

        if (stateService.shouldSkipDueToFailures(area)) {
            logger.warn("Skipping area {} due to circuit breaker (too many consecutive failures)", area.getId());
            logExecution(area, AreaExecutionLog.ExecutionStatus.SKIPPED, null,
                    "Circuit breaker open - too many consecutive failures",
                    System.currentTimeMillis() - startTime);
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
        }

        // Get the appropriate executors based on the area's action and reaction types
        String actionType = determineActionType(area);
        String reactionType = determineReactionType(area);

        ActionExecutor actionExecutor;
        ReactionExecutor reactionExecutor;

        try {
            actionExecutor = actionExecutorRegistry.getExecutorForAction(actionType);
            reactionExecutor = reactionExecutorRegistry.getExecutor(reactionType);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to get executors for area {}: {}", area.getId(), e.getMessage());
            stateService.recordFailure(area, e.getMessage());
            logExecution(area, AreaExecutionLog.ExecutionStatus.FAILURE, null, e.getMessage(),
                    System.currentTimeMillis() - startTime);
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.FAILURE));
        }

        // Use the executor framework
        return actionExecutor.getTriggerContext(area)
                .flatMap(context -> {
                    // For Gmail-specific logic
                    if (actionType.startsWith("gmail.")) {
                        return processGmailAction(area, context, reactionExecutor, startTime);
                    }

                    // Generic action processing - check if explicitly not triggered
                    Boolean triggered = context.getBoolean("triggered");
                    if (triggered != null && !triggered) {
                        stateService.updateCheckedTime(area);
                        logger.debug("Action not triggered for area {}", area.getId());
                        return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
                    }

                    logger.info("Area {} triggered with action type: {}", area.getId(), actionType);

                    // Execute the reaction
                    return reactionExecutor.execute(area, context)
                            .then(Mono.fromRunnable(() -> {
                                stateService.updateCheckedTime(area);

                                long execTime = System.currentTimeMillis() - startTime;
                                String logMessage = String.format("Executed action: %s", actionType);
                                logExecution(area, AreaExecutionLog.ExecutionStatus.SUCCESS,
                                        null, logMessage, execTime);

                                logger.info("Successfully processed area {} in {}ms", area.getId(), execTime);
                            }))
                            .thenReturn(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SUCCESS));
                })
                .onErrorResume(error -> {
                    String errorMsg = error.getMessage();
                    logger.error("Failed to process area {}: {}", area.getId(), errorMsg, error);

                    stateService.recordFailure(area, errorMsg);

                    long execTime = System.currentTimeMillis() - startTime;
                    logExecution(area, AreaExecutionLog.ExecutionStatus.FAILURE, null, errorMsg, execTime);

                    return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.FAILURE));
                });
    }

    private String determineActionType(Area area) {
        // Check if actionType is explicitly set
        if (area.getActionType() != null) {
            return area.getActionType();
        }

        // Fallback to connection type for backward compatibility
        if (area.getActionConnection() != null) {
            switch (area.getActionConnection().getType()) {
                case GMAIL:
                    return "gmail.email_received";
                default:
                    throw new IllegalStateException(
                            "Unknown action connection type: " + area.getActionConnection().getType());
            }
        }

        throw new IllegalStateException("Area has no action type or action connection");
    }

    private String determineReactionType(Area area) {
        // Check if reactionType is explicitly set
        if (area.getReactionType() != null) {
            return area.getReactionType();
        }

        // Fallback to connection type for backward compatibility
        if (area.getReactionConnection() != null) {
            switch (area.getReactionConnection().getType()) {
                case DISCORD:
                    return "discord.send_webhook";
                default:
                    throw new IllegalStateException(
                            "Unknown reaction connection type: " + area.getReactionConnection().getType());
            }
        }

        throw new IllegalStateException("Area has no reaction type or reaction connection");
    }

    private Mono<ProcessingResult> processGmailAction(Area area, TriggerContext context,
            ReactionExecutor reactionExecutor, long startTime) {
        // Check if there are new messages
        if (!context.has("newMessages") || context.getInteger("messageCount") == null
                || context.getInteger("messageCount") == 0) {
            stateService.updateCheckedTime(area);
            logger.debug("No new messages for area {}", area.getId());
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
        }

        @SuppressWarnings("unchecked")
        List<GmailMessage> newMessages = (List<GmailMessage>) context.get("newMessages");

        if (!stateService.shouldTrigger(area, newMessages)) {
            stateService.updateCheckedTime(area);
            logger.debug("Messages found but should not trigger for area {}", area.getId());
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
        }

        GmailMessage latestMessage = newMessages.get(0);
        Integer messageCount = context.getInteger("messageCount");
        logger.info("Area {} triggered with {} new message(s). Latest: '{}'",
                area.getId(), messageCount, latestMessage.getSubject());

        // Execute the reaction
        return reactionExecutor.execute(area, context)
                .then(Mono.fromRunnable(() -> {
                    stateService.updateStateAfterSuccess(area, latestMessage, messageCount);

                    long execTime = System.currentTimeMillis() - startTime;
                    String logMessage = String.format("Sent notification for: %s (from: %s)",
                            latestMessage.getSubject(), latestMessage.getFrom());
                    logExecution(area, AreaExecutionLog.ExecutionStatus.SUCCESS,
                            messageCount, logMessage, execTime);

                    logger.info("Successfully processed area {} in {}ms", area.getId(), execTime);
                }))
                .thenReturn(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SUCCESS));
    }

    private void logExecution(Area area, AreaExecutionLog.ExecutionStatus status,
            Integer count, String message, long executionTimeMs) {
        try {
            AreaExecutionLog log = new AreaExecutionLog();
            log.setArea(area);
            log.setExecutedAt(Instant.now());
            log.setStatus(status);
            log.setUnreadCount(count);

            if (status == AreaExecutionLog.ExecutionStatus.SUCCESS) {
                log.setMessageSent(message);
            } else if (status == AreaExecutionLog.ExecutionStatus.FAILURE) {
                log.setErrorMessage(message);
            }

            log.setExecutionTimeMs(executionTimeMs);
            logRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log execution for area {}: {}", area.getId(), e.getMessage());
        }
    }

    static class ProcessingResult {
        final AreaExecutionLog.ExecutionStatus status;

        ProcessingResult(AreaExecutionLog.ExecutionStatus status) {
            this.status = status;
        }
    }
}
