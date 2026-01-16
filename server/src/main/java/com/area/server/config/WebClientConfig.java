package com.area.server.config;

import com.area.server.logging.ExternalApiLogger;
import com.area.server.logging.LoggingWebClientFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for WebClient beans with logging support.
 * Provides pre-configured WebClient builders for each external service.
 */
@Configuration
public class WebClientConfig {

    private final ExternalApiLogger apiLogger;

    public WebClientConfig(ExternalApiLogger apiLogger) {
        this.apiLogger = apiLogger;
    }

    /**
     * Creates a LoggingWebClientFilter bean.
     */
    @Bean
    public LoggingWebClientFilter loggingWebClientFilter() {
        return new LoggingWebClientFilter(apiLogger);
    }

    /**
     * Creates a pre-configured WebClient for Gmail API with logging.
     */
    @Bean
    public WebClient gmailWebClient(LoggingWebClientFilter loggingFilter) {
        return WebClient.builder()
            .baseUrl("https://www.googleapis.com")
            .filter(loggingFilter.logExchange("Gmail"))
            .build();
    }

    /**
     * Creates a pre-configured WebClient for Discord webhooks with logging.
     */
    @Bean
    public WebClient discordWebClient(LoggingWebClientFilter loggingFilter) {
        return WebClient.builder()
            .filter(loggingFilter.logExchange("Discord"))
            .build();
    }

    /**
     * Creates a pre-configured WebClient for Discord Bot API with logging.
     */
    @Bean
    public WebClient discordBotWebClient(LoggingWebClientFilter loggingFilter) {
        return WebClient.builder()
            .baseUrl("https://discord.com/api/v10")
            .filter(loggingFilter.logExchange("Discord-Bot"))
            .build();
    }

    /**
     * Creates a pre-configured WebClient for GitHub API with logging.
     */
    @Bean
    public WebClient githubWebClient(LoggingWebClientFilter loggingFilter) {
        return WebClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .filter(loggingFilter.logExchange("GitHub"))
            .build();
    }
}
