package com.area.server.controller;

import com.area.server.dto.response.ApiResponse;
import com.area.server.exception.ServiceIntegrationException;
import com.area.server.exception.ValidationException;
import com.area.server.model.ServiceConnection;
import com.area.server.service.ServiceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/services/discord")
public class DiscordConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(DiscordConnectionController.class);

    @Value("${discord.bot.token:}")
    private String botToken;

    private final ServiceConnectionService connectionService;
    private final WebClient discordClient;

    public DiscordConnectionController(ServiceConnectionService connectionService,
                                       WebClient.Builder webClientBuilder) {
        this.connectionService = connectionService;
        this.discordClient = webClientBuilder.baseUrl("https://discord.com/api/v10").build();
    }

    /**
     * Connect Discord by providing a bot token and channel ID
     */
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectDiscord(@RequestBody Map<String, String> request) {
        String userBotToken = request.get("botToken");
        String channelId = request.get("channelId");

        if (userBotToken == null || userBotToken.isBlank()) {
            throw new ValidationException("botToken", "Bot token is required");
        }

        if (channelId == null || channelId.isBlank()) {
            throw new ValidationException("channelId", "Channel ID is required");
        }

        // Validate the bot token by fetching bot user info
        Map<String, Object> botUser = discordClient.get()
            .uri("/users/@me")
            .header(HttpHeaders.AUTHORIZATION, "Bot " + userBotToken)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (botUser == null || !botUser.containsKey("id")) {
            throw new ServiceIntegrationException("Discord", 
                "Could not authenticate with Discord using the provided bot token");
        }

        String botUsername = (String) botUser.get("username");
        String botId = (String) botUser.get("id");

        // Validate channel access by trying to get channel info
        Map<String, Object> channel;
        try {
            channel = discordClient.get()
                .uri("/channels/{channelId}", channelId)
                .header(HttpHeaders.AUTHORIZATION, "Bot " + userBotToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (channel == null) {
                throw new ValidationException("channelId", 
                    "Could not access the specified channel. Make sure the bot is added to the server and has access to the channel.");
            }
        } catch (WebClientResponseException e) {
            logger.error("Failed to access Discord channel: {}", e.getMessage());
            String message = e.getStatusCode().value() == 404
                ? "Channel not found or bot not in server"
                : "The bot cannot access this channel. Make sure the bot has permissions to view the channel.";
            throw new ValidationException("channelId", message);
        }

        String channelName = (String) channel.get("name");

        // Create or update service connection
        ServiceConnection connection = new ServiceConnection();
        connection.setType(ServiceConnection.ServiceType.DISCORD);
        connection.setAccessToken(userBotToken);
        connection.setMetadata(String.format(
            "{\"channelId\":\"%s\",\"channelName\":\"%s\",\"botId\":\"%s\",\"botUsername\":\"%s\"}",
            channelId, channelName, botId, botUsername
        ));

        ServiceConnection saved = connectionService.create(connection);

        logger.info("Successfully created Discord connection with ID: {}", saved.getId());

        Map<String, Object> responseData = Map.of(
            "connectionId", saved.getId(),
            "botUsername", botUsername,
            "channelName", channelName,
            "channelId", channelId
        );

        return ResponseEntity.ok(ApiResponse.success("Discord connected successfully", responseData));
    }

    /**
     * Get connection info for setting up Discord
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionInfo() {
        Map<String, Object> data = Map.of(
            "instructions", Map.of(
                "step1", "Create a bot at https://discord.com/developers/applications",
                "step2", "Go to the 'Bot' section and copy your bot token",
                "step3", "Enable 'MESSAGE CONTENT INTENT' in the bot settings",
                "step4", "Invite the bot to your server using OAuth2 URL Generator",
                "step5", "Get the channel ID by enabling Developer Mode in Discord and right-clicking the channel",
                "step6", "Provide the bot token and channel ID here"
            ),
            "requiredScopes", new String[]{"bot"},
            "requiredPermissions", new String[]{"Send Messages", "Read Messages", "View Channel"}
        );

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Test Discord connection
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> testConnection(@RequestBody Map<String, String> request) {
        String testBotToken = request.get("botToken");
        String testChannelId = request.get("channelId");

        if (testBotToken == null || testBotToken.isBlank() || testChannelId == null || testChannelId.isBlank()) {
            throw new ValidationException("Both bot token and channel ID are required");
        }

        Map<String, String> payload = Map.of("content", "✅ Test message from AREA - Your Discord bot is working!");

        try {
            discordClient.post()
                .uri("/channels/{channelId}/messages", testChannelId)
                .header(HttpHeaders.AUTHORIZATION, "Bot " + testBotToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return ResponseEntity.ok(ApiResponse.success("Test message sent successfully!"));
        } catch (WebClientResponseException e) {
            logger.error("Discord test failed: {}", e.getMessage());
            String message = e.getStatusCode().value() == 404
                ? "Channel not found or bot not in server"
                : "Failed to send message. Check bot permissions.";
            throw new ServiceIntegrationException("Discord", message);
        }
    }
}
