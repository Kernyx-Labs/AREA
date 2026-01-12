package com.area.server.service.integration.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for reaction executors.
 * Automatically discovers and registers all ReactionExecutor beans.
 */
@Component
public class ReactionExecutorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ReactionExecutorRegistry.class);
    private final Map<String, ReactionExecutor> executors = new HashMap<>();

    public ReactionExecutorRegistry(List<ReactionExecutor> reactionExecutors) {
        for (ReactionExecutor executor : reactionExecutors) {
            register(executor);
        }
        logger.info("Registered {} reaction executors", executors.size());
    }

    private void register(ReactionExecutor executor) {
        String reactionType = executor.getReactionType();
        if (executors.containsKey(reactionType)) {
            logger.warn("Duplicate reaction executor for type: {}", reactionType);
        }
        executors.put(reactionType, executor);
        logger.debug("Registered reaction executor: {}", reactionType);
    }

    public ReactionExecutor getExecutor(String reactionType) {
        ReactionExecutor executor = executors.get(reactionType);
        if (executor == null) {
            throw new IllegalArgumentException("No reaction executor found for type: " + reactionType);
        }
        return executor;
    }

    public boolean hasExecutor(String reactionType) {
        return executors.containsKey(reactionType);
    }
}
