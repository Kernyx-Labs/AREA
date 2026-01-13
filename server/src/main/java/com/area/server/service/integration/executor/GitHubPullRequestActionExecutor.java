package com.area.server.service.integration.executor;

import com.area.server.dto.GitHubPullRequest;
import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.GitHubActionConfig;
import com.area.server.service.GitHubService;
import com.area.server.service.TriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Executor for GitHub "pr_created" action.
 * Checks for new pull requests created in a repository.
 */
@Component
public class GitHubPullRequestActionExecutor implements ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubPullRequestActionExecutor.class);

    private final GitHubService githubService;
    private final TriggerStateService stateService;

    public GitHubPullRequestActionExecutor(GitHubService githubService,
                                            TriggerStateService stateService) {
        this.githubService = githubService;
        this.stateService = stateService;
    }

    @Override
    public String getActionType() {
        return "github.pr_created";
    }

    @Override
    public Mono<Boolean> isTriggered(Area area) {
        return getTriggerContext(area)
            .map(context -> context.has("newPullRequests") &&
                           context.getInteger("prCount") != null &&
                           context.getInteger("prCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(Area area) {
        GitHubActionConfig config = area.getGithubActionConfig();

        if (config == null || !"pr_created".equals(config.getActionType())) {
            return Mono.just(new TriggerContext());
        }

        AreaTriggerState state = stateService.getOrCreateState(area);
        Long afterPrNumber = parsePrNumber(state.getLastProcessedMessageId());

        return githubService.fetchNewPullRequests(
                area.getActionConnection(),
                config,
                afterPrNumber
            )
            .map(newPullRequests -> {
                TriggerContext context = new TriggerContext();
                context.put("newPullRequests", newPullRequests);
                context.put("prCount", newPullRequests.size());

                if (!newPullRequests.isEmpty()) {
                    GitHubPullRequest latestPr = newPullRequests.get(0);
                    context.put("latestPullRequest", latestPr);
                    context.put("prNumber", latestPr.getNumber());
                    context.put("prTitle", latestPr.getTitle());
                    context.put("prBody", latestPr.getBody());
                    context.put("prUrl", latestPr.getHtmlUrl());

                    if (latestPr.getUser() != null) {
                        context.put("prAuthor", latestPr.getUser().getLogin());
                    }

                    if (latestPr.getHead() != null) {
                        context.put("prSourceBranch", latestPr.getHead().getRef());
                    }

                    if (latestPr.getBase() != null) {
                        context.put("prTargetBranch", latestPr.getBase().getRef());
                    }

                    // Update state with latest PR number to prevent reprocessing
                    state.setLastProcessedMessageId("pr:" + latestPr.getNumber());
                    stateService.update(state);
                    logger.debug("Updated state for area {} to PR number {}", area.getId(), latestPr.getNumber());
                }

                return context;
            })
            .doOnSuccess(context -> {
                if (context.getInteger("prCount") != null && context.getInteger("prCount") > 0) {
                    logger.info("GitHub PR action triggered for area {} with {} new PR(s)",
                               area.getId(), context.getInteger("prCount"));
                }
            })
            .onErrorResume(error -> {
                logger.error("Error executing GitHub PR action for area {}: {}",
                           area.getId(), error.getMessage());
                return Mono.just(new TriggerContext());
            });
    }

    /**
     * Parse PR number from stored state
     * Format: "pr:456" or just "456"
     */
    private Long parsePrNumber(String lastProcessedMessageId) {
        if (lastProcessedMessageId == null || lastProcessedMessageId.isBlank()) {
            return null;
        }

        try {
            if (lastProcessedMessageId.startsWith("pr:")) {
                return Long.parseLong(lastProcessedMessageId.substring(3));
            }
            return Long.parseLong(lastProcessedMessageId);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse PR number from state: {}", lastProcessedMessageId);
            return null;
        }
    }
}
