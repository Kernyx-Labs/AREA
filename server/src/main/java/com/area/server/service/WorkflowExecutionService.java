package com.area.server.service;

import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.Workflow;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class WorkflowExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final AreaRepository areaRepository;
    private final WorkflowRepository workflowRepository;
    private final AreaExecutionLogRepository executionLogRepository;
    private final EnhancedGmailService gmailService;
    private final EnhancedDiscordService discordService;

    public WorkflowExecutionService(
            AreaRepository areaRepository,
            WorkflowRepository workflowRepository,
            AreaExecutionLogRepository executionLogRepository,
            EnhancedGmailService gmailService,
            EnhancedDiscordService discordService) {
        this.areaRepository = areaRepository;
        this.workflowRepository = workflowRepository;
        this.executionLogRepository = executionLogRepository;
        this.gmailService = gmailService;
        this.discordService = discordService;
    }

    /**
     * Scheduled task to check and execute active workflows
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void checkAndExecuteWorkflows() {
        logger.info("Checking active workflows for execution");

        try {
            List<Area> activeAreas = areaRepository.findByActiveTrue();

            for (Area area : activeAreas) {
                executeWorkflow(area);
            }

            logger.info("Completed workflow execution check. Processed {} active areas", activeAreas.size());
        } catch (Exception e) {
            logger.error("Error during scheduled workflow execution", e);
        }
    }

    private void executeWorkflow(Area area) {
        long startTime = System.currentTimeMillis();
        AreaExecutionLog log = new AreaExecutionLog();
        log.setArea(area);
        log.setExecutedAt(Instant.now());

        try {
            // For now, we'll implement a simple check-and-send pattern
            // This will be expanded based on actual trigger/action configuration

            // Placeholder for actual execution logic
            log.setStatus(AreaExecutionLog.ExecutionStatus.SUCCESS);
            log.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Successfully executed workflow for area {}", area.getId());
        } catch (Exception e) {
            log.setStatus(AreaExecutionLog.ExecutionStatus.FAILURE);
            log.setErrorMessage(e.getMessage());
            log.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            logger.error("Failed to execute workflow for area {}", area.getId(), e);
        } finally {
            executionLogRepository.save(log);
        }
    }

    /**
     * Get execution statistics for an area over the last 24 hours
     */
    public Map<String, Object> getWorkflowStats(Long areaId) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<AreaExecutionLog> logs = executionLogRepository.findByAreaIdAndExecutedAtAfter(areaId, since);

        long successCount = logs.stream()
                .filter(log -> log.getStatus() == AreaExecutionLog.ExecutionStatus.SUCCESS)
                .count();
        long failureCount = logs.stream()
                .filter(log -> log.getStatus() == AreaExecutionLog.ExecutionStatus.FAILURE)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", logs.size());
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("lastExecution", logs.isEmpty() ? "" : logs.get(0).getExecutedAt().toString());
        stats.put("recentLogs", logs.stream().limit(10).map(log -> {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("time", log.getExecutedAt().toString());
            logMap.put("status", log.getStatus().toString());
            logMap.put("executionTimeMs", log.getExecutionTimeMs() != null ? log.getExecutionTimeMs() : 0);
            logMap.put("error", log.getErrorMessage() != null ? log.getErrorMessage() : "");
            return logMap;
        }).toList());

        return stats;
    }

    /**
     * Manually execute a workflow (for testing)
     */
    public void executeWorkflowManually(Workflow workflow) {
        logger.info("Manually executing workflow: {} (ID: {})", workflow.getName(), workflow.getId());

        // Create a mock execution log for the workflow
        AreaExecutionLog log = new AreaExecutionLog();
        log.setExecutedAt(Instant.now());

        long startTime = System.currentTimeMillis();

        try {
            // For now, just log the manual execution
            // In the future, parse workflowData JSON and execute trigger/action
            logger.info("Workflow '{}' executed manually - Active: {}", workflow.getName(), workflow.isActive());

            log.setStatus(AreaExecutionLog.ExecutionStatus.SUCCESS);
            log.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            log.setMessageSent("Manual execution triggered for workflow: " + workflow.getName());

        } catch (Exception e) {
            log.setStatus(AreaExecutionLog.ExecutionStatus.FAILURE);
            log.setErrorMessage("Manual execution failed: " + e.getMessage());
            log.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            logger.error("Failed to manually execute workflow {}", workflow.getId(), e);
        }

        // Only save log if we have an associated Area
        // For pure workflows without Area, we skip logging for now
        logger.info("Manual execution completed for workflow {} - Status: {}",
                    workflow.getId(), log.getStatus());
    }
}
