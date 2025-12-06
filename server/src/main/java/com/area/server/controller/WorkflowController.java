package com.area.server.controller;

import com.area.server.dto.CreateWorkflowRequest;
import com.area.server.model.Workflow;
import com.area.server.repository.WorkflowRepository;
import com.area.server.service.WorkflowExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService executionService;
    private final ObjectMapper objectMapper;

    public WorkflowController(WorkflowRepository workflowRepository, WorkflowExecutionService executionService, ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all workflows
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listWorkflows(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        try {
            List<Workflow> workflows = activeOnly
                    ? workflowRepository.findByActive(true)
                    : workflowRepository.findAll();

            List<Map<String, Object>> workflowList = workflows.stream()
                    .map(this::mapToResponse)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflows", workflowList
            ));
        } catch (Exception e) {
            logger.error("Failed to list workflows", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to load workflows"
            ));
        }
    }

    /**
     * Get a single workflow by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWorkflow(@PathVariable Long id) {
        try {
            Optional<Workflow> workflow = workflowRepository.findById(id);
            if (workflow.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflow", mapToResponse(workflow.get())
            ));
        } catch (Exception e) {
            logger.error("Failed to get workflow", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to load workflow"
            ));
        }
    }

    /**
     * Create a new workflow
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        try {
            Workflow workflow = new Workflow();
            workflow.setName(request.getName());
            workflow.setDescription("Trigger: " + request.getTrigger().getService() + " → Action: " + request.getAction().getService());
            workflow.setActive(true); // Start active by default

            // Store the workflow data as JSON
            Map<String, Object> workflowData = Map.of(
                    "trigger", request.getTrigger(),
                    "action", request.getAction()
            );
            String workflowDataJson = objectMapper.writeValueAsString(workflowData);
            workflow.setWorkflowData(workflowDataJson);

            Workflow saved = workflowRepository.save(workflow);

            logger.info("Created and activated workflow: {}", saved.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflow", mapToResponse(saved)
            ));
        } catch (Exception e) {
            logger.error("Failed to create workflow", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to create workflow",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Update an existing workflow
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, Object> request) {
        try {
            Optional<Workflow> existing = workflowRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            Workflow workflow = existing.get();
            if (request.containsKey("name")) {
                workflow.setName((String) request.get("name"));
            }
            if (request.containsKey("description")) {
                workflow.setDescription((String) request.get("description"));
            }
            if (request.containsKey("workflowData")) {
                String workflowData = objectMapper.writeValueAsString(request.get("workflowData"));
                workflow.setWorkflowData(workflowData);
            }

            Workflow saved = workflowRepository.save(workflow);

            logger.info("Updated workflow: {}", saved.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflow", mapToResponse(saved)
            ));
        } catch (Exception e) {
            logger.error("Failed to update workflow", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to update workflow",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Activate/deactivate a workflow
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateWorkflowStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Optional<Workflow> existing = workflowRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            Workflow workflow = existing.get();
            workflow.setActive(request.getOrDefault("active", false));
            Workflow saved = workflowRepository.save(workflow);

            logger.info("Updated workflow {} status to: {}", saved.getId(), saved.isActive());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflow", mapToResponse(saved)
            ));
        } catch (Exception e) {
            logger.error("Failed to update workflow status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to update workflow status"
            ));
        }
    }

    /**
     * Delete a workflow
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWorkflow(@PathVariable Long id) {
        try {
            if (!workflowRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            workflowRepository.deleteById(id);

            logger.info("Deleted workflow: {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Workflow deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to delete workflow", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to delete workflow"
            ));
        }
    }

    /**
     * Force execution of a workflow (for testing)
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(@PathVariable Long id) {
        try {
            Optional<Workflow> workflowOpt = workflowRepository.findById(id);
            if (workflowOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            Workflow workflow = workflowOpt.get();
            executionService.executeWorkflowManually(workflow);

            logger.info("Manually executed workflow: {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Workflow execution triggered successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to execute workflow", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to execute workflow: " + e.getMessage()
            ));
        }
    }

    /**
     * Get available triggers, actions, and reactions
     */
    @GetMapping("/available-nodes")
    public ResponseEntity<Map<String, Object>> getAvailableNodes() {
        Map<String, Object> triggers = Map.of(
                "manual", Map.of(
                        "name", "Manual Trigger",
                        "description", "Trigger manually by clicking a button",
                        "icon", "hand"
                ),
                "time", Map.of(
                        "name", "Time Trigger",
                        "description", "Trigger based on time interval",
                        "icon", "clock",
                        "config", List.of(
                                Map.of("name", "interval", "type", "number", "label", "Interval"),
                                Map.of("name", "unit", "type", "select", "label", "Unit",
                                        "options", List.of("seconds", "minutes", "hours", "days"))
                        )
                )
        );

        List<Map<String, Object>> actions = List.of(
                Map.of(
                        "id", "gmail.new_email",
                        "service", "gmail",
                        "name", "New Email Received",
                        "description", "Triggers when a new email arrives",
                        "config", List.of(
                                Map.of("name", "from", "type", "text", "label", "From Address (optional)"),
                                Map.of("name", "subject", "type", "text", "label", "Subject Contains (optional)")
                        )
                )
        );

        List<Map<String, Object>> reactions = List.of(
                Map.of(
                        "id", "discord.send_message",
                        "service", "discord",
                        "name", "Send Message",
                        "description", "Send a message to a Discord channel",
                        "config", List.of(
                                Map.of("name", "message", "type", "text", "label", "Message Template")
                        )
                )
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "triggers", triggers,
                "actions", actions,
                "reactions", reactions
        ));
    }

    /**
     * Get workflow execution statistics for the last 24 hours
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getWorkflowStats(@PathVariable Long id) {
        try {
            Optional<Workflow> workflow = workflowRepository.findById(id);
            if (workflow.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Workflow not found"
                ));
            }

            Map<String, Object> stats = executionService.getWorkflowStats(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            logger.error("Failed to get workflow stats", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to get stats",
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> mapToResponse(Workflow workflow) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", workflow.getId());
        response.put("name", workflow.getName());
        response.put("description", workflow.getDescription());
        response.put("active", workflow.isActive());
        response.put("createdAt", workflow.getCreatedAt().toString());
        response.put("updatedAt", workflow.getUpdatedAt().toString());

        // Parse workflow data
        if (workflow.getWorkflowData() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> workflowData = objectMapper.readValue(
                        workflow.getWorkflowData(),
                        Map.class
                );
                response.put("workflowData", workflowData);
            } catch (Exception e) {
                logger.warn("Failed to parse workflow data for workflow {}", workflow.getId());
                response.put("workflowData", null);
            }
        }

        return response;
    }
}
