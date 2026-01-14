package com.area.server.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit log for workflow executions.
 * Records every time a workflow is triggered, including success, failure, and skip events.
 */
@Entity
@Table(name = "workflow_execution_logs", indexes = {
    @Index(name = "idx_workflow_timestamp", columnList = "workflow_id,executed_at"),
    @Index(name = "idx_executed_at", columnList = "executed_at")
})
public class WorkflowExecutionLog {

    public enum ExecutionStatus {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExecutionStatus status;

    /**
     * Service that triggered the workflow (e.g., "gmail", "github")
     */
    @Column(name = "trigger_service", length = 64)
    private String triggerService;

    /**
     * Action type that triggered the workflow (e.g., "email_received", "issue_created")
     */
    @Column(name = "trigger_action", length = 64)
    private String triggerAction;

    /**
     * Number of items that triggered the workflow (e.g., number of new emails)
     */
    @Column(name = "trigger_count")
    private Integer triggerCount;

    /**
     * Number of actions/reactions successfully executed
     */
    @Column(name = "actions_executed")
    private Integer actionsExecuted;

    /**
     * Description of what was done (for successful executions)
     */
    @Column(name = "execution_details", length = 2048)
    private String executionDetails;

    /**
     * Error message (for failed executions)
     */
    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /**
     * Total execution time in milliseconds
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    public Long getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getTriggerService() {
        return triggerService;
    }

    public void setTriggerService(String triggerService) {
        this.triggerService = triggerService;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }

    public Integer getActionsExecuted() {
        return actionsExecuted;
    }

    public void setActionsExecuted(Integer actionsExecuted) {
        this.actionsExecuted = actionsExecuted;
    }

    public String getExecutionDetails() {
        return executionDetails;
    }

    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
