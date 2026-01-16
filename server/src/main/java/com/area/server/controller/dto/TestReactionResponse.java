package com.area.server.controller.dto;

import java.util.Map;

/**
 * Response DTO for reaction test endpoint.
 * Contains information about the execution result and any relevant data
 * returned from the external service.
 */
public class TestReactionResponse {

    private boolean success;
    private String message;
    private Map<String, Object> resultData;  // e.g., Discord message ID, GitHub issue/PR number and URL

    public TestReactionResponse() {
    }

    public TestReactionResponse(boolean success, String message, Map<String, Object> resultData) {
        this.success = success;
        this.message = message;
        this.resultData = resultData;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getResultData() {
        return resultData;
    }

    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
    }
}
