package com.area.server.service.integration.executor;

import com.area.server.dto.GitHubIssue;
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
 * Executor for GitHub "issue_created" action.
 * Checks for new issues created in a repository.
 */
@Component
public class GitHubIssueActionExecutor implements ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubIssueActionExecutor.class);

    private final GitHubService githubService;
    private final TriggerStateService areaStateService;
    private final WorkflowTriggerStateService workflowStateService;

    public GitHubIssueActionExecutor(GitHubService githubService,
            TriggerStateService areaStateService,
            WorkflowTriggerStateService workflowStateService) {
        this.githubService = githubService;
        this.areaStateService = areaStateService;
        this.workflowStateService = workflowStateService;
    }

    @Override
    public String getActionType() {
        return "github.issue_created";
    }

    @Override
    public Mono<Boolean> isTriggered(AutomationEntity entity) {
        return getTriggerContext(entity)
                .map(context -> context.has("newIssues") &&
                        context.getInteger("issueCount") != null &&
                        context.getInteger("issueCount") > 0);
    }

    @Override
    public Mono<TriggerContext> getTriggerContext(AutomationEntity entity) {
        GitHubActionConfig config = entity.getGithubActionConfig();

        if (config == null || !"issue_created".equals(config.getActionType())) {
            return Mono.just(new TriggerContext());
        }

        if (entity.getActionConnection() == null) {
            logger.error("No GitHub connection found for entity {}", entity.getId());
            return Mono.just(new TriggerContext());
        }

        String lastProcessedId = getLastProcessedId(entity);
        Long afterIssueNumber = parseIssueNumber(lastProcessedId);

        return githubService.fetchNewIssues(
                entity.getActionConnection(),
                config,
                afterIssueNumber)
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
                        updateState(entity, "issue:" + latestIssue.getNumber());
                        logger.debug("Updated state for entity {} to issue number {}", entity.getId(),
                                latestIssue.getNumber());
                    }

                    return context;
                })
                .doOnSuccess(context -> {
                    if (context.getInteger("issueCount") != null && context.getInteger("issueCount") > 0) {
                        logger.info("GitHub issue action triggered for entity {} with {} new issue(s)",
                                entity.getId(), context.getInteger("issueCount"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error executing GitHub issue action for entity {}: {}",
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
