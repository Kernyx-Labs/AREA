package com.area.server.service;

import com.area.server.dto.GmailApiResponse;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.ServiceConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class TokenRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshService.class);

    private final WebClient googleOAuthClient;
    private final ServiceConnectionRepository repository;

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    public TokenRefreshService(WebClient.Builder builder,
                               ServiceConnectionRepository repository,
                               @Value("${google.oauth.token-url:https://oauth2.googleapis.com}") String tokenUrl) {
        this.googleOAuthClient = builder.baseUrl(tokenUrl).build();
        this.repository = repository;
    }

    public Mono<ServiceConnection> refreshTokenIfNeeded(ServiceConnection connection) {
        if (!connection.needsRefresh()) {
            return Mono.just(connection);
        }

        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            logger.warn("No refresh token available for connection {}", connection.getId());
            return Mono.error(new IllegalStateException(
                "No refresh token available for connection " + connection.getId()));
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            logger.warn("OAuth credentials not configured, skipping token refresh");
            return Mono.just(connection);
        }

        logger.info("Refreshing access token for connection {}", connection.getId());

        return googleOAuthClient.post()
            .uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(
                "grant_type=refresh_token" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&refresh_token=" + connection.getRefreshToken()
            )
            .retrieve()
            .bodyToMono(GmailApiResponse.TokenResponse.class)
            .map(response -> {
                connection.setAccessToken(response.getAccessToken());
                connection.setTokenExpiresAt(
                    Instant.now().plusSeconds(response.getExpiresIn())
                );
                connection.setLastRefreshAttempt(Instant.now());
                logger.info("Successfully refreshed token for connection {}", connection.getId());
                return repository.save(connection);
            })
            .onErrorResume(error -> {
                logger.error("Failed to refresh token for connection {}: {}",
                           connection.getId(), error.getMessage());
                connection.setLastRefreshAttempt(Instant.now());
                repository.save(connection);
                return Mono.error(new IllegalStateException(
                    "Token refresh failed: " + error.getMessage(), error));
            });
    }
}
