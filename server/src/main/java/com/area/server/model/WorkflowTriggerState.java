package com.area.server.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks the execution state of a workflow's trigger.
 * Prevents duplicate processing and implements circuit breaker pattern.
 */
@Entity
@Table(name = "workflow_trigger_states", indexes = {
    @Index(name = "idx_workflow_id", columnList = "workflow_id")
})
public class WorkflowTriggerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "workflow_id", nullable = false, unique = true)
    private Workflow workflow;

    @Column(name = "last_unread_count")
    private Integer lastUnreadCount;

    /**
     * Stores the last processed item identifier to prevent reprocessing.
     * Format varies by service:
     * - Gmail: message ID
     * - GitHub Issues: "issue:123"
     * - GitHub PRs: "pr:456"
     */
    @Column(name = "last_processed_item_id", length = 256)
    private String lastProcessedItemId;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    public Long getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Integer getLastUnreadCount() {
        return lastUnreadCount;
    }

    public void setLastUnreadCount(Integer lastUnreadCount) {
        this.lastUnreadCount = lastUnreadCount;
    }

    public String getLastProcessedItemId() {
        return lastProcessedItemId;
    }

    public void setLastProcessedItemId(String lastProcessedItemId) {
        this.lastProcessedItemId = lastProcessedItemId;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(Instant lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
}
