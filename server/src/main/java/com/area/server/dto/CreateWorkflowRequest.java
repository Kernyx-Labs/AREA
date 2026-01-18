package com.area.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class CreateWorkflowRequest {
    @NotBlank(message = "Workflow name is required")
    private String name;

    @NotNull(message = "Trigger configuration is required")
    @Valid
    private TriggerConfig trigger;

    // Support both single action and array of actions for flexibility
    @Valid
    private ActionConfig action;

    @Valid
    private List<ActionConfig> actions;

    public static class TriggerConfig {
        @NotBlank(message = "Trigger service is required")
        private String service;

        private String type;
        private Map<String, Object> config;
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
    }

    public static class ActionConfig {
        @NotBlank(message = "Action service is required")
        private String service;

        private String type;
        private Map<String, Object> config;
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
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TriggerConfig getTrigger() {
        return trigger;
    }

    public void setTrigger(TriggerConfig trigger) {
        this.trigger = trigger;
    }

    public ActionConfig getAction() {
        return action;
    }

    public void setAction(ActionConfig action) {
        this.action = action;
    }

    public List<ActionConfig> getActions() {
        return actions;
    }

    public void setActions(List<ActionConfig> actions) {
        this.actions = actions;
    }
}
