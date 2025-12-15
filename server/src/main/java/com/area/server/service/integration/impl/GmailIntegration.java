package com.area.server.service.integration.impl;

import com.area.server.model.ServiceConnection;
import com.area.server.repository.ServiceConnectionRepository;
import com.area.server.service.integration.*;
import com.area.server.service.integration.oauth.BaseOAuthService;
import com.area.server.service.integration.oauth.OAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Gmail service integration providing email-related actions and reactions.
 * Implements OAuth 2.0 authentication for accessing Gmail API.
 */
@Service
public class GmailIntegration extends BaseOAuthService {

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:8080/api/services/gmail/callback}")
    private String redirectUri;

    public GmailIntegration(WebClient.Builder webClientBuilder,
                           ServiceConnectionRepository connectionRepository,
                           ObjectMapper objectMapper) {
        super(webClientBuilder, connectionRepository, objectMapper);
    }

    @Override
    public String getServiceId() {
        return "gmail";
    }

    @Override
    public String getServiceName() {
        return "Gmail";
    }

    @Override
    public String getServiceDescription() {
        return "Google Mail service providing email actions and reactions";
    }

    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            ActionDefinition.builder()
                .name("email_received")
                .displayName("New Email Received")
                .description("Triggered when an email is received matching the specified filters")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("fromAddress")
                        .type("string")
                        .description("Filter by sender email address")
                        .required(false)
                        .build(),
                    FieldDefinition.builder()
                        .name("subjectContains")
                        .type("string")
                        .description("Filter by keywords in subject line")
                        .required(false)
                        .build(),
                    FieldDefinition.builder()
                        .name("label")
                        .type("string")
                        .description("Filter by Gmail label")
                        .required(false)
                        .build()
                ))
                .build()
        );
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            ReactionDefinition.builder()
                .name("send_email")
                .displayName("Send Email")
                .description("Send an email via Gmail")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("to")
                        .type("string")
                        .description("Recipient email address")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("subject")
                        .type("string")
                        .description("Email subject line")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("body")
                        .type("text")
                        .description("Email body content")
                        .required(true)
                        .build()
                ))
                .build()
        );
    }

    @Override
    public boolean requiresOAuth() {
        return true;
    }

    @Override
    public OAuthConfig getOAuthConfig() {
        return OAuthConfig.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUrl("https://oauth2.googleapis.com/token")
            .scopes(List.of(
                "https://www.googleapis.com/auth/gmail.readonly",
                "https://www.googleapis.com/auth/gmail.send"
            ))
            .build();
    }

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.GMAIL;
    }

    @Override
    public String getName() {
        return "Gmail";
    }
}
