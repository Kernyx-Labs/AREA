package com.area.server.service.integration.executor;

import com.area.server.model.AutomationEntity;
import com.area.server.model.GitHubReactionConfig;
import com.area.server.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executor for GitHub "create_pr" reaction.
 * Creates a new pull request in a repository.
 */
@Component
public class GitHubPullRequestReactionExecutor implements ReactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubPullRequestReactionExecutor.class);

    private final GitHubService githubService;

    public GitHubPullRequestReactionExecutor(GitHubService githubService) {
        this.githubService = githubService;
    }

    @Override
    public String getReactionType() {
        return "github.create_pr";
    }

    @Override
    public Mono<Void> execute(AutomationEntity entity, TriggerContext context) {
        GitHubReactionConfig config = entity.getGithubReactionConfig();

        if (config == null || !"create_pr".equals(config.getReactionType())) {
            logger.warn("GitHub create_pr reaction not configured for entity {}", entity.getId());
            return Mono.empty();
        }

        if (config.getRepositoryOwner() == null || config.getRepositoryName() == null) {
            logger.error("GitHub repository not configured for entity {}", entity.getId());
            return Mono.error(new IllegalStateException("GitHub repository not configured"));
        }

        if (config.getPrTitle() == null || config.getPrTitle().isBlank()) {
            logger.error("GitHub PR title not configured for entity {}", entity.getId());
            return Mono.error(new IllegalStateException("GitHub PR title not configured"));
        }

        if (config.getSourceBranch() == null || config.getSourceBranch().isBlank()) {
            logger.error("GitHub source branch not configured for entity {}", entity.getId());
            return Mono.error(new IllegalStateException("GitHub source branch not configured"));
        }

        if (config.getFilePath() == null || config.getFilePath().isBlank()) {
            logger.error("GitHub file path not configured for entity {}", entity.getId());
            return Mono.error(new IllegalStateException("GitHub file path not configured"));
        }

        logger.info("Executing GitHub create_pr reaction for entity {}", entity.getId());

        return githubService.createPullRequest(
                entity.getReactionConnection(),
                config,
                context)
                .doOnSuccess(response -> logger.info("Successfully created PR #{} in {}/{} for entity {}",
                        response.getNumber(),
                        config.getRepositoryOwner(),
                        config.getRepositoryName(),
                        entity.getId()))
                .then()
                .onErrorResume(error -> {
                    logger.error("Failed to create GitHub pull request for entity {}: {}",
                            entity.getId(), error.getMessage());
                    return Mono.error(error);
                });
    }
}
