package com.area.server.scheduler;

import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.service.TriggerStateService;
import com.area.server.service.integration.executor.ActionExecutor;
import com.area.server.service.integration.executor.ActionExecutorRegistry;
import com.area.server.service.integration.executor.ReactionExecutor;
import com.area.server.service.integration.executor.ReactionExecutorRegistry;
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
 * Dedicates scheduler for Timer-based triggers.
 * This allows timers to be polled at a different frequency than other services.
 */
@Service
@EnableScheduling
@ConditionalOnProperty(name = "timer.polling.enabled", havingValue = "true", matchIfMissing = true)
public class TimerPollingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TimerPollingScheduler.class);

    private final AreaRepository areaRepository;
    private final TriggerStateService stateService;
    private final AreaExecutionLogRepository logRepository;
    private final ActionExecutorRegistry actionExecutorRegistry;
    private final ReactionExecutorRegistry reactionExecutorRegistry;

    public TimerPollingScheduler(AreaRepository areaRepository,
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

    @Scheduled(fixedDelayString = "${timer.polling.interval:60000}", initialDelayString = "${timer.polling.initial-delay:10000}")
    public void pollActiveTimers() {
        // Only log at debug level to avoid spamming logs for frequent timer checks
        logger.debug("=== Starting TIMER polling cycle ===");
        long startTime = System.currentTimeMillis();

        List<Area> timerAreas = areaRepository.findActiveTimerAreas();
        logger.debug("Found {} active timer area(s) to process", timerAreas.size());

        if (timerAreas.isEmpty()) {
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        Flux.fromIterable(timerAreas)
                .flatMap(area -> processTimerArea(area)
                        .doOnSuccess(result -> {
                            switch (result.status) {
                                case SUCCESS -> successCount.incrementAndGet();
                                case FAILURE -> failureCount.incrementAndGet();
                                case SKIPPED -> skippedCount.incrementAndGet();
                            }
                        })
                        .onErrorResume(error -> {
                            logger.error("Unexpected error processing timer area {}", area.getId(), error);
                            failureCount.incrementAndGet();
                            return Mono.empty();
                        }),
                        10 // Allow higher concurrency for timers as they are usually lightweight
                )
                .collectList()
                .block(Duration.ofMinutes(1)); // Timers should be fast

        long duration = System.currentTimeMillis() - startTime;
        if (successCount.get() > 0 || failureCount.get() > 0) {
            logger.info("=== Timer cycle finished in {}ms - Success: {}, Failed: {}, Skipped: {} ===",
                    duration, successCount.get(), failureCount.get(), skippedCount.get());
        }
    }

    private Mono<ProcessingResult> processTimerArea(Area area) {
        long startTime = System.currentTimeMillis();

        if (stateService.shouldSkipDueToFailures(area)) {
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
        }

        String actionType = determineTimerActionType(area);
        String reactionType = determineReactionType(area);

        try {
            ActionExecutor actionExecutor = actionExecutorRegistry.getExecutorForAction(actionType);
            ReactionExecutor reactionExecutor = reactionExecutorRegistry.getExecutor(reactionType);

            return actionExecutor.getTriggerContext(area)
                    .flatMap(context -> {
                        // Check if timer triggered
                        Boolean triggered = context.getBoolean("triggered");
                        if (triggered != null && !triggered) {
                            stateService.updateCheckedTime(area);
                            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
                        }

                        String timerType = context.getString("timerType");
                        String time = context.getString("time");
                        logger.info("Timer triggered for area {} (Type: {}) at {}", area.getId(), timerType, time);

                        // Execute reaction
                        return reactionExecutor.execute(area, context)
                                .then(Mono.fromRunnable(() -> {
                                    stateService.updateStateAfterTimerSuccess(area);

                                    long execTime = System.currentTimeMillis() - startTime;
                                    String logMessage = String.format("Timer triggered: %s at %s", timerType, time);
                                    logExecution(area, AreaExecutionLog.ExecutionStatus.SUCCESS, logMessage, execTime);
                                }))
                                .thenReturn(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SUCCESS));
                    })
                    .onErrorResume(error -> {
                        logger.error("Failed to process timer area {}: {}", area.getId(), error.getMessage());
                        stateService.recordFailure(area, error.getMessage());
                        long execTime = System.currentTimeMillis() - startTime;
                        logExecution(area, AreaExecutionLog.ExecutionStatus.FAILURE, error.getMessage(), execTime);
                        return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.FAILURE));
                    });

        } catch (Exception e) {
            logger.error("Timer setup error for area {}: {}", area.getId(), e.getMessage());
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.FAILURE));
        }
    }

    private String determineTimerActionType(Area area) {
        if (area.getTimerConfig() != null && area.getTimerConfig().getTimerType() != null) {
            return "timer." + area.getTimerConfig().getTimerType();
        }
        return "timer.recurring";
    }

    private String determineReactionType(Area area) {
        if (area.getReactionType() != null) {
            if ("discord.send_webhook".equals(area.getReactionType())) {
                return "discord.send_message";
            }
            return area.getReactionType();
        }
        if (area.getReactionConnection() != null && area.getReactionConnection()
                .getType() == com.area.server.model.ServiceConnection.ServiceType.DISCORD) {
            return "discord.send_message";
        }
        throw new IllegalStateException("Unknown reaction type");
    }

    private void logExecution(Area area, AreaExecutionLog.ExecutionStatus status, String message,
            long executionTimeMs) {
        try {
            AreaExecutionLog log = new AreaExecutionLog();
            log.setArea(area);
            log.setExecutedAt(Instant.now());
            log.setStatus(status);
            if (status == AreaExecutionLog.ExecutionStatus.SUCCESS) {
                log.setMessageSent(message);
            } else {
                log.setErrorMessage(message);
            }
            log.setExecutionTimeMs(executionTimeMs);
            logRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log timer execution", e);
        }
    }

    static class ProcessingResult {
        final AreaExecutionLog.ExecutionStatus status;

        ProcessingResult(AreaExecutionLog.ExecutionStatus status) {
            this.status = status;
        }
    }
}
