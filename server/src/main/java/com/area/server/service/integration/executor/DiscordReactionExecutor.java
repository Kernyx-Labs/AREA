package com.area.server.service.integration.executor;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.model.ServiceConnection;
import com.area.server.service.DiscordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for Discord "send_webhook" reaction.
 * Sends messages to Discord channels via Discord Bot API using bot token.
 */
@Component
public class DiscordReactionExecutor implements ReactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DiscordReactionExecutor.class);

    private final DiscordService discordService;

    public DiscordReactionExecutor(DiscordService discordService) {
        this.discordService = discordService;
    }

    @Override
    public String getReactionType() {
        return "discord.send_webhook";
    }

    @Override
    public Mono<Void> execute(Area area, TriggerContext context) {
        // Get the Discord ServiceConnection (contains bot token and channel ID)
        ServiceConnection reactionConnection = area.getReactionConnection();

        if (reactionConnection == null) {
            logger.error("No reaction connection found for area {}", area.getId());
            return Mono.error(new IllegalStateException("Discord connection not configured for this area"));
        }

        if (reactionConnection.getType() != ServiceConnection.ServiceType.DISCORD) {
            logger.error("Reaction connection is not Discord for area {}", area.getId());
            return Mono.error(new IllegalStateException("Invalid reaction connection type"));
        }

        // Extract bot token and channel ID
        String botToken = reactionConnection.getAccessToken();
        String channelId = discordService.extractChannelId(reactionConnection.getMetadata());

        if (botToken == null || botToken.isBlank()) {
            logger.error("Bot token is missing for area {}", area.getId());
            return Mono.error(new IllegalStateException("Discord bot token not found in connection"));
        }

        if (channelId == null || channelId.isBlank()) {
            logger.error("Channel ID is missing for area {}", area.getId());
            return Mono.error(new IllegalStateException("Discord channel ID not found in connection metadata"));
        }

        // Check if we should use custom template or rich embed
        String messageTemplate = area.getDiscordConfig() != null
            ? area.getDiscordConfig().getMessageTemplate()
            : null;

        // If context contains a Gmail message and no custom template, send rich embed
        if (context.has("latestMessage") && (messageTemplate == null || messageTemplate.isBlank())) {
            GmailMessage message = (GmailMessage) context.get("latestMessage");
            logger.info("Sending Discord rich embed for area {} with email: {}",
                       area.getId(), message.getSubject());
            return discordService.sendRichEmbedViaBotApi(botToken, channelId, message);
        }

        // Otherwise, send formatted text message using custom template
        String messageContent = formatMessage(area, context);
        logger.info("Sending Discord message for area {} to channel {}: {}",
                   area.getId(), channelId, messageContent);
        return discordService.sendMessageViaBotApi(botToken, channelId, messageContent);
    }

    private String formatMessage(Area area, TriggerContext context) {
        String template = area.getDiscordConfig() != null
            ? area.getDiscordConfig().getMessageTemplate()
            : null;

        if (template == null || template.isBlank()) {
            // Default message format
            Integer count = context.getInteger("messageCount");
            if (count != null && count > 0) {
                return String.format("You have %d new email(s) matching your AREA filters.", count);
            }
            return "AREA triggered successfully!";
        }

        // Create placeholders map from context
        Map<String, String> placeholders = new HashMap<>();

        // If we have a Gmail message, extract placeholders from it
        if (context.has("latestMessage")) {
            GmailMessage message = (GmailMessage) context.get("latestMessage");
            placeholders = discordService.createPlaceholdersFromEmail(message);
        }

        // Add other context data as placeholders
        for (String key : context.getData().keySet()) {
            Object value = context.get(key);
            if (value != null && !placeholders.containsKey(key)) {
                placeholders.put(key, value.toString());
            }
        }

        // Replace placeholders in template
        return discordService.replacePlaceholders(template, placeholders);
    }
}
