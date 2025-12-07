package com.area.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class AboutController {

    @GetMapping("/about.json")
    public Map<String, Object> about(HttpServletRequest request) {
        Map<String, Object> gmailService = Map.of(
                "name", "gmail",
                "actions", List.of(Map.of(
                        "name", "gmail.new_unread_email",
                        "description", "Triggered when a Gmail account receives a new unread email matching filters"
                )),
                "reactions", List.of()
        );

        Map<String, Object> discordService = Map.of(
                "name", "discord",
                "actions", List.of(),
                "reactions", List.of(Map.of(
                        "name", "discord.send_message",
                        "description", "Send a message to a Discord channel via webhook"
                ))
        );

        return Map.of(
                "client", Map.of("host", request.getRemoteAddr()),
                "server", Map.of(
                        "current_time", Instant.now().getEpochSecond(),
                        "services", List.of(gmailService, discordService)
                )
        );
    }
}

