package com.area.server.util;

import java.util.regex.Pattern;

public class DiscordWebhookValidator {

    private static final Pattern DISCORD_WEBHOOK_PATTERN = Pattern.compile(
        "^https://discord\\.com/api/webhooks/\\d+/[A-Za-z0-9_-]+$"
    );

    private static final Pattern DISCORD_CANARY_WEBHOOK_PATTERN = Pattern.compile(
        "^https://canary\\.discord\\.com/api/webhooks/\\d+/[A-Za-z0-9_-]+$"
    );

    private static final Pattern DISCORD_PTB_WEBHOOK_PATTERN = Pattern.compile(
        "^https://ptb\\.discord\\.com/api/webhooks/\\d+/[A-Za-z0-9_-]+$"
    );

    private DiscordWebhookValidator() {
        // Utility class
    }

    public static boolean isValidWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }

        return DISCORD_WEBHOOK_PATTERN.matcher(webhookUrl).matches() ||
               DISCORD_CANARY_WEBHOOK_PATTERN.matcher(webhookUrl).matches() ||
               DISCORD_PTB_WEBHOOK_PATTERN.matcher(webhookUrl).matches();
    }

    public static void validateWebhookUrl(String webhookUrl) {
        if (!isValidWebhookUrl(webhookUrl)) {
            throw new IllegalArgumentException(
                "Invalid Discord webhook URL. Expected format: https://discord.com/api/webhooks/{id}/{token}"
            );
        }
    }
}
