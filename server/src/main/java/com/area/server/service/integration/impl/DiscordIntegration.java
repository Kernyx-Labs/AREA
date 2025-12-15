package com.area.server.service.integration.impl;

import com.area.server.service.integration.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Discord service integration providing webhook-based message reactions.
 * Discord primarily provides reactions (output actions) rather than triggers.
 */
@Service
public class DiscordIntegration implements ServiceIntegration {

    @Override
    public String getServiceId() {
        return "discord";
    }

    @Override
    public String getServiceName() {
        return "Discord";
    }

    @Override
    public String getServiceDescription() {
        return "Discord messaging platform integration via webhooks and bot API";
    }

    @Override
    public List<ActionDefinition> getActions() {
        // Discord currently doesn't provide action triggers in this implementation
        // Future: Could add message_received, reaction_added, etc. with bot integration
        return Collections.emptyList();
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            ReactionDefinition.builder()
                .name("send_webhook")
                .displayName("Send Webhook Message")
                .description("Send a message to a Discord channel via webhook")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("webhookUrl")
                        .type("string")
                        .description("Discord webhook URL")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("message")
                        .type("text")
                        .description("Message content to send")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("messageTemplate")
                        .type("text")
                        .description("Message template with {{placeholders}}")
                        .required(false)
                        .build()
                ))
                .build(),
            ReactionDefinition.builder()
                .name("send_bot_message")
                .displayName("Send Bot Message")
                .description("Send a message to a Discord channel using bot credentials")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("channelId")
                        .type("string")
                        .description("Discord channel ID")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("message")
                        .type("text")
                        .description("Message content to send")
                        .required(true)
                        .build()
                ))
                .build()
        );
    }

    @Override
    public boolean requiresOAuth() {
        return false;
    }

    @Override
    public boolean supportsWebhooks() {
        return true;
    }
}
