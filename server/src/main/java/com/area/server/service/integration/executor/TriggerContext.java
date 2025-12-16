package com.area.server.service.integration.executor;

import java.util.HashMap;
import java.util.Map;

/**
 * Context data from an action trigger.
 * Contains information about what triggered the action (e.g., email details, message content)
 * that can be used by reactions.
 */
public class TriggerContext {
    private final Map<String, Object> data;

    public TriggerContext() {
        this.data = new HashMap<>();
    }

    public TriggerContext(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getInteger(String key) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }
}
