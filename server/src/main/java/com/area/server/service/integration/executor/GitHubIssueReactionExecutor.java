package com.area.server.service.integration.executor;

import com.area.server.model.Area;
import com.area.server.model.GitHubReactionConfig;
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

    private final GitHubService githubService;

    public GitHubIssueReactionExecutor(GitHubService githubService) {
        this.githubService = githubService;
    }

    @Override
    public String getReactionType() {
        return "github.create_issue";
    }

    @Override
    public Mono<Void> execute(Area area, TriggerContext context) {
        GitHubReactionConfig config = area.getGithubReactionConfig();

        if (config == null || !"create_issue".equals(config.getReactionType())) {
            logger.warn("GitHub create_issue reaction not configured for area {}", area.getId());
            return Mono.empty();
        }

        if (config.getRepositoryOwner() == null || config.getRepositoryName() == null) {
            logger.error("GitHub repository not configured for area {}", area.getId());
            return Mono.error(new IllegalStateException("GitHub repository not configured"));
        }

        if (config.getIssueTitle() == null || config.getIssueTitle().isBlank()) {
            logger.error("GitHub issue title not configured for area {}", area.getId());
            return Mono.error(new IllegalStateException("GitHub issue title not configured"));
        }

        logger.info("Executing GitHub create_issue reaction for area {}", area.getId());

        return githubService.createIssue(
                area.getReactionConnection(),
                config,
                context
            )
            .doOnSuccess(response ->
                logger.info("Successfully created issue #{} in {}/{} for area {}",
                          response.getNumber(),
                          config.getRepositoryOwner(),
                          config.getRepositoryName(),
                          area.getId()))
            .then()
            .onErrorResume(error -> {
                logger.error("Failed to create GitHub issue for area {}: {}",
                           area.getId(), error.getMessage());
                return Mono.error(error);
            });
    }
}
