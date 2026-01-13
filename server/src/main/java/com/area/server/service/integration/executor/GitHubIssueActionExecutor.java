package com.area.server.service.integration.executor;

import com.area.server.dto.GitHubIssue;
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
 * Executor for GitHub "issue_created" action.
 * Checks for new issues created in a repository.
 */
@Component
public class GitHubIssueActionExecutor implements ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubIssueActionExecutor.class);

    private final GitHubService githubService;
    private final TriggerStateService stateService;

    public GitHubIssueActionExecutor(GitHubService githubService,
                                      TriggerStateService stateService) {
        this.githubService = githubService;
        this.stateService = stateService;
    }

    @Override
    public String getActionType() {
        return "github.issue_created";
    }

    @Override
    public Mono<Boolean> isTriggered(Area area) {
        return getTriggerContext(area)
            .map(context -> context.has("newIssues") &&
                           context.getInteger("issueCount") != null &&
                           context.getInteger("issueCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(Area area) {
        GitHubActionConfig config = area.getGithubActionConfig();

        if (config == null || !"issue_created".equals(config.getActionType())) {
            return Mono.just(new TriggerContext());
        }

        AreaTriggerState state = stateService.getOrCreateState(area);
        Long afterIssueNumber = parseIssueNumber(state.getLastProcessedMessageId());

        return githubService.fetchNewIssues(
                area.getActionConnection(),
                config,
                afterIssueNumber
            )
            .map(newIssues -> {
                TriggerContext context = new TriggerContext();
                context.put("newIssues", newIssues);
                context.put("issueCount", newIssues.size());

                if (!newIssues.isEmpty()) {
                    GitHubIssue latestIssue = newIssues.get(0);
                    context.put("latestIssue", latestIssue);
                    context.put("issueNumber", latestIssue.getNumber());
                    context.put("issueTitle", latestIssue.getTitle());
                    context.put("issueBody", latestIssue.getBody());
                    context.put("issueUrl", latestIssue.getHtmlUrl());

                    if (latestIssue.getUser() != null) {
                        context.put("issueAuthor", latestIssue.getUser().getLogin());
                    }

                    // Update state with latest issue number to prevent reprocessing
                    state.setLastProcessedMessageId("issue:" + latestIssue.getNumber());
                    stateService.update(state);
                    logger.debug("Updated state for area {} to issue number {}", area.getId(), latestIssue.getNumber());
                }

                return context;
            })
            .doOnSuccess(context -> {
                if (context.getInteger("issueCount") != null && context.getInteger("issueCount") > 0) {
                    logger.info("GitHub issue action triggered for area {} with {} new issue(s)",
                               area.getId(), context.getInteger("issueCount"));
                }
            })
            .onErrorResume(error -> {
                logger.error("Error executing GitHub issue action for area {}: {}",
                           area.getId(), error.getMessage());
                return Mono.just(new TriggerContext());
            });
    }

    /**
     * Parse issue number from stored state
     * Format: "issue:123" or just "123"
     */
    private Long parseIssueNumber(String lastProcessedMessageId) {
        if (lastProcessedMessageId == null || lastProcessedMessageId.isBlank()) {
            return null;
        }

        try {
            if (lastProcessedMessageId.startsWith("issue:")) {
                return Long.parseLong(lastProcessedMessageId.substring(6));
            }
            return Long.parseLong(lastProcessedMessageId);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse issue number from state: {}", lastProcessedMessageId);
            return null;
        }
    }
}
