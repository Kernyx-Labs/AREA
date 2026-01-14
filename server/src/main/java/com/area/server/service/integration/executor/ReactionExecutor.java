package com.area.server.service.integration.executor;

import com.area.server.model.AutomationEntity;
import reactor.core.publisher.Mono;

/**
 * Interface for executing reactions (outputs).
 * Reaction executors perform actions in response to triggered events.
 *
 * Example: DiscordReactionExecutor sends a message to Discord
 */
public interface ReactionExecutor {

    /**
     * Get the unique identifier for this reaction type
     * Format: "service.reaction" (e.g., "discord.send_webhook")
     */
    String getReactionType();

    /**
     * Execute the reaction for the given entity with the provided trigger context.
     *
     * @param entity  The entity configuration containing reaction parameters
     * @param context Context data from the trigger
     * @return Mono<Void> completes when reaction is executed
     */
    Mono<Void> execute(AutomationEntity entity, TriggerContext context);
}
