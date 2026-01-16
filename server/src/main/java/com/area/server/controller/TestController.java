package com.area.server.controller;

import com.area.server.controller.dto.*;
import com.area.server.dto.response.ApiResponse;
import com.area.server.model.*;
import com.area.server.repository.ServiceConnectionRepository;
import com.area.server.security.CustomUserDetailsService;
import com.area.server.service.integration.executor.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for testing individual triggers and reactions.
 * Provides endpoints to test each trigger/action and reaction independently
 * without creating full workflows.
 *
 * All endpoints require authentication and verify that the service connection
 * belongs to the authenticated user.
 */
@RestController
@RequestMapping("/api/test")
@PreAuthorize("isAuthenticated()")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final ServiceConnectionRepository serviceConnectionRepository;
    private final CustomUserDetailsService userDetailsService;
    private final ActionExecutorRegistry actionExecutorRegistry;
    private final ReactionExecutorRegistry reactionExecutorRegistry;

    public TestController(
            ServiceConnectionRepository serviceConnectionRepository,
            CustomUserDetailsService userDetailsService,
            ActionExecutorRegistry actionExecutorRegistry,
            ReactionExecutorRegistry reactionExecutorRegistry) {
        this.serviceConnectionRepository = serviceConnectionRepository;
        this.userDetailsService = userDetailsService;
        this.actionExecutorRegistry = actionExecutorRegistry;
        this.reactionExecutorRegistry = reactionExecutorRegistry;
    }

    /**
     * Test Gmail email_received trigger.
     * Checks for new emails matching the provided filters and returns
     * what context data would be provided to reactions.
     *
     * POST /api/test/trigger/gmail/email_received
     *
     * @param request test configuration
     * @return trigger test result with context data
     */
    @PostMapping("/trigger/gmail/email_received")
    public ResponseEntity<ApiResponse<TestTriggerResponse>> testGmailTrigger(
            @Valid @RequestBody TestTriggerRequest request) {

        logger.info("Testing Gmail email_received trigger");

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.GMAIL) {
            throw new IllegalArgumentException("Service connection must be of type GMAIL");
        }

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection);

        // Set Gmail config
        GmailActionConfig gmailConfig = new GmailActionConfig();
        gmailConfig.setLabel(request.getGmailLabel());
        gmailConfig.setSubjectContains(request.getGmailSubjectContains());
        gmailConfig.setFromAddress(request.getGmailFromAddress());
        testArea.setGmailConfig(gmailConfig);

        // Get the executor and check trigger
        ActionExecutor executor = actionExecutorRegistry.getExecutor("gmail.email_received");

        return executor.getTriggerContext(testArea)
                .flatMap(context -> {
                    Integer messageCount = context.getInteger("messageCount");
                    boolean triggered = messageCount != null && messageCount > 0;

                    String message = triggered
                            ? String.format("Trigger would fire! Found %d new email(s) matching your filters.",
                                    messageCount)
                            : "Trigger would not fire. No new emails found matching your filters.";

                    // Remove complex objects for clean JSON response
                    Map<String, Object> cleanContext = new HashMap<>(context.getData());
                    cleanContext.remove("newMessages"); // Remove full message list
                    cleanContext.remove("latestMessage"); // Remove full message object

                    TestTriggerResponse response = new TestTriggerResponse(
                            triggered, message, cleanContext, messageCount);

                    return Mono.just(
                            ResponseEntity.ok(ApiResponse.success("Gmail trigger tested successfully", response)));
                })
                .onErrorResume(error -> {
                    logger.error("Error testing Gmail trigger: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok(
                            ApiResponse.error("Failed to test Gmail trigger", error.getMessage())));
                })
                .block();
    }

    /**
     * Test GitHub issue_created trigger.
     * Checks for new issues in the specified repository.
     *
     * POST /api/test/trigger/github/issue_created
     *
     * @param request test configuration
     * @return trigger test result with context data
     */
    @PostMapping("/trigger/github/issue_created")
    public ResponseEntity<ApiResponse<TestTriggerResponse>> testGitHubIssueTrigger(
            @Valid @RequestBody TestTriggerRequest request) {

        logger.info("Testing GitHub issue_created trigger for repository: {}", request.getGithubRepository());

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.GITHUB) {
            throw new IllegalArgumentException("Service connection must be of type GITHUB");
        }

        if (request.getGithubRepository() == null || request.getGithubRepository().isBlank()) {
            throw new IllegalArgumentException("GitHub repository is required (format: owner/repo)");
        }

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection);

        // Set GitHub config
        GitHubActionConfig githubConfig = new GitHubActionConfig();
        githubConfig.setActionType("issue_created");
        githubConfig.setRepository(request.getGithubRepository());
        testArea.setGithubActionConfig(githubConfig);

        // Get the executor and check trigger
        ActionExecutor executor = actionExecutorRegistry.getExecutor("github.issue_created");

        return executor.getTriggerContext(testArea)
                .flatMap(context -> {
                    Integer issueCount = context.getInteger("issueCount");
                    boolean triggered = issueCount != null && issueCount > 0;

                    String message = triggered
                            ? String.format("Trigger would fire! Found %d new issue(s) in repository %s.",
                                    issueCount, request.getGithubRepository())
                            : String.format("Trigger would not fire. No new issues found in repository %s.",
                                    request.getGithubRepository());

                    // Remove complex objects for clean JSON response
                    Map<String, Object> cleanContext = new HashMap<>(context.getData());
                    cleanContext.remove("newIssues");
                    cleanContext.remove("latestIssue");

                    TestTriggerResponse response = new TestTriggerResponse(
                            triggered, message, cleanContext, issueCount);

                    return Mono.just(ResponseEntity
                            .ok(ApiResponse.success("GitHub issue trigger tested successfully", response)));
                })
                .onErrorResume(error -> {
                    logger.error("Error testing GitHub issue trigger: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok(
                            ApiResponse.error("Failed to test GitHub issue trigger", error.getMessage())));
                })
                .block();
    }

    /**
     * Test GitHub pr_created trigger.
     * Checks for new pull requests in the specified repository.
     *
     * POST /api/test/trigger/github/pr_created
     *
     * @param request test configuration
     * @return trigger test result with context data
     */
    @PostMapping("/trigger/github/pr_created")
    public ResponseEntity<ApiResponse<TestTriggerResponse>> testGitHubPRTrigger(
            @Valid @RequestBody TestTriggerRequest request) {

        logger.info("Testing GitHub pr_created trigger for repository: {}", request.getGithubRepository());

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.GITHUB) {
            throw new IllegalArgumentException("Service connection must be of type GITHUB");
        }

        if (request.getGithubRepository() == null || request.getGithubRepository().isBlank()) {
            throw new IllegalArgumentException("GitHub repository is required (format: owner/repo)");
        }

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection);

        // Set GitHub config
        GitHubActionConfig githubConfig = new GitHubActionConfig();
        githubConfig.setActionType("pr_created");
        githubConfig.setRepository(request.getGithubRepository());
        testArea.setGithubActionConfig(githubConfig);

        // Get the executor and check trigger
        ActionExecutor executor = actionExecutorRegistry.getExecutor("github.pr_created");

        return executor.getTriggerContext(testArea)
                .flatMap(context -> {
                    Integer prCount = context.getInteger("prCount");
                    boolean triggered = prCount != null && prCount > 0;

                    String message = triggered
                            ? String.format("Trigger would fire! Found %d new pull request(s) in repository %s.",
                                    prCount, request.getGithubRepository())
                            : String.format("Trigger would not fire. No new pull requests found in repository %s.",
                                    request.getGithubRepository());

                    // Remove complex objects for clean JSON response
                    Map<String, Object> cleanContext = new HashMap<>(context.getData());
                    cleanContext.remove("newPullRequests");
                    cleanContext.remove("latestPullRequest");

                    TestTriggerResponse response = new TestTriggerResponse(
                            triggered, message, cleanContext, prCount);

                    return Mono.just(
                            ResponseEntity.ok(ApiResponse.success("GitHub PR trigger tested successfully", response)));
                })
                .onErrorResume(error -> {
                    logger.error("Error testing GitHub PR trigger: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok(
                            ApiResponse.error("Failed to test GitHub PR trigger", error.getMessage())));
                })
                .block();
    }

    /**
     * Test Discord send_message reaction.
     * Actually sends a message to Discord using the provided configuration.
     *
     * POST /api/test/reaction/discord/send_message
     *
     * @param request test configuration with message template
     * @return reaction test result
     */
    @PostMapping("/reaction/discord/send_message")
    public ResponseEntity<ApiResponse<TestReactionResponse>> testDiscordReaction(
            @Valid @RequestBody TestReactionRequest request) {

        logger.info("Testing Discord send_message reaction");

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.DISCORD) {
            throw new IllegalArgumentException("Service connection must be of type DISCORD");
        }

        if (request.getDiscordChannelId() == null || request.getDiscordChannelId().isBlank()) {
            throw new IllegalArgumentException("Discord channel ID is required");
        }

        // Update connection metadata with channel ID
        String metadata = String.format("{\"channelId\":\"%s\"}", request.getDiscordChannelId());
        connection.setMetadata(metadata);

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection, connection);

        // Set Discord config
        DiscordReactionConfig discordConfig = new DiscordReactionConfig();
        discordConfig.setMessageTemplate(request.getDiscordMessageTemplate());
        testArea.setDiscordConfig(discordConfig);

        // Create mock context
        TriggerContext context = new TriggerContext(
                request.getMockContextData() != null
                        ? request.getMockContextData()
                        : createDefaultMockContext());

        // Get the executor and execute reaction
        ReactionExecutor executor = reactionExecutorRegistry.getExecutor("discord.send_message");

        return executor.execute(testArea, context)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("channelId", request.getDiscordChannelId());

                    TestReactionResponse response = new TestReactionResponse(
                            true,
                            "Discord message sent successfully!",
                            resultData);

                    return ResponseEntity.ok(ApiResponse.success("Discord reaction tested successfully", response));
                }))
                .onErrorResume(error -> {
                    logger.error("Error testing Discord reaction: {}", error.getMessage());
                    TestReactionResponse response = new TestReactionResponse(
                            false,
                            "Failed to send Discord message: " + error.getMessage(),
                            null);
                    return Mono.just(ResponseEntity
                            .ok(ApiResponse.success("Discord reaction test completed with errors", response)));
                })
                .block();
    }

    /**
     * Test GitHub create_issue reaction.
     * Actually creates an issue in the specified repository.
     *
     * POST /api/test/reaction/github/create_issue
     *
     * @param request test configuration with issue details
     * @return reaction test result with created issue details
     */
    @PostMapping("/reaction/github/create_issue")
    public ResponseEntity<ApiResponse<TestReactionResponse>> testGitHubIssueReaction(
            @Valid @RequestBody TestReactionRequest request) {

        logger.info("Testing GitHub create_issue reaction for repository: {}", request.getGithubRepository());

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.GITHUB) {
            throw new IllegalArgumentException("Service connection must be of type GITHUB");
        }

        if (request.getGithubRepository() == null || request.getGithubRepository().isBlank()) {
            throw new IllegalArgumentException("GitHub repository is required (format: owner/repo)");
        }

        if (request.getGithubIssueTitle() == null || request.getGithubIssueTitle().isBlank()) {
            throw new IllegalArgumentException("GitHub issue title is required");
        }

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection, connection);

        // Set GitHub reaction config
        GitHubReactionConfig githubConfig = new GitHubReactionConfig();
        githubConfig.setReactionType("create_issue");
        githubConfig.setRepository(request.getGithubRepository());
        githubConfig.setIssueTitle(request.getGithubIssueTitle());
        githubConfig.setIssueBody(request.getGithubIssueBody());
        githubConfig.setLabels(request.getGithubIssueLabels());
        testArea.setGithubReactionConfig(githubConfig);

        // Create mock context
        TriggerContext context = new TriggerContext(
                request.getMockContextData() != null
                        ? request.getMockContextData()
                        : createDefaultMockContext());

        // Get the executor and execute reaction
        ReactionExecutor executor = reactionExecutorRegistry.getExecutor("github.create_issue");

        return executor.execute(testArea, context)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("repository", request.getGithubRepository());
                    resultData.put("title", request.getGithubIssueTitle());

                    TestReactionResponse response = new TestReactionResponse(
                            true,
                            String.format("GitHub issue created successfully in repository %s!",
                                    request.getGithubRepository()),
                            resultData);

                    return ResponseEntity
                            .ok(ApiResponse.success("GitHub create_issue reaction tested successfully", response));
                }))
                .onErrorResume(error -> {
                    logger.error("Error testing GitHub create_issue reaction: {}", error.getMessage());
                    TestReactionResponse response = new TestReactionResponse(
                            false,
                            "Failed to create GitHub issue: " + error.getMessage(),
                            null);
                    return Mono.just(ResponseEntity.ok(
                            ApiResponse.success("GitHub create_issue reaction test completed with errors", response)));
                })
                .block();
    }

    /**
     * Test GitHub create_pr reaction.
     * Actually creates a pull request in the specified repository.
     *
     * POST /api/test/reaction/github/create_pr
     *
     * @param request test configuration with PR details
     * @return reaction test result with created PR details
     */
    @PostMapping("/reaction/github/create_pr")
    public ResponseEntity<ApiResponse<TestReactionResponse>> testGitHubPRReaction(
            @Valid @RequestBody TestReactionRequest request) {

        logger.info("Testing GitHub create_pr reaction for repository: {}", request.getGithubRepository());

        User user = getCurrentUser();
        ServiceConnection connection = validateAndGetConnection(request.getServiceConnectionId(), user);

        if (connection.getType() != ServiceConnection.ServiceType.GITHUB) {
            throw new IllegalArgumentException("Service connection must be of type GITHUB");
        }

        if (request.getGithubRepository() == null || request.getGithubRepository().isBlank()) {
            throw new IllegalArgumentException("GitHub repository is required (format: owner/repo)");
        }

        if (request.getGithubPrTitle() == null || request.getGithubPrTitle().isBlank()) {
            throw new IllegalArgumentException("GitHub PR title is required");
        }

        if (request.getGithubSourceBranch() == null || request.getGithubSourceBranch().isBlank()) {
            throw new IllegalArgumentException("GitHub source branch is required");
        }

        if (request.getGithubFilePath() == null || request.getGithubFilePath().isBlank()) {
            throw new IllegalArgumentException("GitHub file path is required");
        }

        // Create a temporary Area object for testing
        Area testArea = createTestArea(user, connection, connection);

        // Set GitHub reaction config
        GitHubReactionConfig githubConfig = new GitHubReactionConfig();
        githubConfig.setReactionType("create_pr");
        githubConfig.setRepository(request.getGithubRepository());
        githubConfig.setPrTitle(request.getGithubPrTitle());
        githubConfig.setPrBody(request.getGithubPrBody());
        githubConfig.setSourceBranch(request.getGithubSourceBranch());
        githubConfig
                .setTargetBranch(request.getGithubTargetBranch() != null ? request.getGithubTargetBranch() : "main");
        githubConfig.setCommitMessage(request.getGithubCommitMessage());
        githubConfig.setFilePath(request.getGithubFilePath());
        githubConfig.setFileContent(request.getGithubFileContent());
        testArea.setGithubReactionConfig(githubConfig);

        // Create mock context
        TriggerContext context = new TriggerContext(
                request.getMockContextData() != null
                        ? request.getMockContextData()
                        : createDefaultMockContext());

        // Get the executor and execute reaction
        ReactionExecutor executor = reactionExecutorRegistry.getExecutor("github.create_pr");

        return executor.execute(testArea, context)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("repository", request.getGithubRepository());
                    resultData.put("title", request.getGithubPrTitle());
                    resultData.put("sourceBranch", request.getGithubSourceBranch());
                    resultData.put("targetBranch", githubConfig.getTargetBranch());

                    TestReactionResponse response = new TestReactionResponse(
                            true,
                            String.format("GitHub pull request created successfully in repository %s!",
                                    request.getGithubRepository()),
                            resultData);

                    return ResponseEntity
                            .ok(ApiResponse.success("GitHub create_pr reaction tested successfully", response));
                }))
                .onErrorResume(error -> {
                    logger.error("Error testing GitHub create_pr reaction: {}", error.getMessage());
                    TestReactionResponse response = new TestReactionResponse(
                            false,
                            "Failed to create GitHub pull request: " + error.getMessage(),
                            null);
                    return Mono.just(ResponseEntity
                            .ok(ApiResponse.success("GitHub create_pr reaction test completed with errors", response)));
                })
                .block();
    }

    // Helper methods

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userDetailsService.loadUserEntityByEmail(email);
    }

    /**
     * Validate that the service connection exists and belongs to the current user
     */
    private ServiceConnection validateAndGetConnection(Long connectionId, User user) {
        ServiceConnection connection = serviceConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Service connection not found"));

        if (!connection.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Service connection does not belong to the current user");
        }

        return connection;
    }

    /**
     * Create a temporary Area object for testing (action only)
     */
    private Area createTestArea(User user, ServiceConnection actionConnection) {
        Area area = new Area();
        area.setUser(user);
        area.setActionConnection(actionConnection);
        return area;
    }

    /**
     * Create a temporary Area object for testing (with both action and reaction
     * connections)
     */
    private Area createTestArea(User user, ServiceConnection actionConnection, ServiceConnection reactionConnection) {
        Area area = new Area();
        area.setUser(user);
        area.setActionConnection(actionConnection);
        area.setReactionConnection(reactionConnection);
        return area;
    }

    /**
     * Create default mock context data for testing reactions
     */
    private Map<String, Object> createDefaultMockContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("messageCount", 1);
        context.put("subject", "Test Email Subject");
        context.put("from", "test@example.com");
        context.put("snippet", "This is a test email snippet for testing purposes.");
        return context;
    }
}
