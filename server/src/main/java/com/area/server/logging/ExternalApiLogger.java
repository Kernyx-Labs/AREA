package com.area.server.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Centralized logger for external API calls (Gmail, Discord, GitHub, etc.).
 * Provides structured logging for requests and responses to external services.
 *
 * Logs are written to both the main log and a dedicated external-api.log file.
 */
@Component
public class ExternalApiLogger {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiLogger.class);
    private static final int MAX_BODY_LENGTH = 2000;

    /**
     * Log an outgoing request to an external API.
     *
     * @param service   The service name (e.g., "Gmail", "Discord", "GitHub")
     * @param method    HTTP method (GET, POST, PUT, DELETE)
     * @param url       The target URL (sensitive parts like tokens should be masked)
     * @param headers   Request headers (tokens will be masked)
     * @param body      Request body (may be null for GET requests)
     * @return A unique request ID for correlation with the response
     */
    public String logRequest(String service, String method, String url,
                             Map<String, String> headers, Object body) {
        String requestId = generateRequestId();

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("╔══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ [%s] EXTERNAL API REQUEST - %s\n", service.toUpperCase(), requestId));
        logMessage.append("╠══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ Method: %s\n", method));
        logMessage.append(String.format("║ URL: %s\n", maskSensitiveUrl(url)));

        if (headers != null && !headers.isEmpty()) {
            logMessage.append("║ Headers:\n");
            headers.forEach((key, value) ->
                logMessage.append(String.format("║   %s: %s\n", key, maskSensitiveHeader(key, value))));
        }

        if (body != null) {
            String bodyStr = truncateBody(body.toString());
            logMessage.append(String.format("║ Body: %s\n", bodyStr));
        }

        logMessage.append("╚══════════════════════════════════════════════════════════════════════════════");

        logger.debug(logMessage.toString());
        return requestId;
    }

    /**
     * Log a response from an external API.
     *
     * @param service    The service name (e.g., "Gmail", "Discord", "GitHub")
     * @param requestId  The request ID from logRequest() for correlation
     * @param statusCode HTTP status code
     * @param duration   Request duration in milliseconds
     * @param body       Response body (may be null)
     */
    public void logResponse(String service, String requestId, int statusCode,
                           long duration, Object body) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("╔══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ [%s] EXTERNAL API RESPONSE - %s\n", service.toUpperCase(), requestId));
        logMessage.append("╠══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ Status: %d %s\n", statusCode, getStatusDescription(statusCode)));
        logMessage.append(String.format("║ Duration: %d ms\n", duration));

        if (body != null) {
            String bodyStr = truncateBody(body.toString());
            logMessage.append(String.format("║ Response Body: %s\n", bodyStr));
        }

        logMessage.append("╚══════════════════════════════════════════════════════════════════════════════");

        if (statusCode >= 400) {
            logger.warn(logMessage.toString());
        } else {
            logger.debug(logMessage.toString());
        }
    }

    /**
     * Log an error during an external API call.
     *
     * @param service   The service name
     * @param requestId The request ID for correlation
     * @param error     The exception that occurred
     * @param duration  Duration until failure in milliseconds
     */
    public void logError(String service, String requestId, Throwable error, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n");
        logMessage.append("╔══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ [%s] EXTERNAL API ERROR - %s\n", service.toUpperCase(), requestId));
        logMessage.append("╠══════════════════════════════════════════════════════════════════════════════\n");
        logMessage.append(String.format("║ Duration: %d ms\n", duration));
        logMessage.append(String.format("║ Error Type: %s\n", error.getClass().getSimpleName()));
        logMessage.append(String.format("║ Error Message: %s\n", error.getMessage()));
        logMessage.append("╚══════════════════════════════════════════════════════════════════════════════");

        logger.error(logMessage.toString(), error);
    }

    /**
     * Log a simple operation message for a service.
     *
     * @param service   The service name
     * @param operation The operation being performed
     * @param details   Additional details about the operation
     */
    public void logOperation(String service, String operation, String details) {
        logger.info("[{}] {} - {}", service.toUpperCase(), operation, details);
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String maskSensitiveUrl(String url) {
        if (url == null) return "null";
        // Mask webhook URLs (keep only the domain)
        if (url.contains("discord.com/api/webhooks/")) {
            return url.replaceAll("(webhooks/)[^/]+/[^/]+", "$1****/****");
        }
        return url;
    }

    private String maskSensitiveHeader(String key, String value) {
        if (value == null) return "null";
        String keyLower = key.toLowerCase();
        if (keyLower.contains("authorization") || keyLower.contains("token") ||
            keyLower.contains("secret") || keyLower.contains("api-key")) {
            if (value.length() > 10) {
                return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
            }
            return "****";
        }
        return value;
    }

    private String truncateBody(String body) {
        if (body == null) return "null";
        if (body.length() <= MAX_BODY_LENGTH) return body;
        return body.substring(0, MAX_BODY_LENGTH) + "... [truncated, total: " + body.length() + " chars]";
    }

    private String getStatusDescription(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}
