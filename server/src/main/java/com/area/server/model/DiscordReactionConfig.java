package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DiscordReactionConfig {

    /**
     * @deprecated Use bot token from ServiceConnection instead.
     * This field is kept for backward compatibility but should not be used in new implementations.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @Column(name = "discord_webhook_url", length = 2048)
    private String webhookUrl;

    @Column(name = "discord_channel_name")
    private String channelName;

    @Column(name = "discord_message_template", length = 1024)
    private String messageTemplate;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @SuppressWarnings("unused")
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }
}
