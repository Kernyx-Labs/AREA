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
    @GetMapping(value = "/callback", produces = "text/html")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code,
                                                               @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            logger.error("OAuth error: {}", error);
            return ResponseEntity.badRequest().body(
                generateHtmlResponse("Error", "OAuth authorization failed: " + error, false)
            );
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
                return ResponseEntity.badRequest().body(
                    generateHtmlResponse("Error", "No refresh token received. Please revoke app access at https://myaccount.google.com/permissions and try again", false)
                );
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

            return ResponseEntity.ok(
                generateHtmlResponse("Success", "Gmail connected successfully for " + userEmail + ". You can close this window.", true)
            );

        } catch (Exception e) {
            logger.error("Failed to complete OAuth flow", e);
            return ResponseEntity.status(500).body(
                generateHtmlResponse("Error", "Failed to exchange authorization code: " + e.getMessage(), false)
            );
        }
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
