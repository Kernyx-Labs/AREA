package com.area.server.service;

import com.area.server.dto.GmailMessage;
import com.area.server.model.DiscordReactionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class EnhancedDiscordService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDiscordService.class);
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final WebClient discordClient;

    public EnhancedDiscordService(WebClient.Builder builder) {
        this.discordClient = builder.build();
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

        logger.debug("Sending Discord embed for email: {}", email.getSubject());

        return discordClient.post()
            .uri(config.getWebhookUrl())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> {
                    logger.error("Discord webhook returned 4xx error");
                    return Mono.error(new IllegalArgumentException("Invalid Discord webhook URL or configuration"));
                }
            )
            .bodyToMono(Void.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                .doBeforeRetry(signal ->
                    logger.warn("Retrying Discord webhook call (attempt {})", signal.totalRetries() + 1)
                ))
            .doOnSuccess(v -> logger.info("Successfully sent Discord notification for email: {}", email.getSubject()))
            .onErrorResume(error -> {
                logger.error("Failed to send Discord notification: {}", error.getMessage());
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
            .doOnSuccess(v -> logger.debug("Successfully sent Discord message"))
            .onErrorResume(error -> {
                logger.error("Failed to send Discord message: {}", error.getMessage());
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
}
