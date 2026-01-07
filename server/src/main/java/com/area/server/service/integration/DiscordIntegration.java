package com.area.server.service.integration;

import com.area.server.model.ServiceConnection;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Discord integration service providing message sending reactions.
 * Implements the ServiceIntegration interface to enable auto-discovery.
 *
 * Discord uses bot token authentication and provides
 * reactions for sending messages and rich embeds to Discord channels.
 */
@Service
public class DiscordIntegration implements ServiceIntegration {

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.DISCORD;
    }

    @Override
    public String getName() {
        return "Discord";
    }

    @Override
    public String getDescription() {
        return "Send messages and notifications to Discord channels using a bot";
    }

    @Override
    public List<ActionDefinition> getActions() {
        // Discord only provides reactions, not actions (triggers)
        return Collections.emptyList();
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            new ReactionDefinition(
                "discord.send_message",
                "Send Message",
                "Send a simple text message to a Discord channel using your configured bot",
                List.of(
                    new FieldDefinition(
                        "channelId",
                        "Channel ID",
                        "string",
                        true,
                        "Discord channel ID where the message will be sent. Enable Developer Mode in Discord and right-click the channel to copy ID"
                    ),
                    new FieldDefinition(
                        "messageTemplate",
                        "Message Template",
                        "text",
                        false,
                        "Custom message template. Use placeholders like {subject}, {from}, {snippet}"
                    ),
                    new FieldDefinition(
                        "channelName",
                        "Channel Name",
                        "string",
                        false,
                        "Channel name for reference (optional, does not affect functionality)"
                    )
                )
            ),
            new ReactionDefinition(
                "discord.send_rich_embed",
                "Send Rich Embed",
                "Send a formatted rich embed message to Discord with email details",
                List.of(
                    new FieldDefinition(
                        "channelId",
                        "Channel ID",
                        "string",
                        true,
                        "Discord channel ID (required)"
                    ),
                    new FieldDefinition(
                        "channelName",
                        "Channel Name",
                        "string",
                        false,
                        "Channel name for reference (optional)"
                    )
                )
            ),
            new ReactionDefinition(
                "discord.send_batch_notification",
                "Send Batch Notification",
                "Send a summary notification when multiple emails are received",
                List.of(
                    new FieldDefinition(
                        "channelId",
                        "Channel ID",
                        "string",
                        true,
                        "Discord channel ID (required)"
                    ),
                    new FieldDefinition(
                        "maxEmailsToShow",
                        "Max Emails to Show",
                        "number",
                        false,
                        "Maximum number of emails to display in summary (default: 5)"
                    )
                )
            )
        );
    }

    @Override
    public boolean requiresAuthentication() {
        return true; // Discord now requires bot token authentication
    }
}
