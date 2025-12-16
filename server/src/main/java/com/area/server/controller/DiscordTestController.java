package com.area.server.controller;

import com.area.server.controller.dto.DiscordTestRequest;
import com.area.server.dto.response.ApiResponse;
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

    /**
     * Test Discord bot by sending a message
     * Uses GlobalExceptionHandler for error handling - no try-catch needed
     * Throws IllegalArgumentException if configuration is invalid
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testBot(@Valid @RequestBody DiscordTestRequest request) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalArgumentException("Discord bot token not configured. Please set DISCORD_CLIENT_SECRET in your .env file");
        }

        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("Discord channel ID not configured. Please set DISCORD_CHANNEL_ID in your .env file");
        }

        Map<String, String> payload = Map.of("content", request.getMessage());

        discordClient.post()
            .uri("/channels/{channelId}/messages", channelId)
            .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        Map<String, Object> data = Map.of("channelId", channelId);
        return ResponseEntity.ok(ApiResponse.success("Message sent to Discord successfully", data));
    }
}
