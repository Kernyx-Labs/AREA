package com.area.server.service.integration.oauth;

import com.area.server.model.ServiceConnection;
import com.area.server.service.integration.ServiceIntegration;
import reactor.core.publisher.Mono;

/**
 * Extended interface for service integrations that use OAuth 2.0 authentication.
 * Provides standard methods for OAuth flow: authorization, token exchange, and refresh.
 *
 * Implementations should extend BaseOAuthService for common OAuth functionality.
 */
public interface OAuthServiceIntegration extends ServiceIntegration {

    /**
     * Get the OAuth configuration for this service
     * @return OAuth configuration containing client credentials, URLs, and scopes
     */
    OAuthConfig getOAuthConfig();

    /**
     * Build the authorization URL for the OAuth flow
     * @param state Random state parameter for CSRF protection
     * @return Full authorization URL to redirect user to
     */
    String buildAuthorizationUrl(String state);

    /**
     * Exchange authorization code for access and refresh tokens
     * @param code Authorization code received from OAuth provider
     * @return ServiceConnection with access and refresh tokens
     */
    Mono<ServiceConnection> exchangeAuthorizationCode(String code);

    /**
     * Refresh an expired access token using the refresh token
     * @param connection Existing connection with refresh token
     * @return Updated ServiceConnection with new access token
     */
    Mono<ServiceConnection> refreshAccessToken(ServiceConnection connection);

    /**
     * Validate that a connection has valid OAuth tokens
     * @param connection Connection to validate
     * @return true if connection has required tokens, false otherwise
     */
    default boolean hasValidTokens(ServiceConnection connection) {
        return connection != null
            && connection.getAccessToken() != null
            && !connection.getAccessToken().isBlank();
    }
}
