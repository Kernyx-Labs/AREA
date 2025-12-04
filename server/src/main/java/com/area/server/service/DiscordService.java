package com.area.server.service;

import com.area.server.model.DiscordReactionConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DiscordService {

    private final WebClient discordClient;

    public DiscordService(WebClient.Builder builder) {
        this.discordClient = builder.build();
    }

    public Mono<Void> sendMessage(DiscordReactionConfig config, String content) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) {
            return Mono.error(new IllegalArgumentException("Discord webhook URL is required"));
        }
        return discordClient.post()
                .uri(config.getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"content\":\"" + content.replace("\"", "\\\"") + "\"}")
                .retrieve()
                .bodyToMono(Void.class);
    }
}
