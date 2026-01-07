package com.area.server.service.integration.executor;

import com.area.server.dto.GmailMessage;
import com.area.server.model.Area;
import com.area.server.service.DiscordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executor for Discord "send_message" reaction.
 * Sends messages to Discord channels using the configured bot.
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
        return "discord.send_message";
    }

    @Override
    public Mono<Void> execute(Area area, TriggerContext context) {
        // Check if user has provided a custom message template
        String messageTemplate = area.getDiscordConfig().getMessageTemplate();
        boolean hasCustomTemplate = messageTemplate != null && !messageTemplate.isBlank();

        // If context contains a Gmail message and no custom template, send rich embed
        if (context.has("latestMessage") && !hasCustomTemplate) {
            GmailMessage message = (GmailMessage) context.get("latestMessage");
            logger.info("Sending Discord rich embed for area {} with email: {}",
                       area.getId(), message.getSubject());
            return discordService.sendRichEmbed(
                area.getReactionConnection(),
                area.getDiscordConfig(),
                message
            );
        }
        
        // Otherwise, send formatted text message using the template
        String messageContent = formatMessage(area, context);
        logger.info("Sending Discord message for area {}: {}", area.getId(), messageContent);
        return discordService.sendMessage(
            area.getReactionConnection(),
            area.getDiscordConfig(),
            messageContent
        );
    }

    private String formatMessage(Area area, TriggerContext context) {
        String template = area.getDiscordConfig().getMessageTemplate();
        
        if (template == null || template.isBlank()) {
            // Default message format
            Integer count = context.getInteger("messageCount");
            if (count != null && count > 0) {
                return String.format("You have %d new email(s) matching your AREA filters.", count);
            }
            return "AREA triggered successfully!";
        }
        
        // Replace placeholders in template
        String message = template;

        // If we have a Gmail message, extract its fields for template replacement
        if (context.has("latestMessage")) {
            GmailMessage gmailMessage = (GmailMessage) context.get("latestMessage");
            message = message.replace("{{subject}}", gmailMessage.getSubject() != null ? gmailMessage.getSubject() : "");
            message = message.replace("{{from}}", gmailMessage.getFrom() != null ? gmailMessage.getFrom() : "");
            message = message.replace("{{snippet}}", gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "");
            if (gmailMessage.getReceivedAt() != null) {
                message = message.replace("{{receivedAt}}", gmailMessage.getReceivedAt().toString());
            }
        }

        // Replace other context placeholders
        for (String key : context.getData().keySet()) {
            String placeholder = "{{" + key + "}}";
            Object value = context.get(key);
            if (value != null && !key.equals("latestMessage")) {
                message = message.replace(placeholder, value.toString());
            }
        }
        
        return message;
    }
}
