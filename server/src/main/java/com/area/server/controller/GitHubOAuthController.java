package com.area.server.controller;

import com.area.server.dto.GitHubApiResponse;
import com.area.server.dto.response.ApiResponse;
import com.area.server.model.ServiceConnection;
import com.area.server.model.User;
import com.area.server.security.CustomUserDetailsService;
import com.area.server.service.GitHubService;
import com.area.server.service.ServiceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Controller for GitHub OAuth 2.0 authentication flow.
 * Handles authorization URL generation and OAuth callback processing.
 */
@RestController
@RequestMapping("/api/services/github")
public class GitHubOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthController.class);

    /**
     * Inner class to store OAuth state with expiration
     */
    private static class OAuthState {
        private final Long userId;
        private final Instant createdAt;

        public OAuthState(Long userId) {
            this.userId = userId;
            this.createdAt = Instant.now();
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(600)); // 10 minutes
        }
    }

    // Server-side storage for OAuth states (token -> OAuthState)
    // Using ConcurrentHashMap for thread-safe access
    private final Map<String, OAuthState> stateStore = new ConcurrentHashMap<>();

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    private final ServiceConnectionService connectionService;
    private final GitHubService githubService;
    private final CustomUserDetailsService userDetailsService;
    private final WebClient webClient;

    public GitHubOAuthController(ServiceConnectionService connectionService,
                                  GitHubService githubService,
                                  CustomUserDetailsService userDetailsService,
                                  WebClient.Builder webClientBuilder) {
        this.connectionService = connectionService;
        this.githubService = githubService;
        this.userDetailsService = userDetailsService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Step 1: Get the authorization URL to start OAuth flow
     * The user will visit this URL in their browser to authorize the app
     */
    @GetMapping("/auth-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GitHub OAuth not configured. Please set GITHUB_CLIENT_ID in your .env file");
        }

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (not anonymous)
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getName())) {
            logger.error("Unauthenticated user tried to access GitHub OAuth URL");
            return ResponseEntity.status(401)
                .body(ApiResponse.error("Authentication required. Please log in again."));
        }

        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);

        // Request repo scope for full repository access (issues, PRs, code)
        String scope = URLEncoder.encode("repo", StandardCharsets.UTF_8);
        String redirectEncoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        // Pass user ID in state parameter to identify user in callback
        String state = generateState(user.getId());

        String authUrl = String.format(
            "https://github.com/login/oauth/authorize?" +
            "client_id=%s&" +
            "redirect_uri=%s&" +
            "scope=%s&" +
            "state=%s",
            clientId, redirectEncoded, scope, state
        );

        Map<String, String> data = Map.of(
            "authUrl", authUrl,
            "instructions", "Visit this URL in your browser to authorize GitHub access",
            "redirectUri", redirectUri
        );

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Step 2: OAuth callback endpoint
     * GitHub redirects here with the authorization code
     * We exchange it for an access token and create the service connection
     */
    @GetMapping(value = "/callback", produces = "text/html")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code,
                                                   @RequestParam(value = "state", required = false) String state,
                                                   @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            logger.error("GitHub OAuth error: {}", error);
            return ResponseEntity.badRequest().body(
                generateHtmlResponse("Error", "GitHub authorization failed: " + error, false)
            );
        }

        // Extract user ID from state parameter
        if (state == null || state.isBlank()) {
            logger.error("No state parameter received in callback");
            return ResponseEntity.badRequest().body(
                generateHtmlResponse("Error", "Invalid OAuth state. Please try connecting again.", false)
            );
        }

        Long userId;
        try {
            userId = extractUserIdFromState(state);
        } catch (Exception e) {
            logger.error("Failed to extract user ID from state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                generateHtmlResponse("Error", "Invalid OAuth state. Please try connecting again.", false)
            );
        }

        // Verify user exists
        User user;
        try {
            user = userDetailsService.loadUserEntityById(userId);
        } catch (Exception e) {
            logger.error("User not found with ID: {}", userId);
            return ResponseEntity.badRequest().body(
                generateHtmlResponse("Error", "User not found. Please try connecting again.", false)
            );
        }

        try {
            // Exchange authorization code for access token
            logger.info("Exchanging GitHub authorization code for access token for user {}", userId);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("code", code);
            formData.add("redirect_uri", redirectUri);

            GitHubApiResponse.TokenResponse tokenResponse = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GitHubApiResponse.TokenResponse.class)
                .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                logger.error("Failed to get access token from GitHub");
                return ResponseEntity.status(500).body(
                    generateHtmlResponse("Error", "Failed to obtain access token from GitHub", false)
                );
            }

            String accessToken = tokenResponse.getAccessToken();

            // Get user information
            GitHubApiResponse.UserResponse userInfo = githubService.getUserInfo(accessToken).block();

            if (userInfo == null) {
                logger.error("Failed to get user information from GitHub");
                return ResponseEntity.status(500).body(
                    generateHtmlResponse("Error", "Failed to retrieve user information from GitHub", false)
                );
            }

            // Create service connection
            ServiceConnection connection = new ServiceConnection();
            connection.setType(ServiceConnection.ServiceType.GITHUB);
            connection.setUser(user); // Associate connection with user
            connection.setAccessToken(accessToken);
            // GitHub tokens don't expire by default, so we don't set expiration
            connection.setRefreshToken(null);
            connection.setExpiresInSeconds(null);
            connection.setTokenExpiresAt(null);
            connection.setMetadata(String.format("{\"login\":\"%s\",\"name\":\"%s\"}",
                                                userInfo.getLogin(),
                                                userInfo.getName() != null ? userInfo.getName() : ""));

            ServiceConnection saved = connectionService.create(connection);

            logger.info("Successfully created GitHub connection with ID: {} for user: {} (GitHub: @{})",
                       saved.getId(), user.getEmail(), userInfo.getLogin());

            return ResponseEntity.ok(
                generateHtmlResponse("Success",
                                   "GitHub connected successfully for @" + userInfo.getLogin() +
                                   ". You can close this window.",
                                   true)
            );

        } catch (Exception e) {
            logger.error("Failed to complete GitHub OAuth flow", e);
            return ResponseEntity.status(500).body(
                generateHtmlResponse("Error", "Failed to complete authorization: " + e.getMessage(), false)
            );
        }
    }

    /**
     * Get status of GitHub OAuth configuration
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean configured = clientId != null && !clientId.isBlank()
                          && clientSecret != null && !clientSecret.isBlank();

        long githubConnectionCount = StreamSupport.stream(connectionService.list().spliterator(), false)
            .filter(c -> c.getType() == ServiceConnection.ServiceType.GITHUB)
            .count();

        Map<String, Object> data = Map.of(
            "configured", configured,
            "clientIdPresent", clientId != null && !clientId.isBlank(),
            "clientSecretPresent", clientSecret != null && !clientSecret.isBlank(),
            "redirectUri", redirectUri != null ? redirectUri : "not set",
            "existingConnections", githubConnectionCount
        );

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Generate secure state parameter for OAuth flow
     * Uses UUID stored server-side to prevent CSRF attacks and state prediction
     * State expires after 10 minutes
     */
    private String generateState(Long userId) {
        String token = UUID.randomUUID().toString();
        stateStore.put(token, new OAuthState(userId));

        // Clean up expired states (basic cleanup, not comprehensive)
        stateStore.entrySet().removeIf(entry -> entry.getValue().isExpired());

        logger.debug("Generated OAuth state token for user {}, active states: {}", userId, stateStore.size());
        return token;
    }

    /**
     * Extract user ID from state parameter and validate
     * State is one-time use and must not be expired
     */
    private Long extractUserIdFromState(String state) {
        OAuthState oauthState = stateStore.remove(state); // One-time use

        if (oauthState == null) {
            logger.warn("Invalid or already used OAuth state token");
            throw new IllegalArgumentException("Invalid or expired state");
        }

        if (oauthState.isExpired()) {
            logger.warn("Expired OAuth state token for user {}", oauthState.getUserId());
            throw new IllegalArgumentException("State expired");
        }

        return oauthState.getUserId();
    }

    /**
     * Generate HTML response for OAuth callback
     */
    private String generateHtmlResponse(String title, String message, boolean success) {
        String color = success ? "#10b981" : "#ef4444";
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + title + "</title>" +
            "<style>" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
            "display: flex; align-items: center; justify-content: center; min-height: 100vh; " +
            "margin: 0; background: #f9fafb; }" +
            ".container { text-align: center; padding: 2rem; background: white; " +
            "border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 400px; }" +
            ".icon { font-size: 3rem; margin-bottom: 1rem; }" +
            "h1 { color: " + color + "; margin: 0 0 1rem; }" +
            "p { color: #6b7280; line-height: 1.5; margin: 0 0 1.5rem; }" +
            ".close-btn { background: " + color + "; color: white; border: none; " +
            "padding: 0.75rem 1.5rem; border-radius: 8px; font-size: 1rem; cursor: pointer; }" +
            ".close-btn:hover { opacity: 0.9; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='icon'>" + (success ? "✓" : "✗") + "</div>" +
            "<h1>" + title + "</h1>" +
            "<p>" + message + "</p>" +
            "<button class='close-btn' onclick='window.close()'>Close Window</button>" +
            "</div>" +
            "<script>" +
            "setTimeout(() => { window.close(); }, 3000);" +
            "</script>" +
            "</body>" +
            "</html>";
    }
}
