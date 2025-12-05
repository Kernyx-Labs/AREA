package com.area.server.controller;

import com.area.server.model.ServiceConnection;
import com.area.server.service.ServiceConnectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/services/gmail")
public class GmailOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GmailOAuthController.class);

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private final ServiceConnectionService connectionService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GmailOAuthController(ServiceConnectionService connectionService,
                                WebClient.Builder webClientBuilder,
                                ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Step 1: Get the authorization URL to start OAuth flow
     * The user will visit this URL in their browser to authorize the app
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Gmail OAuth not configured",
                "message", "Please set GOOGLE_CLIENT_ID in your .env file"
            ));
        }

        String scope = URLEncoder.encode("https://www.googleapis.com/auth/gmail.readonly", StandardCharsets.UTF_8);
        String redirectEncoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        String authUrl = String.format(
            "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=%s&" +
            "redirect_uri=%s&" +
            "response_type=code&" +
            "scope=%s&" +
            "access_type=offline&" +
            "prompt=consent",
            clientId, redirectEncoded, scope
        );

        return ResponseEntity.ok(Map.of(
            "authUrl", authUrl,
            "instructions", "Visit this URL in your browser to authorize Gmail access",
            "redirectUri", redirectUri
        ));
    }

    /**
     * Step 2: OAuth callback endpoint
     * Google redirects here with the authorization code
     * We exchange it for tokens and create the service connection
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(@RequestParam("code") String code,
                                                               @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            logger.error("OAuth error: {}", error);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "OAuth authorization failed",
                "details", error
            ));
        }

        try {
            // Exchange authorization code for tokens
            logger.info("Exchanging authorization code for tokens");

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("code", code);
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("redirect_uri", redirectUri);
            formData.add("grant_type", "authorization_code");

            String responseBody = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token")
                ? jsonResponse.get("refresh_token").asText()
                : null;
            int expiresIn = jsonResponse.get("expires_in").asInt();

            if (refreshToken == null) {
                logger.warn("No refresh token received. User may have already authorized this app.");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "No refresh token received",
                    "message", "Please revoke app access at https://myaccount.google.com/permissions and try again"
                ));
            }

            // Get user email
            String userEmail = getUserEmail(accessToken);

            // Create service connection
            ServiceConnection connection = new ServiceConnection();
            connection.setType(ServiceConnection.ServiceType.GMAIL);
            connection.setAccessToken(accessToken);
            connection.setRefreshToken(refreshToken);
            connection.setExpiresInSeconds((long) expiresIn);
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setMetadata(String.format("{\"email\":\"%s\"}", userEmail));

            ServiceConnection saved = connectionService.create(connection);

            logger.info("Successfully created Gmail connection with ID: {}", saved.getId());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Gmail connection created successfully",
                "connectionId", saved.getId(),
                "email", userEmail,
                "type", saved.getType().toString(),
                "expiresIn", expiresIn
            ));

        } catch (Exception e) {
            logger.error("Failed to complete OAuth flow", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to exchange authorization code",
                "details", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to get user's email address
     */
    private String getUserEmail(String accessToken) {
        try {
            String response = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode userInfo = objectMapper.readTree(response);
            return userInfo.get("email").asText();
        } catch (Exception e) {
            logger.warn("Could not fetch user email: {}", e.getMessage());
            return "unknown@gmail.com";
        }
    }

    /**
     * Quick status check endpoint
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean configured = clientId != null && !clientId.isBlank()
                          && clientSecret != null && !clientSecret.isBlank();

        long gmailConnectionCount = StreamSupport.stream(connectionService.list().spliterator(), false)
            .filter(c -> c.getType() == ServiceConnection.ServiceType.GMAIL)
            .count();

        return ResponseEntity.ok(Map.of(
            "configured", configured,
            "clientIdPresent", clientId != null && !clientId.isBlank(),
            "clientSecretPresent", clientSecret != null && !clientSecret.isBlank(),
            "redirectUri", redirectUri != null ? redirectUri : "not set",
            "existingConnections", gmailConnectionCount
        ));
    }
}
