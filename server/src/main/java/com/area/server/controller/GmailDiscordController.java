package com.area.server.controller;

import com.area.server.controller.dto.*;
import com.area.server.model.Area;
import com.area.server.model.DiscordReactionConfig;
import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import com.area.server.service.AreaService;
import com.area.server.service.DiscordService;
import com.area.server.service.GmailService;
import com.area.server.service.ServiceConnectionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class GmailDiscordController {

    private static final Logger logger = LoggerFactory.getLogger(GmailDiscordController.class);

    private final GmailService gmailService;
    private final DiscordService discordService;
    private final ServiceConnectionService connectionService;
    private final AreaService areaService;

    public GmailDiscordController(GmailService gmailService,
                                  DiscordService discordService,
                                  ServiceConnectionService connectionService,
                                  AreaService areaService) {
        this.gmailService = gmailService;
        this.discordService = discordService;
        this.connectionService = connectionService;
        this.areaService = areaService;
    }

    @GetMapping("/services")
    public List<ServiceDescriptorResponse> listServices() {
        return List.of(
                new ServiceDescriptorResponse(
                        "gmail",
                        "Gmail",
                        "Google Mail service offering unread-email triggers",
                        List.of(new ActionSchemaResponse(
                                "gmail.new_unread_email",
                                "Gmail: New unread email",
                                "Triggered when Gmail receives an unread message matching the provided filters",
                                List.of(
                                        new FieldSchemaResponse("label", "Label", "string", false, "Specific Gmail label to monitor"),
                                        new FieldSchemaResponse("subjectContains", "Subject contains", "string", false, "Substring that must appear in the subject"),
                                        new FieldSchemaResponse("fromAddress", "From address", "string", false, "Restrict to messages coming from this address")
                                )
                        )),
                        List.of()
                ),
                new ServiceDescriptorResponse(
                        "discord",
                        "Discord",
                        "Discord webhook integration for posting messages",
                        List.of(),
                        List.of(new ReactionSchemaResponse(
                                "discord.send_message",
                                "Discord: Send message",
                                "Send a text message into a Discord channel via webhook",
                                List.of(
                                        new FieldSchemaResponse("webhookUrl", "Webhook URL", "string", true, "Discord channel webhook URL"),
                                        new FieldSchemaResponse("messageTemplate", "Message template", "text", false, "Template using {{unreadCount}} placeholder")
                                )
                        ))
                )
        );
    }

    @PostMapping("/actions/gmail/validate")
    public ResponseEntity<Map<String, Object>> validateGmail(@Valid @RequestBody GmailValidationRequest request) {
        try {
            ServiceConnection connection = connectionService.findById(request.getConnectionId());
            if (connection.getType() != ServiceConnection.ServiceType.GMAIL) {
                return ResponseEntity.badRequest().body(Map.of("error", "Connection is not of type GMAIL"));
            }

            // Validate that the connection has proper OAuth tokens
            if (connection.getAccessToken() == null || connection.getAccessToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Gmail connection",
                    "message", "Access token is missing. Please reconnect your Gmail account."
                ));
            }

            // Check if token looks like a client ID (they start with numbers and end with .apps.googleusercontent.com)
            if (connection.getAccessToken().contains("apps.googleusercontent.com")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Gmail connection",
                    "message", "Connection has OAuth client credentials instead of access tokens. Please delete this connection and create a new one through the OAuth flow at /api/services/gmail/auth-url"
                ));
            }

            GmailActionConfig config = new GmailActionConfig();
            config.setLabel(request.getLabel());
            config.setSubjectContains(request.getSubjectContains());
            config.setFromAddress(request.getFromAddress());

            int count = gmailService.fetchUnreadCount(connection, config).blockOptional().orElse(0);
            return ResponseEntity.ok(Map.of(
                    "connectionId", request.getConnectionId(),
                    "unreadCount", count,
                    "query", gmailService.buildQuery(config)
            ));
        } catch (Exception e) {
            logger.error("Failed to validate Gmail connection", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to validate Gmail connection",
                "message", e.getMessage(),
                "details", "Check server logs for more information"
            ));
        }
    }

    @PostMapping("/reactions/discord/validate")
    public ResponseEntity<Map<String, Object>> validateDiscord(@Valid @RequestBody DiscordValidationRequest request) {
        DiscordReactionConfig config = new DiscordReactionConfig();
        config.setWebhookUrl(request.getWebhookUrl());
        config.setMessageTemplate(request.getMessage());
        discordService.sendMessage(config, request.getMessage() != null ? request.getMessage() : "Test message from AREA").block();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/areas/{id}/trigger")
    public ResponseEntity<AreaTriggerResponse> triggerArea(@PathVariable Long id) {
        Area area = areaService.findById(id);
        ServiceConnection actionConnection = area.getActionConnection();
        int unread = gmailService.fetchUnreadCount(actionConnection, area.getGmailConfig()).blockOptional().orElse(0);
        String message = areaService.formatDiscordMessage(area, unread);
        discordService.sendMessage(area.getDiscordConfig(), message).block();
        return ResponseEntity.ok(new AreaTriggerResponse(id, unread, message));
    }
}
