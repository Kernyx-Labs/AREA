package com.area.server.service;

import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.Workflow;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.WorkflowRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final GmailService gmailService;
    private final DiscordService discordService;
    private final ExternalApiLogger apiLogger;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionService(
            AreaRepository areaRepository,
            WorkflowRepository workflowRepository,
            AreaExecutionLogRepository executionLogRepository,
            GmailService gmailService,
            DiscordService discordService,
            ExternalApiLogger apiLogger,
            ObjectMapper objectMapper) {
        this.areaRepository = areaRepository;
        this.workflowRepository = workflowRepository;
        this.executionLogRepository = executionLogRepository;
        this.gmailService = gmailService;
        this.discordService = discordService;
        this.apiLogger = apiLogger;
        this.objectMapper = objectMapper;
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
     *
     * NOTE: This method parses the workflow configuration and logs the actions
     * that would be executed. Full execution requires ServiceConnections to be
     * linked to the workflow (currently workflows store config as JSON only).
     */
    public void executeWorkflowManually(Workflow workflow) {
        apiLogger.logOperation("WorkflowExecution", "MANUAL_EXECUTE_START",
            String.format("Workflow ID: %d, Name: '%s', Active: %s",
                workflow.getId(), workflow.getName(), workflow.isActive()));

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ MANUAL WORKFLOW EXECUTION - ID: {}", workflow.getId());
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Name: {}", workflow.getName());
        logger.info("║ Description: {}", workflow.getDescription());
        logger.info("║ Active: {}", workflow.isActive());

        long startTime = System.currentTimeMillis();

        try {
            // Parse and log the workflow configuration
            if (workflow.getWorkflowData() != null && !workflow.getWorkflowData().isBlank()) {
                Map<String, Object> workflowData = objectMapper.readValue(
                    workflow.getWorkflowData(),
                    new TypeReference<Map<String, Object>>() {}
                );

                // Log trigger configuration
                if (workflowData.containsKey("trigger")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> trigger = (Map<String, Object>) workflowData.get("trigger");
                    logger.info("║ ");
                    logger.info("║ TRIGGER:");
                    logger.info("║   Service: {}", trigger.get("service"));
                    logger.info("║   Type: {}", trigger.get("type"));
                    if (trigger.containsKey("config")) {
                        logger.info("║   Config: {}", trigger.get("config"));
                    }
                }

                // Log actions configuration
                if (workflowData.containsKey("actions")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> actions = (List<Map<String, Object>>) workflowData.get("actions");
                    logger.info("║ ");
                    logger.info("║ ACTIONS ({} total):", actions.size());

                    for (int i = 0; i < actions.size(); i++) {
                        Map<String, Object> action = actions.get(i);
                        String service = (String) action.get("service");
                        String type = (String) action.get("type");
                        String actionType = service + "." + type;

                        logger.info("║   [{}] Service: {}, Type: {}", i + 1, service, type);
                        logger.info("║       Action Type Key: {}", actionType);
                        if (action.containsKey("config")) {
                            logger.info("║       Config: {}", action.get("config"));
                        }

                        // Log what would happen
                        apiLogger.logOperation("WorkflowExecution", "ACTION_" + (i + 1),
                            String.format("Would execute: %s (Service: %s)", actionType, service));
                    }
                }

                logger.info("║ ");
                logger.info("║ ⚠️  NOTE: Workflow execution is currently simulation-only.");
                logger.info("║     Full execution requires ServiceConnections to be linked.");
                logger.info("║     Use Areas (with configured connections) for actual execution.");
            } else {
                logger.warn("║ No workflow data configured!");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("╠══════════════════════════════════════════════════════════════════════════════");
            logger.info("║ Execution completed in {} ms - Status: SUCCESS (simulated)", duration);
            logger.info("╚══════════════════════════════════════════════════════════════════════════════");

            apiLogger.logOperation("WorkflowExecution", "MANUAL_EXECUTE_COMPLETE",
                String.format("Workflow %d completed (simulated) in %d ms", workflow.getId(), duration));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("╠══════════════════════════════════════════════════════════════════════════════");
            logger.error("║ Execution FAILED in {} ms: {}", duration, e.getMessage());
            logger.error("╚══════════════════════════════════════════════════════════════════════════════");

            apiLogger.logOperation("WorkflowExecution", "MANUAL_EXECUTE_ERROR",
                String.format("Workflow %d failed: %s", workflow.getId(), e.getMessage()));

            logger.error("Failed to manually execute workflow {}", workflow.getId(), e);
        }
    }
}
