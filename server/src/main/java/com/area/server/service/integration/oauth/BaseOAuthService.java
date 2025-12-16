package com.area.server.service.integration.oauth;

import com.area.server.exception.ServiceIntegrationException;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.ServiceConnectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Base class for OAuth 2.0 service integrations.
 * Provides common OAuth functionality: authorization URL building, token exchange, and refresh.
 *
 * Subclasses only need to implement service-specific methods and provide OAuthConfig.
 */
public abstract class BaseOAuthService implements OAuthServiceIntegration {

    private static final Logger logger = LoggerFactory.getLogger(BaseOAuthService.class);

    protected final WebClient webClient;
    protected final ServiceConnectionRepository connectionRepository;
    protected final ObjectMapper objectMapper;

    protected BaseOAuthService(WebClient.Builder webClientBuilder,
                               ServiceConnectionRepository connectionRepository,
                               ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.connectionRepository = connectionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        OAuthConfig config = getOAuthConfig();

        String scopeParam = config.getScopes().stream()
            .map(scope -> URLEncoder.encode(scope, StandardCharsets.UTF_8))
            .collect(Collectors.joining("%20"));

        String redirectEncoded = URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8);

        return String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent",
            config.getAuthorizationUrl(),
            config.getClientId(),
            redirectEncoded,
            scopeParam
        );
    }

    @Override
    public Mono<ServiceConnection> exchangeAuthorizationCode(String code) {
        OAuthConfig config = getOAuthConfig();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", config.getClientId());
        formData.add("client_secret", config.getClientSecret());
        formData.add("redirect_uri", config.getRedirectUri());
        formData.add("grant_type", "authorization_code");

        logger.info("Exchanging authorization code for {} tokens", getName());

        return webClient.post()
            .uri(config.getTokenUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(responseBody -> {
                try {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);

                    String accessToken = jsonResponse.get("access_token").asText();
                    String refreshToken = jsonResponse.has("refresh_token")
                        ? jsonResponse.get("refresh_token").asText()
                        : null;
                    int expiresIn = jsonResponse.get("expires_in").asInt();

                    if (refreshToken == null) {
                        logger.warn("No refresh token received for {}. User may have already authorized.", getName());
                        return Mono.error(new ServiceIntegrationException(
                            getName(),
                            "No refresh token received. Please revoke app access and try again."
                        ));
                    }

                    ServiceConnection connection = new ServiceConnection();
                    connection.setType(getType());
                    connection.setAccessToken(accessToken);
                    connection.setRefreshToken(refreshToken);
                    connection.setExpiresInSeconds((long) expiresIn);
                    connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));

                    // Allow subclasses to add service-specific metadata
                    return enrichConnection(connection, accessToken)
                        .map(enriched -> {
                            ServiceConnection saved = connectionRepository.save(enriched);
                            logger.info("Successfully created {} connection with ID: {}", getName(), saved.getId());
                            return saved;
                        });

                } catch (Exception e) {
                    logger.error("Failed to parse OAuth token response for {}", getName(), e);
                    return Mono.error(new ServiceIntegrationException(
                        getName(),
                        "Failed to parse OAuth response",
                        e
                    ));
                }
            })
            .onErrorResume(error -> {
                if (error instanceof ServiceIntegrationException) {
                    return Mono.error(error);
                }
                logger.error("OAuth token exchange failed for {}", getName(), error);
                return Mono.error(new ServiceIntegrationException(
                    getName(),
                    "Token exchange failed: " + error.getMessage(),
                    error
                ));
            });
    }

    @Override
    public Mono<ServiceConnection> refreshAccessToken(ServiceConnection connection) {
        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            logger.warn("No refresh token available for {} connection {}", getName(), connection.getId());
            return Mono.error(new IllegalStateException(
                "No refresh token available for connection " + connection.getId()
            ));
        }

        OAuthConfig config = getOAuthConfig();

        if (config.getClientId() == null || config.getClientId().isBlank()
            || config.getClientSecret() == null || config.getClientSecret().isBlank()) {
            logger.warn("{} OAuth credentials not configured, skipping token refresh", getName());
            return Mono.just(connection);
        }

        logger.info("Refreshing access token for {} connection {}", getName(), connection.getId());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", config.getClientId());
        formData.add("client_secret", config.getClientSecret());
        formData.add("refresh_token", connection.getRefreshToken());

        return webClient.post()
            .uri(config.getTokenUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(String.class)
            .map(responseBody -> {
                try {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    String newAccessToken = jsonResponse.get("access_token").asText();
                    int expiresIn = jsonResponse.get("expires_in").asInt();

                    connection.setAccessToken(newAccessToken);
                    connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
                    connection.setLastRefreshAttempt(Instant.now());

                    logger.info("Successfully refreshed token for {} connection {}", getName(), connection.getId());
                    return connectionRepository.save(connection);

                } catch (Exception e) {
                    logger.error("Failed to parse token refresh response for {}", getName(), e);
                    throw new ServiceIntegrationException(
                        getName(),
                        "Failed to parse token refresh response",
                        e
                    );
                }
            })
            .onErrorResume(error -> {
                logger.error("Failed to refresh token for {} connection {}: {}",
                           getName(), connection.getId(), error.getMessage());
                connection.setLastRefreshAttempt(Instant.now());
                connectionRepository.save(connection);
                return Mono.error(new ServiceIntegrationException(
                    getName(),
                    "Token refresh failed: " + error.getMessage(),
                    error
                ));
            });
    }

    /**
     * Hook for subclasses to enrich the connection with service-specific metadata
     * (e.g., fetch user email, profile info, etc.)
     *
     * @param connection The connection to enrich
     * @param accessToken The access token to use for API calls
     * @return Mono of the enriched connection
     */
    protected Mono<ServiceConnection> enrichConnection(ServiceConnection connection, String accessToken) {
        // Default implementation: no enrichment
        return Mono.just(connection);
    }

    /**
     * Helper method to check if a connection needs token refresh
     */
    protected boolean needsRefresh(ServiceConnection connection) {
        return connection.needsRefresh();
    }

    /**
     * Refresh token if needed, otherwise return the connection as-is
     */
    public Mono<ServiceConnection> refreshTokenIfNeeded(ServiceConnection connection) {
        if (!needsRefresh(connection)) {
            return Mono.just(connection);
        }
        return refreshAccessToken(connection);
    }
}
