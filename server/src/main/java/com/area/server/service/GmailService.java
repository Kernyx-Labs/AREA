package com.area.server.service;

import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GmailService {

    private final WebClient gmailClient;

    public GmailService(@Value("${google.api.base:https://www.googleapis.com}") String baseUrl,
                        WebClient.Builder builder) {
        this.gmailClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<Integer> fetchUnreadCount(ServiceConnection connection, GmailActionConfig config) {
        String query = buildQuery(config);
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
