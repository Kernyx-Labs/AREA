package com.area.server.service;

import com.area.server.dto.GmailApiResponse;
import com.area.server.dto.GmailMessage;
import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class EnhancedGmailService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedGmailService.class);

    private final WebClient gmailClient;

    public EnhancedGmailService(@Value("${google.api.base:https://www.googleapis.com}") String baseUrl,
                                WebClient.Builder builder) {
        this.gmailClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<List<GmailMessage>> fetchNewMessages(ServiceConnection connection,
                                                      GmailActionConfig config,
                                                      String afterMessageId) {
        String query = buildQuery(config);

        logger.debug("Fetching new Gmail messages with query: {} (after: {})", query, afterMessageId);

        return gmailClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/gmail/v1/users/me/messages")
                .queryParam("labelIds", "UNREAD")
                .queryParam("q", query)
                .queryParam("maxResults", 10)
                .build())
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(GmailApiResponse.MessageListResponse.class)
            .flatMapMany(response -> {
                List<GmailApiResponse.MessageRef> messages = response.getMessages();
                if (messages == null || messages.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(messages);
            })
            .filter(msg -> afterMessageId == null || msg.getId().compareTo(afterMessageId) > 0)
            .flatMap(msg -> fetchMessageDetails(connection, msg.getId()))
            .collectList()
            .doOnSuccess(messages -> logger.debug("Fetched {} new messages", messages.size()))
            .onErrorResume(error -> {
                logger.error("Error fetching Gmail messages: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    public Mono<GmailMessage> fetchMessageDetails(ServiceConnection connection, String messageId) {
        return gmailClient.get()
            .uri("/gmail/v1/users/me/messages/" + messageId)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(GmailApiResponse.MessageDetail.class)
            .map(this::parseMessage)
            .onErrorResume(error -> {
                logger.error("Error fetching message details for {}: {}", messageId, error.getMessage());
                return Mono.error(error);
            });
    }

    private GmailMessage parseMessage(GmailApiResponse.MessageDetail detail) {
        GmailMessage message = new GmailMessage();
        message.setId(detail.getId());
        message.setThreadId(detail.getThreadId());
        message.setSnippet(detail.getSnippet());

        // Parse received timestamp
        if (detail.getInternalDate() != null) {
            message.setReceivedAt(Instant.ofEpochMilli(detail.getInternalDate()));
        }

        // Extract headers
        if (detail.getPayload() != null && detail.getPayload().getHeaders() != null) {
            for (GmailApiResponse.Header header : detail.getPayload().getHeaders()) {
                switch (header.getName().toLowerCase()) {
                    case "subject":
                        message.setSubject(header.getValue());
                        break;
                    case "from":
                        message.setFrom(parseFromAddress(header.getValue()));
                        break;
                }
            }
        }

        // Default values if not found
        if (message.getSubject() == null) {
            message.setSubject("(No Subject)");
        }
        if (message.getFrom() == null) {
            message.setFrom("Unknown");
        }

        return message;
    }

    private String parseFromAddress(String fromHeader) {
        // Extract email from "Name <email@example.com>" format
        if (fromHeader == null) {
            return "Unknown";
        }

        int startBracket = fromHeader.indexOf('<');
        int endBracket = fromHeader.indexOf('>');

        if (startBracket != -1 && endBracket != -1) {
            return fromHeader.substring(startBracket + 1, endBracket);
        }

        return fromHeader.trim();
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
            .bodyToMono(GmailApiResponse.MessageListResponse.class)
            .map(response -> {
                Integer estimate = response.getResultSizeEstimate();
                return estimate != null ? estimate : 0;
            })
            .onErrorReturn(0);
    }
}
