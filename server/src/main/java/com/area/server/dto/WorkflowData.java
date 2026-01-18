package com.area.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO for parsing workflow JSON data.
 * Represents the complete workflow configuration including trigger and actions.
 */
public class WorkflowData {

    private TriggerConfig trigger;
    private List<ActionConfig> actions;

    public TriggerConfig getTrigger() {
        return trigger;
    }

    public void setTrigger(TriggerConfig trigger) {
        this.trigger = trigger;
    }

    public List<ActionConfig> getActions() {
        return actions;
    }

    public void setActions(List<ActionConfig> actions) {
        this.actions = actions;
    }

    /**
     * Configuration for a workflow trigger (the "IF" part).
     */
    public static class TriggerConfig {
        private String service;
        private String type;
        private Map<String, Object> config;

        @JsonProperty("connectionId")
        private Long connectionId;

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public Long getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(Long connectionId) {
            this.connectionId = connectionId;
        }

        /**
         * Get the full action type in the format "service.type"
         */
        /**
         * Get the full action type in the format "service.type"
         */
        public String getFullType() {
            String effectiveType = type;
            if (effectiveType == null && config != null && config.containsKey("actionType")) {
                effectiveType = (String) config.get("actionType");
            }
            if (effectiveType == null) {
                return service + ".null";
            }
            String prefix = service.toLowerCase() + ".";
            if (effectiveType.toLowerCase().startsWith(prefix)) {
                return effectiveType.toLowerCase();
            }
            return (prefix + effectiveType.toLowerCase());
        }
    }

    /**
     * Configuration for a workflow action/reaction (the "THEN" part).
     */
    public static class ActionConfig {
        private String service;
        private String type;
        private Map<String, Object> config;

        @JsonProperty("connectionId")
        private Long connectionId;

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public Long getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(Long connectionId) {
            this.connectionId = connectionId;
        }

        /**
         * Get the full reaction type in the format "service.type"
         */
        public String getFullType() {
            String effectiveType = type;
            if (effectiveType == null && config != null && config.containsKey("reactionType")) {
                effectiveType = (String) config.get("reactionType");
            }
            // Fallback for actions potentially storing type as actionType or type
            if (effectiveType == null && config != null && config.containsKey("type")) {
                effectiveType = (String) config.get("type");
            }

            if (effectiveType == null) {
                return service + ".null";
            }
            String prefix = service.toLowerCase() + ".";
            if (effectiveType.toLowerCase().startsWith(prefix)) {
                return effectiveType.toLowerCase();
            }
            return (prefix + effectiveType.toLowerCase());
        }
    }
}
