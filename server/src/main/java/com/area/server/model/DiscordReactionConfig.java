package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DiscordReactionConfig {

    @Column(name = "discord_channel_id", length = 64)
    private String channelId;

    @Column(name = "discord_channel_name")
    private String channelName;

    @Column(name = "discord_message_template", length = 1024)
    private String messageTemplate;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
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
