package com.area.server.service;

import com.area.server.dto.GmailMessage;
import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.DiscordReactionConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiscordService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    private static final String SERVICE_NAME = "Discord";
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final WebClient discordClient;
    private final WebClient discordBotClient;
    private final ObjectMapper objectMapper;
    private final ExternalApiLogger apiLogger;

    public DiscordService(@Qualifier("discordWebClient") WebClient discordClient,
                          @Qualifier("discordBotWebClient") WebClient discordBotClient,
                          ObjectMapper objectMapper,
                          ExternalApiLogger apiLogger) {
        this.discordClient = discordClient;
        this.discordBotClient = discordBotClient;
        this.objectMapper = objectMapper;
        this.apiLogger = apiLogger;
    }

    public Mono<Void> sendMessage(DiscordReactionConfig config, String content) {
        return sendMessageWithRetry(config, content);
    }

    public Mono<Void> sendRichEmbed(DiscordReactionConfig config, GmailMessage email) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord webhook URL is required"));
        }

        Map<String, Object> embed = createEmbed(email);
        Map<String, Object> payload = Map.of(
            "username", "AREA Gmail Bot",
            "embeds", List.of(embed)
        );

        apiLogger.logOperation(SERVICE_NAME, "SEND_EMBED",
            String.format("Sending embed for email: subject='%s', from='%s'",
                email.getSubject(), email.getFrom()));

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            logger.debug("[Discord] Webhook payload: {}", payloadJson);
        } catch (JsonProcessingException e) {
            logger.debug("[Discord] Could not serialize payload for logging");
        }

        return discordClient.post()
            .uri(config.getWebhookUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> {
                    logger.error("[Discord] Webhook returned 4xx error - check webhook URL validity");
                    return Mono.error(new IllegalArgumentException("Invalid Discord webhook URL or configuration"));
                }
            )
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                .doBeforeRetry(signal ->
                    logger.warn("[Discord] Retrying webhook call (attempt {})", signal.totalRetries() + 1)
                ))
            .doOnSuccess(v -> apiLogger.logOperation(SERVICE_NAME, "SEND_SUCCESS",
                String.format("Notification sent for email: %s", email.getSubject())))
            .onErrorResume(error -> {
                logger.error("[Discord] Failed to send notification: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    private Mono<Void> sendMessageWithRetry(DiscordReactionConfig config, String content) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord webhook URL is required"));
        }

        Map<String, Object> payload = Map.of(
            "content", content,
            "username", "AREA Bot"
        );

        apiLogger.logOperation(SERVICE_NAME, "SEND_MESSAGE",
            String.format("Sending message via webhook, content length: %d chars", content.length()));
        logger.debug("[Discord] Message content: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);

        return discordClient.post()
            .uri(config.getWebhookUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> Mono.error(new IllegalArgumentException("Invalid Discord webhook"))
            )
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
            .doOnSuccess(v -> logger.debug("[Discord] Message sent successfully via webhook"))
            .onErrorResume(error -> {
                logger.error("[Discord] Failed to send message: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    private Map<String, Object> createEmbed(GmailMessage email) {
        Map<String, Object> embed = new HashMap<>();

        // Title and description
        embed.put("title", truncate("New Email: " + email.getSubject(), 256));
        embed.put("description", truncate(email.getSnippet(), 4096));

        // Color (blue)
        embed.put("color", 3447003);

        // Fields
        Map<String, Object> fromField = Map.of(
            "name", "From",
            "value", truncate(email.getFrom(), 1024),
            "inline", true
        );

        String receivedTime = email.getReceivedAt() != null
            ? TIME_FORMATTER.format(email.getReceivedAt())
            : "Unknown";

        Map<String, Object> timeField = Map.of(
            "name", "Received",
            "value", receivedTime,
            "inline", true
        );

        embed.put("fields", List.of(fromField, timeField));

        // Timestamp
        embed.put("timestamp", Instant.now().toString());

        // Footer
        Map<String, Object> footer = Map.of(
            "text", "AREA Gmail to Discord Integration"
        );
        embed.put("footer", footer);

        return embed;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public Mono<Void> sendBatchNotification(DiscordReactionConfig config,
                                           List<GmailMessage> emails,
                                           int totalCount) {
        if (emails == null || emails.isEmpty()) {
            return Mono.empty();
        }

        // Create a summary embed
        Map<String, Object> embed = new HashMap<>();
        embed.put("title", "New Gmail Messages");
        embed.put("description",
                 String.format("You have %d new unread email(s). Here are the most recent:", totalCount));
        embed.put("color", 3447003);

        // Add fields for each email (up to 5)
        List<Map<String, Object>> fields = emails.stream()
            .limit(5)
            .map(email -> Map.<String, Object>of(
                "name", truncate(email.getSubject(), 256),
                "value", String.format("From: %s", truncate(email.getFrom(), 200)),
                "inline", false
            ))
            .toList();

        embed.put("fields", fields);
        embed.put("timestamp", Instant.now().toString());

        Map<String, Object> payload = Map.of(
            "username", "AREA Gmail Bot",
            "embeds", List.of(embed)
        );

        return discordClient.post()
            .uri(config.getWebhookUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
            .doOnSuccess(v -> logger.info("Successfully sent batch notification for {} emails", emails.size()))
            .onErrorResume(error -> {
                logger.error("Failed to send batch notification: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    /**
     * Send a message to a Discord channel using the Bot API.
     * This method uses the bot token instead of webhooks.
     *
     * @param botToken The Discord bot token from ServiceConnection
     * @param channelId The Discord channel ID from ServiceConnection metadata
     * @param messageContent The message content to send
     * @return Mono<Void> indicating completion
     */
    public Mono<Void> sendMessageViaBotApi(String botToken, String channelId, String messageContent) {
        if (botToken == null || botToken.isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord bot token is required"));
        }
        if (channelId == null || channelId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord channel ID is required"));
        }
        if (messageContent == null || messageContent.isBlank()) {
            return Mono.error(new IllegalArgumentException("Message content is required"));
        }

        Map<String, Object> payload = Map.of("content", truncate(messageContent, 2000));

        apiLogger.logOperation(SERVICE_NAME, "BOT_SEND_MESSAGE",
            String.format("Sending to channel %s, content length: %d chars", channelId, messageContent.length()));
        logger.debug("[Discord-Bot] Message content: {}", messageContent.length() > 100 ? messageContent.substring(0, 100) + "..." : messageContent);

        return discordBotClient.post()
            .uri("/channels/{channelId}/messages", channelId)
            .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> {
                    logger.error("[Discord-Bot] API returned 4xx error for channel {}", channelId);
                    return Mono.error(new IllegalArgumentException(
                        "Invalid Discord bot token, channel ID, or insufficient permissions"));
                }
            )
            .onStatus(
                status -> status.is5xxServerError(),
                response -> {
                    logger.error("[Discord-Bot] API server error");
                    return Mono.error(new RuntimeException("Discord API is temporarily unavailable"));
                }
            )
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                .doBeforeRetry(signal ->
                    logger.warn("[Discord-Bot] Retrying API call (attempt {})", signal.totalRetries() + 1)
                ))
            .doOnSuccess(v -> apiLogger.logOperation(SERVICE_NAME, "BOT_SEND_SUCCESS",
                String.format("Message sent to channel %s", channelId)))
            .onErrorResume(error -> {
                logger.error("[Discord-Bot] Failed to send message: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    /**
     * Send a rich embed message to Discord using the Bot API.
     *
     * @param botToken The Discord bot token from ServiceConnection
     * @param channelId The Discord channel ID from ServiceConnection metadata
     * @param email The Gmail message to format as an embed
     * @return Mono<Void> indicating completion
     */
    public Mono<Void> sendRichEmbedViaBotApi(String botToken, String channelId, GmailMessage email) {
        if (botToken == null || botToken.isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord bot token is required"));
        }
        if (channelId == null || channelId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord channel ID is required"));
        }

        Map<String, Object> embed = createEmbed(email);
        Map<String, Object> payload = Map.of("embeds", List.of(embed));

        logger.debug("Sending Discord embed via Bot API for email: {}", email.getSubject());

        return discordBotClient.post()
            .uri("/channels/{channelId}/messages", channelId)
            .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> {
                    logger.error("Discord Bot API returned 4xx error");
                    return Mono.error(new IllegalArgumentException(
                        "Invalid Discord bot token, channel ID, or insufficient permissions"));
                }
            )
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                .doBeforeRetry(signal ->
                    logger.warn("Retrying Discord embed call (attempt {})", signal.totalRetries() + 1)
                ))
            .doOnSuccess(v -> logger.info("Successfully sent Discord embed via Bot API for email: {}", email.getSubject()))
            .onErrorResume(error -> {
                logger.error("Failed to send Discord embed via Bot API: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    /**
     * Replace placeholders in a message template with actual values.
     * Supports placeholders like {subject}, {from}, {snippet}, etc.
     *
     * @param template The message template with placeholders
     * @param placeholders Map of placeholder names to their values
     * @return The message with placeholders replaced
     */
    public String replacePlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isBlank()) {
            return "";
        }

        String result = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(0); // Full match including braces: {key}
            String key = matcher.group(1); // Just the key without braces

            String value = placeholders.getOrDefault(key, "");
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Extract channel ID from ServiceConnection metadata JSON.
     *
     * @param metadata The JSON metadata string from ServiceConnection
     * @return The channel ID, or null if not found
     */
    public String extractChannelId(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            logger.warn("ServiceConnection metadata is null or empty");
            return null;
        }

        try {
            Map<String, Object> metadataMap = objectMapper.readValue(metadata, Map.class);
            Object channelId = metadataMap.get("channelId");

            if (channelId != null) {
                return channelId.toString();
            }

            logger.warn("channelId not found in metadata");
            return null;
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse ServiceConnection metadata JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create a map of placeholders from a Gmail message for template replacement.
     *
     * @param email The Gmail message
     * @return Map of placeholder names to values
     */
    public Map<String, String> createPlaceholdersFromEmail(GmailMessage email) {
        Map<String, String> placeholders = new HashMap<>();

        if (email == null) {
            return placeholders;
        }

        placeholders.put("subject", email.getSubject() != null ? email.getSubject() : "");
        placeholders.put("from", email.getFrom() != null ? email.getFrom() : "");
        placeholders.put("snippet", email.getSnippet() != null ? email.getSnippet() : "");

        if (email.getReceivedAt() != null) {
            placeholders.put("receivedAt", TIME_FORMATTER.format(email.getReceivedAt()));
        } else {
            placeholders.put("receivedAt", "");
        }

        placeholders.put("messageId", email.getId() != null ? email.getId() : "");

        return placeholders;
    }
}
