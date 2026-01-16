package com.area.server.service.integration.executor;

import com.area.server.dto.GitHubPullRequest;
import com.area.server.model.Area;
import com.area.server.model.AreaTriggerState;
import com.area.server.model.AutomationEntity;
import com.area.server.model.GitHubActionConfig;
import com.area.server.model.WorkflowTriggerState;
import com.area.server.scheduler.WorkflowWrapper;
import com.area.server.service.GitHubService;
import com.area.server.service.TriggerStateService;
import com.area.server.service.WorkflowTriggerStateService;
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
    private final TriggerStateService areaStateService;
    private final WorkflowTriggerStateService workflowStateService;

    public GitHubPullRequestActionExecutor(GitHubService githubService,
            TriggerStateService areaStateService,
            WorkflowTriggerStateService workflowStateService) {
        this.githubService = githubService;
        this.areaStateService = areaStateService;
        this.workflowStateService = workflowStateService;
    }

    @Override
    public String getActionType() {
        return "github.pr_created";
    }

    @Override
    public Mono<Boolean> isTriggered(AutomationEntity entity) {
        return getTriggerContext(entity)
                .map(context -> context.has("newPRs") &&
                        context.getInteger("prCount") != null &&
                        context.getInteger("prCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(AutomationEntity entity) {
        GitHubActionConfig config = entity.getGithubActionConfig();

        if (config == null || !"pr_created".equals(config.getActionType())) {
            return Mono.just(new TriggerContext());
        }

        String lastProcessedId = getLastProcessedId(entity);
        Long afterPrNumber = parsePrNumber(lastProcessedId);

        return githubService.fetchNewPullRequests(
                entity.getActionConnection(),
                config,
                afterPrNumber)
                .map(newPullRequests -> {
                    TriggerContext context = new TriggerContext();
                    context.put("newPRs", newPullRequests);
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
                        updateState(entity, "pr:" + latestPr.getNumber());
                        logger.debug("Updated state for entity {} to PR number {}", entity.getId(),
                                latestPr.getNumber());
                    }

                    return context;
                })
                .doOnSuccess(context -> {
                    if (context.getInteger("prCount") != null && context.getInteger("prCount") > 0) {
                        logger.info("GitHub PR action triggered for entity {} with {} new PR(s)",
                                entity.getId(), context.getInteger("prCount"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error executing GitHub PR action for entity {}: {}",
                            entity.getId(), error.getMessage());
                    return Mono.just(new TriggerContext());
                });
    }

    private String getLastProcessedId(AutomationEntity entity) {
        if (entity instanceof Area area) {
            AreaTriggerState state = areaStateService.getOrCreateState(area);
            return state.getLastProcessedMessageId();
        } else if (entity instanceof WorkflowWrapper wrapper) {
            WorkflowTriggerState state = workflowStateService.getOrCreateState(wrapper.getWorkflow());
            return state.getLastProcessedItemId();
        }
        return null;
    }

    private void updateState(AutomationEntity entity, String lastItemId) {
        if (entity instanceof Area area) {
            AreaTriggerState state = areaStateService.getOrCreateState(area);
            state.setLastProcessedMessageId(lastItemId);
            areaStateService.update(state);
        } else if (entity instanceof WorkflowWrapper wrapper) {
            WorkflowTriggerState state = workflowStateService.getOrCreateState(wrapper.getWorkflow());
            state.setLastProcessedItemId(lastItemId);
            workflowStateService.update(state);
        }
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
