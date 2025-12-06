package com.area.server.controller;

import com.area.server.controller.dto.DiscordTestRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api/discord")
public class DiscordTestController {

    private final WebClient discordClient;

    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.bot.channel-id}")
    private String channelId;

    public DiscordTestController(WebClient.Builder webClientBuilder) {
        this.discordClient = webClientBuilder.baseUrl("https://discord.com/api/v10").build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testBot(@Valid @RequestBody DiscordTestRequest request) {
        if (botToken == null || botToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Discord bot token not configured",
                "message", "Please set DISCORD_CLIENT_SECRET in your .env file"
            ));
        }

        if (channelId == null || channelId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Discord channel ID not configured",
                "message", "Please set DISCORD_CHANNEL_ID in your .env file"
            ));
        }

        try {
            Map<String, String> payload = Map.of("content", request.getMessage());

            discordClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Message sent to Discord successfully",
                "channelId", channelId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send message to Discord",
                "details", e.getMessage()
            ));
        }
    }
}
