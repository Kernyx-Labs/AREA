package com.area.server.controller.dto;

import java.util.Map;

/**
 * Response DTO for trigger test endpoint.
 * Contains information about whether the trigger would fire
 * and what context data would be provided to reactions.
 */
public class TestTriggerResponse {

    private boolean triggered;
    private String message;
    private Map<String, Object> contextData;
    private Integer itemCount;  // Number of new items found (emails, issues, PRs, etc.)

    public TestTriggerResponse() {
    }

    public TestTriggerResponse(boolean triggered, String message, Map<String, Object> contextData, Integer itemCount) {
        this.triggered = triggered;
        this.message = message;
        this.contextData = contextData;
        this.itemCount = itemCount;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }

    public void setContextData(Map<String, Object> contextData) {
        this.contextData = contextData;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }
}
