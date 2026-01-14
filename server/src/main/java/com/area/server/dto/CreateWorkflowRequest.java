package com.area.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class CreateWorkflowRequest {
    private String name;

    @NotNull(message = "Trigger configuration is required")
    @Valid
    private TriggerConfig trigger;

    @NotNull(message = "Action configuration is required")
    @Valid
    private ActionConfig action;

    public static class TriggerConfig {
        private String service;
        private String type;
        private Map<String, Object> config;

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
    }

    public static class ActionConfig {
        private String service;
        private String type;
        private Map<String, Object> config;

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
}
