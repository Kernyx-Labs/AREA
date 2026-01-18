package com.area.server.service.integration.executor;

import com.area.server.model.AutomationEntity;
import reactor.core.publisher.Mono;

/**
 * Interface for executing action triggers.
 * Action executors check if a specific action's conditions are met
 * and determine if the AREA workflow should be triggered.
 *
 * Example: GmailActionExecutor checks for new emails matching filters
 */
public interface ActionExecutor {

    /**
     * Get the unique identifier for this action type
     * Format: "service.action" (e.g., "gmail.email_received")
     */
    String getActionType();

    /**
     * Check if the action conditions are met for the given AREA.
     * Returns true if the action should trigger the workflow.
     *
     * @param entity The entity configuration containing action parameters
     * @return Mono<Boolean> true if action triggered, false otherwise
     */
    Mono<Boolean> isTriggered(AutomationEntity entity);

    /**
     * Get context data from the trigger (e.g., new email details, message content)
     * This data can be used by reactions to customize their behavior.
     *
     * @param entity The entity configuration
     * @return Mono of trigger context data
     */
    Mono<TriggerContext> getTriggerContext(AutomationEntity entity);
}
