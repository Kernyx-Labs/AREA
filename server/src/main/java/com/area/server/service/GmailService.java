package com.area.server.service;

import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GmailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);

    private final WebClient gmailClient;

    public GmailService(@Value("${google.api.base:https://www.googleapis.com}") String baseUrl,
                        WebClient.Builder builder) {
        this.gmailClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<Integer> fetchUnreadCount(ServiceConnection connection, GmailActionConfig config) {
        String query = buildQuery(config);
        logger.debug("Fetching unread count with query: {}", query);

        return gmailClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/gmail/v1/users/me/messages")
                        .queryParam("labelIds", "UNREAD")
                        .queryParam("q", query)
                        .build())
                .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    Object estimate = body.get("resultSizeEstimate");
                    if (estimate instanceof Number number) {
                        return number.intValue();
                    }
                    return 0;
                })
                .doOnError(WebClientResponseException.class, e -> {
                    logger.error("Gmail API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                })
                .onErrorResume(e -> {
                    logger.error("Failed to fetch Gmail unread count", e);
                    return Mono.just(0);
                });
    }

    public String buildQuery(GmailActionConfig config) {
        if (config == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (config.getLabel() != null && !config.getLabel().isBlank()) {
            builder.append("label:").append(config.getLabel()).append(' ');
        }
        if (config.getSubjectContains() != null && !config.getSubjectContains().isBlank()) {
            builder.append("subject:\"").append(config.getSubjectContains()).append("\" ");
        }
        if (config.getFromAddress() != null && !config.getFromAddress().isBlank()) {
            builder.append("from:").append(config.getFromAddress());
        }
        return builder.toString().trim();
    }
}
