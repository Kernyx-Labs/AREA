package com.area.server.service.integration.executor;

import com.area.server.dto.GmailMessage;
import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.AutomationEntity;
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
    private static final String EXECUTOR_NAME = "DiscordReactionExecutor";

    private final DiscordService discordService;
    private final ExternalApiLogger apiLogger;

    public DiscordReactionExecutor(DiscordService discordService, ExternalApiLogger apiLogger) {
        this.discordService = discordService;
        this.apiLogger = apiLogger;
    }

    @Override
    public String getReactionType() {
        return "discord.send_webhook";
    }

    @Override
    public Mono<Void> execute(AutomationEntity entity, TriggerContext context) {
        apiLogger.logOperation("Discord-Executor", "EXECUTE_START",
                String.format("Entity ID: %d, Context keys: %s", entity.getId(), context.getData().keySet()));

        // Get the Discord ServiceConnection (contains bot token and channel ID)
        ServiceConnection reactionConnection = entity.getReactionConnection();

        if (reactionConnection == null) {
            logger.error("[{}] No reaction connection found for entity {}", EXECUTOR_NAME, entity.getId());
            return Mono.error(new IllegalStateException("Discord connection not configured for this entity"));
        }

        if (reactionConnection.getType() != ServiceConnection.ServiceType.DISCORD) {
            logger.error("[{}] Reaction connection is not Discord for entity {} (found: {})",
                    EXECUTOR_NAME, entity.getId(), reactionConnection.getType());
            return Mono.error(new IllegalStateException("Invalid reaction connection type"));
        }

        logger.debug("[{}] Using ServiceConnection ID: {}, Type: {}",
                EXECUTOR_NAME, reactionConnection.getId(), reactionConnection.getType());

        // Extract bot token and channel ID
        String botToken = reactionConnection.getAccessToken();
        String channelId = discordService.extractChannelId(reactionConnection.getMetadata());

        if (botToken == null || botToken.isBlank()) {
            logger.error("[{}] Bot token is missing for entity {}", EXECUTOR_NAME, entity.getId());
            return Mono.error(new IllegalStateException("Discord bot token not found in connection"));
        }

        if (channelId == null || channelId.isBlank()) {
            logger.error("[{}] Channel ID is missing for entity {} (metadata: {})",
                    EXECUTOR_NAME, entity.getId(), reactionConnection.getMetadata());
            return Mono.error(new IllegalStateException("Discord channel ID not found in connection metadata"));
        }

        logger.debug("[{}] Target channel: {}, Token present: true", EXECUTOR_NAME, channelId);

        // Check if we should use custom template or rich embed
        String messageTemplate = entity.getDiscordConfig() != null
                ? entity.getDiscordConfig().getMessageTemplate()
                : null;

        // If context contains a Gmail message and no custom template, send rich embed
        if (context.has("latestMessage") && (messageTemplate == null || messageTemplate.isBlank())) {
            GmailMessage message = (GmailMessage) context.get("latestMessage");
            apiLogger.logOperation("Discord-Executor", "SEND_RICH_EMBED",
                    String.format("Entity: %d, Channel: %s, Email subject: '%s'",
                            entity.getId(), channelId, message.getSubject()));
            return discordService.sendRichEmbedViaBotApi(botToken, channelId, message)
                    .doOnSuccess(v -> apiLogger.logOperation("Discord-Executor", "SEND_SUCCESS",
                            String.format("Rich embed sent for entity %d", entity.getId())));
        }

        // Otherwise, send formatted text message using custom template
        String messageContent = formatMessage(entity, context);
        apiLogger.logOperation("Discord-Executor", "SEND_MESSAGE",
                String.format("Entity: %d, Channel: %s, Content length: %d",
                        entity.getId(), channelId, messageContent.length()));
        logger.debug("[{}] Message content: {}", EXECUTOR_NAME,
                messageContent.length() > 100 ? messageContent.substring(0, 100) + "..." : messageContent);

        return discordService.sendMessageViaBotApi(botToken, channelId, messageContent)
                .doOnSuccess(v -> apiLogger.logOperation("Discord-Executor", "SEND_SUCCESS",
                        String.format("Message sent for entity %d", entity.getId())));
    }

    private String formatMessage(AutomationEntity entity, TriggerContext context) {
        String template = entity.getDiscordConfig() != null
                ? entity.getDiscordConfig().getMessageTemplate()
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
