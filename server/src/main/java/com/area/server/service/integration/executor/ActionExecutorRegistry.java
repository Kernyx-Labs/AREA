package com.area.server.service.integration.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for action executors.
 * Automatically discovers and registers all ActionExecutor beans.
 */
@Component
public class ActionExecutorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutorRegistry.class);
    private final Map<String, ActionExecutor> executors = new HashMap<>();

    public ActionExecutorRegistry(List<ActionExecutor> actionExecutors) {
        for (ActionExecutor executor : actionExecutors) {
            register(executor);
        }
        logger.info("Registered {} action executors", executors.size());
    }

    private void register(ActionExecutor executor) {
        String actionType = executor.getActionType();
        if (executors.containsKey(actionType)) {
            logger.warn("Duplicate action executor for type: {}", actionType);
        }
        executors.put(actionType, executor);
        logger.debug("Registered action executor: {}", actionType);
    }

    public ActionExecutor getExecutor(String actionType) {
        ActionExecutor executor = executors.get(actionType);
        if (executor == null) {
            throw new IllegalArgumentException("No action executor found for type: " + actionType);
        }
        return executor;
    }

    public boolean hasExecutor(String actionType) {
        return executors.containsKey(actionType);
    }
}
