package com.area.server.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebClient exchange filter that logs all outgoing HTTP requests and their responses.
 * This filter intercepts WebClient calls and logs request details, response status,
 * and timing information.
 *
 * Usage: Add this filter to your WebClient configuration:
 * <pre>
 *     WebClient.builder()
 *         .filter(loggingWebClientFilter.logExchange("ServiceName"))
 *         .build();
 * </pre>
 */
@Component
public class LoggingWebClientFilter {

    private final ExternalApiLogger apiLogger;

    public LoggingWebClientFilter(ExternalApiLogger apiLogger) {
        this.apiLogger = apiLogger;
    }

    /**
     * Creates an exchange filter function that logs requests and responses for a specific service.
     *
     * @param serviceName The name of the service (e.g., "Gmail", "Discord", "GitHub")
     * @return An ExchangeFilterFunction that can be added to WebClient
     */
    public ExchangeFilterFunction logExchange(String serviceName) {
        return (request, next) -> {
            long startTime = System.currentTimeMillis();

            // Extract headers for logging (mask sensitive values)
            Map<String, String> headers = new HashMap<>();
            request.headers().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });

            // Log the request
            String requestId = apiLogger.logRequest(
                serviceName,
                request.method().name(),
                request.url().toString(),
                headers,
                null // Body is not easily accessible in filter
            );

            // Set trace ID in MDC for correlation
            String traceId = MDC.get("traceId");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString().substring(0, 8);
                MDC.put("traceId", traceId);
            }

            return next.exchange(request)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    apiLogger.logResponse(
                        serviceName,
                        requestId,
                        response.statusCode().value(),
                        duration,
                        null // Response body logged separately if needed
                    );
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    apiLogger.logError(serviceName, requestId, error, duration);
                });
        };
    }

    /**
     * Creates an exchange filter that logs with body content.
     * Use this when you need to log request/response bodies.
     * Note: This may have performance implications for large payloads.
     *
     * @param serviceName The name of the service
     * @return An ExchangeFilterFunction with body logging
     */
    public ExchangeFilterFunction logExchangeWithBody(String serviceName) {
        return (request, next) -> {
            long startTime = System.currentTimeMillis();

            Map<String, String> headers = new HashMap<>();
            request.headers().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });

            String requestId = apiLogger.logRequest(
                serviceName,
                request.method().name(),
                request.url().toString(),
                headers,
                null
            );

            return next.exchange(request)
                .flatMap(response -> {
                    long duration = System.currentTimeMillis() - startTime;

                    // For successful responses, we want to peek at the body without consuming it
                    return response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            apiLogger.logResponse(
                                serviceName,
                                requestId,
                                response.statusCode().value(),
                                duration,
                                body
                            );

                            // Rebuild the response with the body we consumed
                            return Mono.just(
                                ClientResponse.create(response.statusCode())
                                    .headers(h -> h.addAll(response.headers().asHttpHeaders()))
                                    .body(body)
                                    .build()
                            );
                        });
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    apiLogger.logError(serviceName, requestId, error, duration);
                });
        };
    }
}
