package com.area.server.service.integration.executor;

import com.area.server.logging.ExternalApiLogger;
import com.area.server.model.AutomationEntity;
import com.area.server.model.GitHubReactionConfig;
import com.area.server.model.ServiceConnection;
import com.area.server.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executor for GitHub "create_issue" reaction.
 * Creates a new issue in a repository.
 */
@Component
public class GitHubIssueReactionExecutor implements ReactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubIssueReactionExecutor.class);
    private static final String EXECUTOR_NAME = "GitHubIssueReactionExecutor";

    private final GitHubService githubService;
    private final ExternalApiLogger apiLogger;

    public GitHubIssueReactionExecutor(GitHubService githubService, ExternalApiLogger apiLogger) {
        this.githubService = githubService;
        this.apiLogger = apiLogger;
    }

    @Override
    public String getReactionType() {
        return "github.create_issue";
    }

    @Override
    public Mono<Void> execute(AutomationEntity entity, TriggerContext context) {
        apiLogger.logOperation("GitHub-Executor", "EXECUTE_START",
                String.format("Entity ID: %d, Reaction type: create_issue, Context keys: %s",
                        entity.getId(), context.getData().keySet()));

        GitHubReactionConfig config = entity.getGithubReactionConfig();

        if (config == null || !"create_issue".equals(config.getReactionType())) {
            logger.warn("[{}] GitHub create_issue reaction not configured for entity {}", EXECUTOR_NAME,
                    entity.getId());
            return Mono.empty();
        }

        logger.debug("[{}] Config - Repo: {}/{}, Title template: '{}', Labels: {}",
                EXECUTOR_NAME,
                config.getRepositoryOwner(),
                config.getRepositoryName(),
                config.getIssueTitle(),
                config.getLabels());

        if (config.getRepositoryOwner() == null || config.getRepositoryName() == null) {
            logger.error("[{}] GitHub repository not configured for entity {}", EXECUTOR_NAME, entity.getId());
            return Mono.error(new IllegalStateException("GitHub repository not configured"));
        }

        if (config.getIssueTitle() == null || config.getIssueTitle().isBlank()) {
            logger.error("[{}] GitHub issue title not configured for entity {}", EXECUTOR_NAME, entity.getId());
            return Mono.error(new IllegalStateException("GitHub issue title not configured"));
        }

        ServiceConnection connection = entity.getReactionConnection();
        if (connection == null) {
            logger.error("[{}] No reaction connection found for entity {}", EXECUTOR_NAME, entity.getId());
            return Mono.error(new IllegalStateException("GitHub connection not configured"));
        }

        logger.debug("[{}] Using ServiceConnection ID: {}, Type: {}",
                EXECUTOR_NAME, connection.getId(), connection.getType());

        apiLogger.logOperation("GitHub-Executor", "CREATE_ISSUE",
                String.format("Entity: %d, Repo: %s/%s, Title: '%s'",
                        entity.getId(),
                        config.getRepositoryOwner(),
                        config.getRepositoryName(),
                        config.getIssueTitle()));

        return githubService.createIssue(connection, config, context)
                .doOnSuccess(response -> {
                    apiLogger.logOperation("GitHub-Executor", "CREATE_ISSUE_SUCCESS",
                            String.format("Created issue #%d in %s/%s for entity %d: %s",
                                    response.getNumber(),
                                    config.getRepositoryOwner(),
                                    config.getRepositoryName(),
                                    entity.getId(),
                                    response.getHtmlUrl()));
                })
                .then()
                .onErrorResume(error -> {
                    logger.error("[{}] Failed to create GitHub issue for entity {}: {}",
                            EXECUTOR_NAME, entity.getId(), error.getMessage());
                    return Mono.error(error);
                });
    }
}
