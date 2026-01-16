package com.area.server.service;

import com.area.server.dto.GmailApiResponse;
import com.area.server.dto.GmailMessage;
import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.GmailActionConfig;
import com.area.server.model.ServiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class GmailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
    private static final String SERVICE_NAME = "Gmail";

    private final WebClient gmailClient;
    private final ExternalApiLogger apiLogger;

    public GmailService(@Qualifier("gmailWebClient") WebClient gmailClient,
                        ExternalApiLogger apiLogger) {
        this.gmailClient = gmailClient;
        this.apiLogger = apiLogger;
    }

    public Mono<List<GmailMessage>> fetchNewMessages(ServiceConnection connection,
                                                      GmailActionConfig config,
                                                      String afterMessageId) {
        String query = buildQuery(config);

        apiLogger.logOperation(SERVICE_NAME, "FETCH_MESSAGES",
            String.format("Query: '%s', After: %s, Connection: %d",
                query, afterMessageId, connection.getId()));

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
            .doOnNext(response -> {
                int count = response.getMessages() != null ? response.getMessages().size() : 0;
                logger.debug("[Gmail] Message list response: {} messages found", count);
            })
            .flatMapMany(response -> {
                List<GmailApiResponse.MessageRef> messages = response.getMessages();
                if (messages == null || messages.isEmpty()) {
                    logger.debug("[Gmail] No messages in response");
                    return Flux.empty();
                }
                return Flux.fromIterable(messages);
            })
            .filter(msg -> afterMessageId == null || msg.getId().compareTo(afterMessageId) > 0)
            .flatMap(msg -> fetchMessageDetails(connection, msg.getId()))
            .collectList()
            .doOnSuccess(messages -> {
                apiLogger.logOperation(SERVICE_NAME, "FETCH_COMPLETE",
                    String.format("Retrieved %d new messages", messages.size()));
                if (!messages.isEmpty()) {
                    messages.forEach(m -> logger.debug("[Gmail] Message: id={}, from={}, subject={}",
                        m.getId(), m.getFrom(), m.getSubject()));
                }
            })
            .onErrorResume(error -> {
                logger.error("[Gmail] Error fetching messages: {}", error.getMessage());
                return Mono.error(error);
            });
    }

    public Mono<GmailMessage> fetchMessageDetails(ServiceConnection connection, String messageId) {
        logger.debug("[Gmail] Fetching message details for id={}", messageId);

        return gmailClient.get()
            .uri("/gmail/v1/users/me/messages/" + messageId)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(GmailApiResponse.MessageDetail.class)
            .doOnNext(detail -> logger.debug("[Gmail] Received message detail: threadId={}, snippet={}",
                detail.getThreadId(),
                detail.getSnippet() != null ? detail.getSnippet().substring(0, Math.min(50, detail.getSnippet().length())) + "..." : "null"))
            .map(this::parseMessage)
            .onErrorResume(error -> {
                logger.error("[Gmail] Error fetching message details for {}: {}", messageId, error.getMessage());
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
