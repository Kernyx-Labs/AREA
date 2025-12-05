package com.area.server.scheduler;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.service.EnhancedDiscordService;
import com.area.server.service.EnhancedGmailService;
import com.area.server.service.TokenRefreshService;
import com.area.server.service.TriggerStateService;
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
    private final EnhancedGmailService gmailService;
    private final EnhancedDiscordService discordService;
    private final TriggerStateService stateService;
    private final TokenRefreshService tokenRefreshService;
    private final AreaExecutionLogRepository logRepository;

    public AreaPollingScheduler(AreaRepository areaRepository,
                                EnhancedGmailService gmailService,
                                EnhancedDiscordService discordService,
                                TriggerStateService stateService,
                                TokenRefreshService tokenRefreshService,
                                AreaExecutionLogRepository logRepository) {
        this.areaRepository = areaRepository;
        this.gmailService = gmailService;
        this.discordService = discordService;
        this.stateService = stateService;
        this.tokenRefreshService = tokenRefreshService;
        this.logRepository = logRepository;
    }

    @Scheduled(fixedDelayString = "${area.polling.interval:60000}",
               initialDelayString = "${area.polling.initial-delay:30000}")
    public void pollActiveAreas() {
        logger.info("=== Starting AREA polling cycle ===");
        long startTime = System.currentTimeMillis();

        List<Area> activeAreas = areaRepository.findByActiveTrue();
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
                5
            )
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

        ServiceConnection actionConn = area.getActionConnection();

        if (actionConn == null || actionConn.getType() != ServiceConnection.ServiceType.GMAIL) {
            logger.error("Area {} has invalid action connection", area.getId());
            return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.FAILURE));
        }

        return tokenRefreshService.refreshTokenIfNeeded(actionConn)
            .flatMap(refreshedConn -> {
                AreaTriggerState state = stateService.getOrCreateState(area);

                return gmailService.fetchNewMessages(
                    refreshedConn,
                    area.getGmailConfig(),
                    state.getLastProcessedMessageId()
                );
            })
            .flatMap(newMessages -> {
                if (newMessages.isEmpty()) {
                    stateService.updateCheckedTime(area);
                    logger.debug("No new messages for area {}", area.getId());
                    return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
                }

                if (!stateService.shouldTrigger(area, newMessages)) {
                    stateService.updateCheckedTime(area);
                    logger.debug("Messages found but should not trigger for area {}", area.getId());
                    return Mono.just(new ProcessingResult(AreaExecutionLog.ExecutionStatus.SKIPPED));
                }

                GmailMessage latestMessage = newMessages.get(0);
                logger.info("Area {} triggered with {} new message(s). Latest: '{}'",
                           area.getId(), newMessages.size(), latestMessage.getSubject());

                return discordService.sendRichEmbed(area.getDiscordConfig(), latestMessage)
                    .then(Mono.fromRunnable(() -> {
                        stateService.updateStateAfterSuccess(area, latestMessage, newMessages.size());

                        long execTime = System.currentTimeMillis() - startTime;
                        String logMessage = String.format("Sent notification for: %s (from: %s)",
                                                         latestMessage.getSubject(), latestMessage.getFrom());
                        logExecution(area, AreaExecutionLog.ExecutionStatus.SUCCESS,
                                   newMessages.size(), logMessage, execTime);

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
