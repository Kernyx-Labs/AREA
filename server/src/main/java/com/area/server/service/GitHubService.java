package com.area.server.service;

import com.area.server.dto.GitHubApiResponse;
import com.area.server.dto.GitHubIssue;
import com.area.server.dto.GitHubPullRequest;
import com.area.server.model.GitHubActionConfig;
import com.area.server.model.GitHubReactionConfig;
import com.area.server.model.ServiceConnection;
import com.area.server.service.integration.executor.TriggerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for interacting with GitHub API.
 * Handles OAuth token management, fetching issues/PRs, and creating issues/PRs.
 */
@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private static final int MAX_RESULTS = 30;
    private static final int MAX_RETRIES = 3;

    private final WebClient githubClient;
    private final ObjectMapper objectMapper;

    public GitHubService(@Value("${github.api.base:https://api.github.com}") String baseUrl,
                         WebClient.Builder builder,
                         ObjectMapper objectMapper) {
        this.githubClient = builder
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch new issues created after a specific issue number
     */
    public Mono<List<GitHubIssue>> fetchNewIssues(ServiceConnection connection,
                                                   GitHubActionConfig config,
                                                   Long afterIssueNumber) {
        String owner = config.getRepositoryOwner();
        String repo = config.getRepositoryName();

        logger.debug("Fetching new issues from {}/{} (after: {})", owner, repo, afterIssueNumber);

        return githubClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/repos/{owner}/{repo}/issues")
                .queryParam("state", "open")
                .queryParam("sort", "created")
                .queryParam("direction", "desc")
                .queryParam("per_page", MAX_RESULTS)
                .build(owner, repo))
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .filter(node -> !node.has("pull_request")) // Exclude PRs (they appear as issues too)
            .map(this::parseIssue)
            .filter(issue -> afterIssueNumber == null || issue.getNumber() > afterIssueNumber)
            .collectList()
            .doOnSuccess(issues -> logger.debug("Fetched {} new issues", issues.size()))
            .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(2))
                .filter(this::isRetriableError))
            .onErrorResume(error -> {
                logger.error("Error fetching GitHub issues from {}/{}: {}",
                           owner, repo, error.getMessage());
                return Mono.just(Collections.emptyList());
            });
    }

    /**
     * Fetch new pull requests created after a specific PR number
     */
    public Mono<List<GitHubPullRequest>> fetchNewPullRequests(ServiceConnection connection,
                                                               GitHubActionConfig config,
                                                               Long afterPrNumber) {
        String owner = config.getRepositoryOwner();
        String repo = config.getRepositoryName();

        logger.debug("Fetching new pull requests from {}/{} (after: {})", owner, repo, afterPrNumber);

        return githubClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/repos/{owner}/{repo}/pulls")
                .queryParam("state", "open")
                .queryParam("sort", "created")
                .queryParam("direction", "desc")
                .queryParam("per_page", MAX_RESULTS)
                .build(owner, repo))
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .map(this::parsePullRequest)
            .filter(pr -> afterPrNumber == null || pr.getNumber() > afterPrNumber)
            .collectList()
            .doOnSuccess(prs -> logger.debug("Fetched {} new pull requests", prs.size()))
            .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(2))
                .filter(this::isRetriableError))
            .onErrorResume(error -> {
                logger.error("Error fetching GitHub pull requests from {}/{}: {}",
                           owner, repo, error.getMessage());
                return Mono.just(Collections.emptyList());
            });
    }

    /**
     * Create a new issue in the specified repository
     */
    public Mono<GitHubApiResponse.CreateIssueResponse> createIssue(ServiceConnection connection,
                                                                     GitHubReactionConfig config,
                                                                     TriggerContext context) {
        String owner = config.getRepositoryOwner();
        String repo = config.getRepositoryName();

        String title = substituteVariables(config.getIssueTitle(), context);
        String body = substituteVariables(config.getIssueBody(), context);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("body", body);

        // Add labels if specified
        if (config.getLabels() != null && !config.getLabels().isBlank()) {
            String[] labelsArray = config.getLabels().split(",");
            List<String> labels = Arrays.stream(labelsArray)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            if (!labels.isEmpty()) {
                requestBody.put("labels", labels);
            }
        }

        logger.info("Creating issue in {}/{}: {}", owner, repo, title);

        return githubClient.post()
            .uri("/repos/{owner}/{repo}/issues", owner, repo)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GitHubApiResponse.CreateIssueResponse.class)
            .doOnSuccess(response ->
                logger.info("Successfully created issue #{} in {}/{}",
                          response.getNumber(), owner, repo))
            .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(2))
                .filter(this::isRetriableError))
            .onErrorResume(error -> {
                logger.error("Failed to create issue in {}/{}: {}", owner, repo, error.getMessage());
                return Mono.error(error);
            });
    }

    /**
     * Create a new pull request in the specified repository
     * This creates a branch, commits a file, and opens a PR
     */
    public Mono<GitHubApiResponse.CreatePullRequestResponse> createPullRequest(
            ServiceConnection connection,
            GitHubReactionConfig config,
            TriggerContext context) {

        String owner = config.getRepositoryOwner();
        String repo = config.getRepositoryName();
        String sourceBranch = substituteVariables(config.getSourceBranch(), context);
        String targetBranch = config.getTargetBranch() != null ? config.getTargetBranch() : "main";

        logger.info("Creating pull request in {}/{}: {} -> {}", owner, repo, sourceBranch, targetBranch);

        // Step 1: Get the SHA of the target branch
        return getRef(connection, owner, repo, targetBranch)
            .flatMap(targetSha -> {
                // Step 2: Create a new branch from target
                return createBranch(connection, owner, repo, sourceBranch, targetSha);
            })
            .flatMap(branchSha -> {
                // Step 3: Create or update file in the new branch
                String filePath = substituteVariables(config.getFilePath(), context);
                String fileContent = substituteVariables(config.getFileContent(), context);
                // Default commit message to PR title if not provided
                String commitMessage = (config.getCommitMessage() != null && !config.getCommitMessage().isBlank())
                    ? substituteVariables(config.getCommitMessage(), context)
                    : substituteVariables(config.getPrTitle(), context);

                return commitFile(connection, owner, repo, sourceBranch, filePath, fileContent, commitMessage);
            })
            .flatMap(commitSha -> {
                // Step 4: Create the pull request
                String prTitle = substituteVariables(config.getPrTitle(), context);
                String prBody = substituteVariables(config.getPrBody(), context);

                return createPr(connection, owner, repo, prTitle, prBody, sourceBranch, targetBranch);
            })
            .doOnSuccess(response ->
                logger.info("Successfully created PR #{} in {}/{}", response.getNumber(), owner, repo))
            .onErrorResume(error -> {
                logger.error("Failed to create pull request in {}/{}: {}", owner, repo, error.getMessage());
                return Mono.error(error);
            });
    }

    /**
     * Get the SHA of a branch
     */
    private Mono<String> getRef(ServiceConnection connection, String owner, String repo, String branch) {
        return githubClient.get()
            .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", owner, repo, branch)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .retrieve()
            .bodyToMono(GitHubApiResponse.GitRefResponse.class)
            .map(response -> response.getObject().getSha())
            .doOnSuccess(sha -> logger.debug("Got SHA for {}/{}/heads/{}: {}", owner, repo, branch, sha));
    }

    /**
     * Create a new branch
     */
    private Mono<String> createBranch(ServiceConnection connection, String owner, String repo,
                                       String branchName, String sha) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ref", "refs/heads/" + branchName);
        requestBody.put("sha", sha);

        return githubClient.post()
            .uri("/repos/{owner}/{repo}/git/refs", owner, repo)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GitHubApiResponse.GitRefResponse.class)
            .map(response -> response.getObject().getSha())
            .doOnSuccess(branchSha -> logger.debug("Created branch {} in {}/{}", branchName, owner, repo))
            .onErrorResume(error -> {
                if (error instanceof WebClientResponseException.UnprocessableEntity) {
                    // Branch might already exist, get its SHA
                    logger.warn("Branch {} already exists in {}/{}, using existing branch",
                              branchName, owner, repo);
                    return getRef(connection, owner, repo, branchName);
                }
                return Mono.error(error);
            });
    }

    /**
     * Create or update a file in a branch
     */
    private Mono<String> commitFile(ServiceConnection connection, String owner, String repo,
                                      String branch, String filePath, String content, String message) {
        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("content", encodedContent);
        requestBody.put("branch", branch);

        return githubClient.put()
            .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, filePath)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GitHubApiResponse.FileContentResponse.class)
            .map(response -> response.getCommit().getSha())
            .doOnSuccess(sha -> logger.debug("Committed file {} to {}/{}/{}", filePath, owner, repo, branch));
    }

    /**
     * Create a pull request
     */
    private Mono<GitHubApiResponse.CreatePullRequestResponse> createPr(
            ServiceConnection connection, String owner, String repo,
            String title, String body, String head, String base) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("body", body);
        requestBody.put("head", head);
        requestBody.put("base", base);

        return githubClient.post()
            .uri("/repos/{owner}/{repo}/pulls", owner, repo)
            .headers(headers -> headers.setBearerAuth(connection.getAccessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(GitHubApiResponse.CreatePullRequestResponse.class);
    }

    /**
     * Get authenticated user's information
     */
    public Mono<GitHubApiResponse.UserResponse> getUserInfo(String accessToken) {
        return githubClient.get()
            .uri("/user")
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(GitHubApiResponse.UserResponse.class)
            .doOnSuccess(user -> logger.debug("Retrieved GitHub user: {}", user.getLogin()));
    }

    /**
     * Substitute variables in templates with values from trigger context
     */
    private String substituteVariables(String template, TriggerContext context) {
        if (template == null || template.isBlank()) {
            return template;
        }

        String result = template;

        // Gmail trigger variables
        if (context.has("subject")) {
            String subject = context.getString("subject");
            if (subject != null) {
                result = result.replace("{email.subject}", subject);
                result = result.replace("{subject}", subject); // Backward compatibility
            }
        }
        if (context.has("from")) {
            String from = context.getString("from");
            if (from != null) {
                result = result.replace("{email.from}", from);
                result = result.replace("{from}", from); // Backward compatibility
            }
        }
        if (context.has("snippet")) {
            String snippet = context.getString("snippet");
            if (snippet != null) {
                result = result.replace("{email.snippet}", snippet);
                result = result.replace("{snippet}", snippet); // Backward compatibility
            }
        }

        // GitHub issue trigger variables
        if (context.has("issueNumber")) {
            Object issueNumber = context.get("issueNumber");
            if (issueNumber != null) {
                result = result.replace("{issue.number}", String.valueOf(issueNumber));
            }
        }
        if (context.has("issueTitle")) {
            String issueTitle = context.getString("issueTitle");
            if (issueTitle != null) {
                result = result.replace("{issue.title}", issueTitle);
            }
        }
        if (context.has("issueAuthor")) {
            String issueAuthor = context.getString("issueAuthor");
            if (issueAuthor != null) {
                result = result.replace("{issue.author}", issueAuthor);
            }
        }
        if (context.has("issueUrl")) {
            String issueUrl = context.getString("issueUrl");
            if (issueUrl != null) {
                result = result.replace("{issue.url}", issueUrl);
            }
        }

        // GitHub PR trigger variables
        if (context.has("prNumber")) {
            Object prNumber = context.get("prNumber");
            if (prNumber != null) {
                result = result.replace("{pr.number}", String.valueOf(prNumber));
            }
        }
        if (context.has("prTitle")) {
            String prTitle = context.getString("prTitle");
            if (prTitle != null) {
                result = result.replace("{pr.title}", prTitle);
            }
        }
        if (context.has("prAuthor")) {
            String prAuthor = context.getString("prAuthor");
            if (prAuthor != null) {
                result = result.replace("{pr.author}", prAuthor);
            }
        }
        if (context.has("prUrl")) {
            String prUrl = context.getString("prUrl");
            if (prUrl != null) {
                result = result.replace("{pr.url}", prUrl);
            }
        }

        return result;
    }

    /**
     * Parse JSON node to GitHubIssue
     */
    private GitHubIssue parseIssue(JsonNode node) {
        GitHubIssue issue = new GitHubIssue();
        issue.setNumber(node.get("number").asLong());
        issue.setTitle(node.get("title").asText());
        issue.setBody(node.has("body") && !node.get("body").isNull()
                     ? node.get("body").asText() : "");
        issue.setState(node.get("state").asText());
        issue.setHtmlUrl(node.get("html_url").asText());

        if (node.has("created_at")) {
            issue.setCreatedAt(Instant.parse(node.get("created_at").asText()));
        }
        if (node.has("updated_at")) {
            issue.setUpdatedAt(Instant.parse(node.get("updated_at").asText()));
        }

        if (node.has("user")) {
            GitHubIssue.GitHubUser user = new GitHubIssue.GitHubUser();
            user.setLogin(node.get("user").get("login").asText());
            user.setHtmlUrl(node.get("user").get("html_url").asText());
            issue.setUser(user);
        }

        return issue;
    }

    /**
     * Parse JSON node to GitHubPullRequest
     */
    private GitHubPullRequest parsePullRequest(JsonNode node) {
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(node.get("number").asLong());
        pr.setTitle(node.get("title").asText());
        pr.setBody(node.has("body") && !node.get("body").isNull()
                  ? node.get("body").asText() : "");
        pr.setState(node.get("state").asText());
        pr.setHtmlUrl(node.get("html_url").asText());

        if (node.has("created_at")) {
            pr.setCreatedAt(Instant.parse(node.get("created_at").asText()));
        }
        if (node.has("updated_at")) {
            pr.setUpdatedAt(Instant.parse(node.get("updated_at").asText()));
        }
        if (node.has("draft")) {
            pr.setDraft(node.get("draft").asBoolean());
        }
        if (node.has("merged")) {
            pr.setMerged(node.get("merged").asBoolean());
        }

        if (node.has("user")) {
            GitHubPullRequest.GitHubUser user = new GitHubPullRequest.GitHubUser();
            user.setLogin(node.get("user").get("login").asText());
            user.setHtmlUrl(node.get("user").get("html_url").asText());
            pr.setUser(user);
        }

        if (node.has("head")) {
            GitHubPullRequest.GitHubBranch head = new GitHubPullRequest.GitHubBranch();
            head.setRef(node.get("head").get("ref").asText());
            head.setSha(node.get("head").get("sha").asText());
            pr.setHead(head);
        }

        if (node.has("base")) {
            GitHubPullRequest.GitHubBranch base = new GitHubPullRequest.GitHubBranch();
            base.setRef(node.get("base").get("ref").asText());
            base.setSha(node.get("base").get("sha").asText());
            pr.setBase(base);
        }

        return pr;
    }

    /**
     * Check if an error is retriable (network errors, rate limits, server errors)
     */
    private boolean isRetriableError(Throwable error) {
        if (error instanceof WebClientResponseException webClientError) {
            HttpStatus status = (HttpStatus) webClientError.getStatusCode();
            // Retry on rate limits (403), server errors (5xx), and gateway timeouts
            return status == HttpStatus.FORBIDDEN
                || status == HttpStatus.TOO_MANY_REQUESTS
                || status.is5xxServerError();
        }
        // Retry on network errors
        return true;
    }
}
