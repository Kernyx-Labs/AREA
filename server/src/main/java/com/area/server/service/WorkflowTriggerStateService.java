package com.area.server.service;

import com.area.server.model.Workflow;
import com.area.server.model.WorkflowTriggerState;
import com.area.server.repository.WorkflowTriggerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing workflow trigger state.
 * Handles state creation, updates, failure tracking, and circuit breaker logic.
 */
@Service
public class WorkflowTriggerStateService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowTriggerStateService.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final WorkflowTriggerStateRepository stateRepository;

    public WorkflowTriggerStateService(WorkflowTriggerStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    /**
     * Get existing state or create a new one for the workflow.
     */
    @Transactional
    public WorkflowTriggerState getOrCreateState(Workflow workflow) {
        return stateRepository.findByWorkflowId(workflow.getId())
            .orElseGet(() -> {
                WorkflowTriggerState state = new WorkflowTriggerState();
                state.setWorkflow(workflow);
                state.setLastUnreadCount(0);
                state.setConsecutiveFailures(0);
                logger.info("Created new trigger state for workflow {}", workflow.getId());
                return stateRepository.save(state);
            });
    }

    /**
     * Check if workflow should be skipped due to too many consecutive failures (circuit breaker).
     */
    public boolean shouldSkipDueToFailures(Workflow workflow) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        boolean skip = state.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES;

        if (skip) {
            logger.warn("Circuit breaker OPEN for workflow {} - {} consecutive failures",
                       workflow.getId(), state.getConsecutiveFailures());
        }

        return skip;
    }

    /**
     * Update state after successful execution.
     */
    @Transactional
    public void updateStateAfterSuccess(Workflow workflow, String lastProcessedItemId, Integer itemCount) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        state.setLastProcessedItemId(lastProcessedItemId);
        state.setLastUnreadCount(itemCount);
        state.setLastCheckedAt(Instant.now());
        state.setLastTriggeredAt(Instant.now());
        state.setConsecutiveFailures(0);
        state.setLastErrorMessage(null);
        stateRepository.save(state);

        logger.info("Updated trigger state for workflow {} - last item: {}, count: {}",
                   workflow.getId(), lastProcessedItemId, itemCount);
    }

    /**
     * Update last checked time without triggering.
     */
    @Transactional
    public void updateCheckedTime(Workflow workflow) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        state.setLastCheckedAt(Instant.now());
        stateRepository.save(state);
    }

    /**
     * Record a failure for circuit breaker tracking.
     */
    @Transactional
    public void recordFailure(Workflow workflow, String errorMessage) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
        state.setLastCheckedAt(Instant.now());
        state.setLastErrorMessage(
            errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage
        );
        stateRepository.save(state);

        logger.warn("Recorded failure for workflow {} (consecutive: {}): {}",
                   workflow.getId(), state.getConsecutiveFailures(), errorMessage);
    }

    /**
     * Reset failure count (e.g., when workflow is edited or manually reset).
     */
    @Transactional
    public void resetFailureCount(Workflow workflow) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        state.setConsecutiveFailures(0);
        state.setLastErrorMessage(null);
        stateRepository.save(state);
        logger.info("Reset failure count for workflow {}", workflow.getId());
    }

    /**
     * Update trigger state directly.
     * Used by action executors to persist state changes.
     */
    @Transactional
    public WorkflowTriggerState update(WorkflowTriggerState state) {
        return stateRepository.save(state);
    }

    /**
     * Get the last processed item ID for a workflow.
     */
    public String getLastProcessedItemId(Workflow workflow) {
        WorkflowTriggerState state = getOrCreateState(workflow);
        return state.getLastProcessedItemId();
    }
}
