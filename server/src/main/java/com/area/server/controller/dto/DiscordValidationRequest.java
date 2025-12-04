package com.area.server.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class DiscordValidationRequest {

    @NotBlank
    private String webhookUrl;

    private String message;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    @SuppressWarnings("unused")
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unused")
    public void setMessage(String message) {
        this.message = message;
    }
}
