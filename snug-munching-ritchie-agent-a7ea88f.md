# Implementation Plan: Dynamic Repository Selection for GitHub Triggers

## Overview
Add dynamic repository selection capability to GitHub triggers by creating a new API endpoint that fetches the authenticated user's accessible repositories and updating the field definition system to support a single "owner/repo" format.

---

## Phase 1: Create New DTO Classes

### 1.1 Create GitHubRepositoryDTO.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/dto/GitHubRepositoryDTO.java`

**Purpose:** Response DTO for the repository list endpoint

**Structure:**
```java
package com.area.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitHub repository information used in dynamic repository selection
 */
public class GitHubRepositoryDTO {
    @JsonProperty("full_name")
    private String fullName;  // Format: "owner/repo"

    private String description;

    @JsonProperty("private")
    private boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    // Standard getters and setters
}
```

**Design Rationale:**
- `fullName` is the primary field matching GitHub API's `full_name` field (e.g., "octocat/Hello-World")
- `description` is optional and helps users identify repositories
- `isPrivate` flag helps frontend indicate repository visibility
- `htmlUrl` provides a link to the repository (optional, for UX enhancement)

---

### 1.2 Add Repository Response to GitHubApiResponse.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/dto/GitHubApiResponse.java`

**Add nested class:**
```java
/**
 * Response from GitHub user repositories API
 */
public static class RepositoryResponse {
    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    private String name;

    private String description;

    @JsonProperty("private")
    private boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    private Owner owner;

    private Permissions permissions;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    // Getters and setters

    public static class Owner {
        private String login;

        @JsonProperty("html_url")
        private String htmlUrl;

        // Getters and setters
    }

    public static class Permissions {
        private boolean admin;
        private boolean push;
        private boolean pull;

        // Getters and setters
    }
}
```

**Design Rationale:**
- Matches GitHub API `/user/repos` response structure
- `permissions` field helps filter repositories based on access level
- Will be used internally by GitHubService

---

## Phase 2: Extend GitHubService

### 2.1 Add getUserRepositories Method
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/GitHubService.java`

**Add method:**
```java
/**
 * Fetch all repositories accessible by the authenticated user.
 * This includes personal repositories, organization repositories, and repositories
 * where the user has been granted access.
 *
 * @param accessToken The GitHub OAuth access token
 * @param page The page number for pagination (default: 1)
 * @param perPage Number of results per page (default: 100, max: 100)
 * @return Mono containing list of repositories
 */
public Mono<List<GitHubRepositoryDTO>> getUserRepositories(String accessToken,
                                                             int page,
                                                             int perPage) {
    // Clamp perPage to GitHub's maximum
    int actualPerPage = Math.min(perPage, 100);

    logger.debug("Fetching user repositories (page: {}, per_page: {})", page, actualPerPage);

    return githubClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/user/repos")
            .queryParam("sort", "updated")
            .queryParam("direction", "desc")
            .queryParam("per_page", actualPerPage)
            .queryParam("page", page)
            .queryParam("affiliation", "owner,collaborator,organization_member")
            .build())
        .headers(headers -> headers.setBearerAuth(accessToken))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(GitHubApiResponse.RepositoryResponse.class)
        .filter(repo -> {
            // Only include repositories where user has push access (needed for webhooks/monitoring)
            return repo.getPermissions() != null &&
                   (repo.getPermissions().isPush() || repo.getPermissions().isAdmin());
        })
        .map(this::mapToRepositoryDTO)
        .collectList()
        .doOnSuccess(repos -> logger.debug("Fetched {} repositories", repos.size()))
        .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(2))
            .filter(this::isRetriableError))
        .onErrorResume(error -> {
            logger.error("Error fetching GitHub repositories: {}", error.getMessage());
            if (error instanceof WebClientResponseException webClientError) {
                logger.error("GitHub API error: status={}, body={}",
                           webClientError.getStatusCode(),
                           webClientError.getResponseBodyAsString());
            }
            return Mono.error(error);
        });
}

/**
 * Helper method to map GitHub API repository response to DTO
 */
private GitHubRepositoryDTO mapToRepositoryDTO(GitHubApiResponse.RepositoryResponse repo) {
    GitHubRepositoryDTO dto = new GitHubRepositoryDTO();
    dto.setFullName(repo.getFullName());
    dto.setDescription(repo.getDescription());
    dto.setPrivate(repo.isPrivate());
    dto.setHtmlUrl(repo.getHtmlUrl());
    return dto;
}
```

**Design Rationale:**
- **Pagination support:** GitHub API returns max 100 results per page; pagination allows fetching all repositories
- **Affiliation filter:** `owner,collaborator,organization_member` ensures we get all repos user can access
- **Permission filtering:** Only include repos with push access since monitoring issues/PRs typically requires this level of access
- **Sorting:** Sort by `updated` descending to show most recently active repositories first
- **Error handling:** Comprehensive error logging including API status and response body
- **Retry logic:** Uses existing retry mechanism for network failures and rate limits

---

## Phase 3: Extend ServiceConnectionService

### 3.1 Add User-Specific Connection Queries
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/repository/ServiceConnectionRepository.java`

**Add query methods:**
```java
package com.area.server.repository;

import com.area.server.model.ServiceConnection;
import com.area.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceConnectionRepository extends JpaRepository<ServiceConnection, Long> {

    /**
     * Find all service connections for a specific user and service type
     */
    List<ServiceConnection> findByUserAndType(User user, ServiceConnection.ServiceType type);

    /**
     * Find the first service connection for a specific user and service type
     * Useful when we expect only one connection per service type per user
     */
    Optional<ServiceConnection> findFirstByUserAndType(User user, ServiceConnection.ServiceType type);
}
```

**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/ServiceConnectionService.java`

**Add methods:**
```java
/**
 * Find all connections for a specific user and service type
 */
public List<ServiceConnection> findByUserAndType(User user, ServiceConnection.ServiceType type) {
    return repository.findByUserAndType(user, type);
}

/**
 * Find the first connection for a specific user and service type
 */
public Optional<ServiceConnection> findFirstByUserAndType(User user, ServiceConnection.ServiceType type) {
    return repository.findFirstByUserAndType(user, type);
}
```

**Design Rationale:**
- Spring Data JPA automatically implements these query methods based on naming convention
- `findFirstByUserAndType` is useful since most users will have only one GitHub connection
- These methods enable the controller to retrieve user-specific GitHub access tokens

---

## Phase 4: Create New Controller Endpoint

### 4.1 Add Repository Endpoint to GitHubOAuthController
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/controller/GitHubOAuthController.java`

**Add endpoint method:**
```java
/**
 * Get list of repositories accessible by the authenticated user.
 * This endpoint is used to populate the repository dropdown in trigger configuration.
 *
 * Requires:
 * - User must be authenticated
 * - User must have a GitHub service connection (OAuth completed)
 *
 * Query Parameters:
 * - page: Page number for pagination (optional, default: 1)
 * - per_page: Results per page (optional, default: 100, max: 100)
 *
 * Response:
 * - List of repositories in format: { fullName: "owner/repo", description: "..." }
 * - Repositories are filtered to only include those with push/admin access
 * - Sorted by most recently updated
 *
 * Error Cases:
 * - 401: User not authenticated
 * - 404: No GitHub connection found for user
 * - 500: GitHub API error
 */
@GetMapping("/repositories")
public ResponseEntity<ApiResponse<List<GitHubRepositoryDTO>>> getRepositories(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(name = "per_page", defaultValue = "100") int perPage) {

    // Get authenticated user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() ||
        "anonymousUser".equals(authentication.getName())) {
        logger.error("Unauthenticated user tried to access GitHub repositories");
        return ResponseEntity.status(401)
            .body(ApiResponse.error("Authentication required. Please log in."));
    }

    String email = authentication.getName();
    User user = userDetailsService.loadUserEntityByEmail(email);

    // Find user's GitHub connection
    Optional<ServiceConnection> connectionOpt = connectionService
        .findFirstByUserAndType(user, ServiceConnection.ServiceType.GITHUB);

    if (connectionOpt.isEmpty()) {
        logger.warn("User {} attempted to fetch repositories without GitHub connection", email);
        return ResponseEntity.status(404)
            .body(ApiResponse.error("No GitHub connection found. Please connect your GitHub account first."));
    }

    ServiceConnection connection = connectionOpt.get();
    String accessToken = connection.getAccessToken();

    if (accessToken == null || accessToken.isBlank()) {
        logger.error("GitHub connection {} has no access token", connection.getId());
        return ResponseEntity.status(500)
            .body(ApiResponse.error("GitHub connection is invalid. Please reconnect your account."));
    }

    // Fetch repositories from GitHub
    try {
        List<GitHubRepositoryDTO> repositories = githubService
            .getUserRepositories(accessToken, page, perPage)
            .block(); // Block since this is a synchronous REST endpoint

        if (repositories == null) {
            logger.error("GitHubService returned null for user {}", email);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to fetch repositories from GitHub."));
        }

        logger.info("Successfully fetched {} repositories for user {}", repositories.size(), email);
        return ResponseEntity.ok(ApiResponse.success(repositories));

    } catch (Exception e) {
        logger.error("Error fetching repositories for user {}: {}", email, e.getMessage(), e);

        // Check if it's a GitHub API error
        if (e.getCause() instanceof WebClientResponseException webClientError) {
            HttpStatus status = (HttpStatus) webClientError.getStatusCode();

            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("GitHub access token is invalid or expired. Please reconnect your account."));
            }
        }

        return ResponseEntity.status(500)
            .body(ApiResponse.error("Failed to fetch repositories: " + e.getMessage()));
    }
}
```

**Design Rationale:**
- **Authentication check:** Ensures user is logged in before accessing GitHub data
- **Connection validation:** Verifies user has completed GitHub OAuth flow
- **Access token validation:** Checks token exists and is valid
- **Pagination support:** Allows frontend to handle large repository lists
- **Comprehensive error handling:** Returns appropriate HTTP status codes and user-friendly messages
- **Synchronous blocking:** Uses `.block()` since Spring MVC endpoints are synchronous (could be refactored to reactive if needed)
- **Logging:** Extensive logging for debugging and monitoring

---

## Phase 5: Update FieldDefinition to Support Dynamic Options

### 5.1 Extend FieldDefinition Class
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/FieldDefinition.java`

**Add new fields:**
```java
package com.area.server.service.integration;

import java.util.List;
import java.util.Map;

/**
 * Defines a configuration field for an action or reaction.
 * Describes the field type, validation rules, and user-facing metadata.
 */
public class FieldDefinition {
    private String name;
    private String label;
    private String type; // "string", "number", "boolean", "text", "select", etc.
    private boolean required;
    private String description;
    private Object defaultValue;

    // NEW: Support for select field options
    private List<SelectOption> options;

    // NEW: Metadata for dynamic field loading
    private Map<String, Object> metadata;

    // Existing constructors
    public FieldDefinition() {
    }

    public FieldDefinition(String name, String label, String type, boolean required, String description) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    // NEW: Constructor with metadata
    public FieldDefinition(String name, String label, String type, boolean required,
                           String description, Map<String, Object> metadata) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.description = description;
        this.metadata = metadata;
    }

    // Existing getters/setters...

    // NEW: Getters and setters for options and metadata
    public List<SelectOption> getOptions() {
        return options;
    }

    public void setOptions(List<SelectOption> options) {
        this.options = options;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Nested class for select field options
     */
    public static class SelectOption {
        private String value;
        private String label;

        public SelectOption() {
        }

        public SelectOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
```

**Design Rationale:**
- **Backward compatible:** Existing fields work without changes
- **SelectOption class:** Provides structure for dropdown options (value/label pairs)
- **Metadata map:** Flexible approach to add field-specific configuration without breaking changes
  - Example: `{ "dynamicOptionsEndpoint": "/api/services/github/repositories" }`
- **Frontend contract:** Frontend can check metadata for `dynamicOptionsEndpoint` to fetch options dynamically

---

## Phase 6: Update GitHubIntegration Field Definitions

### 6.1 Update GitHubIntegration.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/GitHubIntegration.java`

**Update getActions() method:**
```java
@Override
public List<ActionDefinition> getActions() {
    // Create metadata for dynamic repository field
    Map<String, Object> repositoryMetadata = new HashMap<>();
    repositoryMetadata.put("dynamicOptionsEndpoint", "/api/services/github/repositories");
    repositoryMetadata.put("optionsValueField", "fullName");
    repositoryMetadata.put("optionsLabelField", "fullName");
    repositoryMetadata.put("searchable", true);

    return List.of(
        new ActionDefinition(
            "github.issue_created",
            "New Issue Created",
            "Triggers when a new issue is created in a repository",
            List.of(
                new FieldDefinition(
                    "repository",
                    "Repository",
                    "select",
                    true,
                    "Select the repository to monitor (format: owner/repo)",
                    repositoryMetadata
                )
            )
        ),
        new ActionDefinition(
            "github.pr_created",
            "New Pull Request Created",
            "Triggers when a new pull request is created in a repository",
            List.of(
                new FieldDefinition(
                    "repository",
                    "Repository",
                    "select",
                    true,
                    "Select the repository to monitor (format: owner/repo)",
                    repositoryMetadata
                )
            )
        )
    );
}
```

**Update getReactions() method (optional but recommended for consistency):**
```java
@Override
public List<ReactionDefinition> getReactions() {
    // Create metadata for dynamic repository field
    Map<String, Object> repositoryMetadata = new HashMap<>();
    repositoryMetadata.put("dynamicOptionsEndpoint", "/api/services/github/repositories");
    repositoryMetadata.put("optionsValueField", "fullName");
    repositoryMetadata.put("optionsLabelField", "fullName");
    repositoryMetadata.put("searchable", true);

    return List.of(
        new ReactionDefinition(
            "github.create_issue",
            "Create Issue",
            "Create a new issue in a GitHub repository with templated content",
            List.of(
                new FieldDefinition(
                    "repository",
                    "Repository",
                    "select",
                    true,
                    "Select the target repository (format: owner/repo)",
                    repositoryMetadata
                ),
                new FieldDefinition(
                    "issueTitle",
                    "Issue Title",
                    "string",
                    true,
                    "Title of the issue (supports variable substitution)"
                ),
                new FieldDefinition(
                    "issueBody",
                    "Issue Body",
                    "text",
                    false,
                    "Body/description of the issue (supports variable substitution)"
                ),
                new FieldDefinition(
                    "labels",
                    "Labels",
                    "string",
                    false,
                    "Comma-separated list of labels (e.g., 'bug,urgent')"
                )
            )
        ),
        new ReactionDefinition(
            "github.create_pr",
            "Create Pull Request",
            "Create a new pull request with file commits in a GitHub repository",
            List.of(
                new FieldDefinition(
                    "repository",
                    "Repository",
                    "select",
                    true,
                    "Select the target repository (format: owner/repo)",
                    repositoryMetadata
                ),
                new FieldDefinition(
                    "prTitle",
                    "PR Title",
                    "string",
                    true,
                    "Title of the pull request (supports variable substitution)"
                ),
                new FieldDefinition(
                    "prBody",
                    "PR Body",
                    "text",
                    false,
                    "Description of the pull request (supports variable substitution)"
                ),
                new FieldDefinition(
                    "sourceBranch",
                    "Source Branch",
                    "string",
                    true,
                    "Branch name to create for the PR (e.g., 'feature/auto-update')"
                ),
                new FieldDefinition(
                    "targetBranch",
                    "Target Branch",
                    "string",
                    false,
                    "Target branch for the PR (defaults to 'main')"
                ),
                new FieldDefinition(
                    "filePath",
                    "File Path",
                    "string",
                    true,
                    "Path of the file to commit (e.g., 'README.md')"
                ),
                new FieldDefinition(
                    "fileContent",
                    "File Content",
                    "text",
                    true,
                    "Content to write to the file (supports variable substitution)"
                ),
                new FieldDefinition(
                    "commitMessage",
                    "Commit Message",
                    "string",
                    false,
                    "Commit message (defaults to PR title)"
                )
            )
        )
    );
}
```

**Add import:**
```java
import java.util.HashMap;
import java.util.Map;
```

**Design Rationale:**
- **Single field approach:** Replaces `repositoryOwner` + `repositoryName` with single `repository` field
- **Field type:** Changed from "string" to "select" to indicate dropdown UI
- **Metadata properties:**
  - `dynamicOptionsEndpoint`: Tells frontend where to fetch options
  - `optionsValueField`: Which field from API response to use as value ("fullName")
  - `optionsLabelField`: Which field to display in dropdown ("fullName")
  - `searchable`: Enables search/filter in dropdown (important for users with many repos)
- **Consistent pattern:** Same field definition used in both triggers and reactions

---

## Phase 7: Update GitHubActionConfig Model

### 7.1 Modify GitHubActionConfig.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/model/GitHubActionConfig.java`

**Update class:**
```java
package com.area.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Configuration for GitHub action triggers.
 * Supports two action types:
 * 1. issue_created - Triggers when a new issue is created in the repository
 * 2. pr_created - Triggers when a new pull request is created in the repository
 */
@Embeddable
public class GitHubActionConfig {

    @Column(name = "github_action_type", length = 32)
    private String actionType;

    // NEW: Single repository field in "owner/repo" format
    @Column(name = "github_repository", length = 255)
    private String repository;

    // DEPRECATED: Keep old fields for backward compatibility during migration
    @Column(name = "github_repository_owner")
    @Deprecated
    private String repositoryOwner;

    @Column(name = "github_repository_name")
    @Deprecated
    private String repositoryName;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    @Deprecated
    public String getRepositoryOwner() {
        // Fallback: if repository is set, parse owner from it
        if (repository != null && repository.contains("/")) {
            return repository.split("/")[0];
        }
        return repositoryOwner;
    }

    @Deprecated
    public void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
        // Auto-sync: if both owner and name are set, update repository field
        if (repositoryOwner != null && repositoryName != null) {
            this.repository = repositoryOwner + "/" + repositoryName;
        }
    }

    @Deprecated
    public String getRepositoryName() {
        // Fallback: if repository is set, parse name from it
        if (repository != null && repository.contains("/")) {
            String[] parts = repository.split("/", 2);
            return parts.length > 1 ? parts[1] : null;
        }
        return repositoryName;
    }

    @Deprecated
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        // Auto-sync: if both owner and name are set, update repository field
        if (repositoryOwner != null && repositoryName != null) {
            this.repository = repositoryOwner + "/" + repositoryName;
        }
    }

    /**
     * Get the full repository identifier in "owner/repo" format.
     * Supports both new (repository) and legacy (owner/name) formats.
     */
    public String getFullRepositoryName() {
        // Prefer new format
        if (repository != null && !repository.isBlank()) {
            return repository;
        }

        // Fallback to legacy format
        if (repositoryOwner == null || repositoryName == null) {
            return null;
        }
        return repositoryOwner + "/" + repositoryName;
    }

    /**
     * Parse and set repository from "owner/repo" format.
     * Also updates legacy fields for backward compatibility.
     */
    public void setFullRepositoryName(String fullName) {
        this.repository = fullName;

        // Also update legacy fields for backward compatibility
        if (fullName != null && fullName.contains("/")) {
            String[] parts = fullName.split("/", 2);
            this.repositoryOwner = parts[0];
            this.repositoryName = parts.length > 1 ? parts[1] : null;
        }
    }
}
```

**Design Rationale:**
- **Backward compatibility:** Keeps old fields marked as `@Deprecated` to support existing data
- **Dual read support:** Getter methods check new field first, fallback to legacy fields
- **Auto-sync on write:** Setting legacy fields updates the new `repository` field automatically
- **Graceful migration:** No data loss - existing Areas will continue to work
- **Helper methods:** `getFullRepositoryName()` and `setFullRepositoryName()` provide convenient access

---

### 7.2 Update GitHubReactionConfig.java (Similar Pattern)
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/model/GitHubReactionConfig.java`

**Apply same changes as GitHubActionConfig** for consistency across triggers and reactions.

---

## Phase 8: Update Action Executors

### 8.1 Update GitHubIssueActionExecutor.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/executor/GitHubIssueActionExecutor.java`

**Changes:** No changes required!

**Rationale:** The executor already uses `config.getRepositoryOwner()` and `config.getRepositoryName()`. Our backward-compatible getters in `GitHubActionConfig` will automatically parse from the new `repository` field if it's set.

**Testing note:** Verify that:
1. New Areas with `repository` field work correctly
2. Old Areas with `repositoryOwner`/`repositoryName` continue to work
3. The parsing logic in `getRepositoryOwner()` and `getRepositoryName()` handles edge cases (null values, malformed strings)

---

### 8.2 Update GitHubPullRequestActionExecutor.java
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/executor/GitHubPullRequestActionExecutor.java`

**Changes:** No changes required!

**Rationale:** Same as above - backward-compatible getters handle the transition automatically.

---

### 8.3 Update GitHubService Methods
**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/GitHubService.java`

**Changes:** No changes required!

**Rationale:** Service methods use `config.getRepositoryOwner()` and `config.getRepositoryName()` which now automatically parse from the new format.

**Optional enhancement:** Add validation to detect malformed repository strings:
```java
private void validateRepositoryFormat(String owner, String repo) {
    if (owner == null || owner.isBlank()) {
        throw new IllegalArgumentException("Repository owner cannot be empty");
    }
    if (repo == null || repo.isBlank()) {
        throw new IllegalArgumentException("Repository name cannot be empty");
    }
    if (owner.contains("/") || repo.contains("/")) {
        throw new IllegalArgumentException("Invalid repository format. Expected 'owner/repo'");
    }
}
```

---

## Phase 9: Database Migration Strategy

### 9.1 Migration Approach
Since this project uses **Spring Boot with JPA/Hibernate**, the schema changes can be handled through:

**Option A: Hibernate Auto DDL (Development/Simple Deployments)**
- Add `spring.jpa.hibernate.ddl-auto=update` in `application.properties`
- Hibernate will automatically add the new `github_repository` column
- Old columns remain for backward compatibility

**Option B: Flyway/Liquibase Migrations (Production-Grade)**

If using Flyway, create migration file:

**File:** `/home/heathcliff/Delivery/AREA/server/src/main/resources/db/migration/V2__add_github_repository_field.sql`

```sql
-- Add new repository column to areas table
ALTER TABLE areas
ADD COLUMN IF NOT EXISTS github_repository VARCHAR(255);

-- Migrate existing data from old format to new format
UPDATE areas
SET github_repository = CONCAT(github_repository_owner, '/', github_repository_name)
WHERE github_repository_owner IS NOT NULL
  AND github_repository_name IS NOT NULL
  AND github_repository IS NULL;

-- Note: We don't drop old columns yet for backward compatibility
-- They can be removed in a future migration after ensuring all code uses new field
```

**Design Rationale:**
- **Non-destructive:** Adds new column without removing old ones
- **Data migration:** Automatically populates new field from existing data
- **Backward compatible:** Old columns remain accessible during transition
- **Idempotent:** Uses `IF NOT EXISTS` and conditional updates

---

### 9.2 Future Cleanup Migration (Phase 2)
After confirming all systems use the new field (weeks/months later):

**File:** `V3__remove_legacy_github_fields.sql`
```sql
-- Remove deprecated columns after transition period
ALTER TABLE areas
DROP COLUMN IF EXISTS github_repository_owner,
DROP COLUMN IF EXISTS github_repository_name;
```

---

## Phase 10: Error Handling and Validation

### 10.1 Repository Format Validation
Add utility class for validation:

**File:** `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/util/GitHubRepositoryParser.java`

```java
package com.area.server.util;

/**
 * Utility class for parsing and validating GitHub repository identifiers
 */
public class GitHubRepositoryParser {

    private static final String REPOSITORY_REGEX = "^[a-zA-Z0-9-_.]+/[a-zA-Z0-9-_.]+$";

    /**
     * Parse repository string into owner and name components
     *
     * @param fullName Repository in "owner/repo" format
     * @return Array [owner, name]
     * @throws IllegalArgumentException if format is invalid
     */
    public static String[] parse(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Repository cannot be empty");
        }

        String[] parts = fullName.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid repository format. Expected 'owner/repo', got: " + fullName
            );
        }

        String owner = parts[0].trim();
        String name = parts[1].trim();

        if (owner.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException(
                "Repository owner and name cannot be empty. Got: " + fullName
            );
        }

        return new String[]{owner, name};
    }

    /**
     * Validate repository format without parsing
     */
    public static boolean isValid(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return false;
        }
        return fullName.matches(REPOSITORY_REGEX);
    }

    /**
     * Get owner from repository string
     */
    public static String getOwner(String fullName) {
        return parse(fullName)[0];
    }

    /**
     * Get name from repository string
     */
    public static String getName(String fullName) {
        return parse(fullName)[1];
    }
}
```

---

### 10.2 Update Area Controller Validation
Add validation when creating/updating Areas with GitHub actions:

```java
// In AreaController or similar
if (area.getGithubActionConfig() != null) {
    String repo = area.getGithubActionConfig().getRepository();
    if (repo != null && !GitHubRepositoryParser.isValid(repo)) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Invalid repository format. Expected 'owner/repo'"));
    }
}
```

---

## Phase 11: Testing Strategy

### 11.1 Unit Tests

**Test: GitHubRepositoryParser**
```java
@Test
void testParseValidRepository() {
    String[] parts = GitHubRepositoryParser.parse("octocat/Hello-World");
    assertEquals("octocat", parts[0]);
    assertEquals("Hello-World", parts[1]);
}

@Test
void testParseInvalidRepository() {
    assertThrows(IllegalArgumentException.class,
        () -> GitHubRepositoryParser.parse("invalid"));
}

@Test
void testIsValid() {
    assertTrue(GitHubRepositoryParser.isValid("owner/repo"));
    assertFalse(GitHubRepositoryParser.isValid("invalid"));
}
```

**Test: GitHubActionConfig Backward Compatibility**
```java
@Test
void testLegacyFieldsUpdateNewField() {
    GitHubActionConfig config = new GitHubActionConfig();
    config.setRepositoryOwner("owner");
    config.setRepositoryName("repo");

    assertEquals("owner/repo", config.getRepository());
    assertEquals("owner/repo", config.getFullRepositoryName());
}

@Test
void testNewFieldParsesToLegacyFields() {
    GitHubActionConfig config = new GitHubActionConfig();
    config.setRepository("owner/repo");

    assertEquals("owner", config.getRepositoryOwner());
    assertEquals("repo", config.getRepositoryName());
    assertEquals("owner/repo", config.getFullRepositoryName());
}
```

**Test: GitHubService.getUserRepositories**
```java
@Test
void testGetUserRepositoriesSuccess() {
    // Mock WebClient response
    // Verify filtering by permissions
    // Verify DTO mapping
}

@Test
void testGetUserRepositoriesWithPagination() {
    // Test pagination parameters
}

@Test
void testGetUserRepositoriesError() {
    // Test error handling
}
```

---

### 11.2 Integration Tests

**Test: Repository Endpoint**
```java
@Test
@WithMockUser
void testGetRepositoriesRequiresAuthentication() {
    // Test 401 when not authenticated
}

@Test
@WithMockUser
void testGetRepositoriesRequiresGitHubConnection() {
    // Test 404 when no GitHub connection exists
}

@Test
@WithMockUser
void testGetRepositoriesSuccess() {
    // Mock GitHub connection
    // Mock GitHub API response
    // Verify correct repository list returned
}
```

---

### 11.3 Manual Testing Checklist

1. **OAuth Flow**
   - [ ] Connect GitHub account
   - [ ] Verify connection saved with access token

2. **Repository Endpoint**
   - [ ] Call `/api/services/github/repositories` without auth → 401
   - [ ] Call without GitHub connection → 404
   - [ ] Call with valid connection → Returns repository list
   - [ ] Verify pagination works (page=2, per_page=50)
   - [ ] Verify only repos with push access are returned

3. **Frontend Integration** (if available)
   - [ ] Repository dropdown loads dynamically
   - [ ] Search/filter works in dropdown
   - [ ] Selecting repository saves in "owner/repo" format

4. **Trigger Execution**
   - [ ] Create new Area with repository field
   - [ ] Verify trigger checks correct repository
   - [ ] Create issue in repository → Trigger fires
   - [ ] Verify trigger context has correct data

5. **Backward Compatibility**
   - [ ] Existing Areas with old format continue to work
   - [ ] Update existing Area → Saves in new format
   - [ ] Verify no data loss during transition

6. **Error Cases**
   - [ ] Invalid access token → Proper error message
   - [ ] Repository format validation works
   - [ ] GitHub API rate limit handling

---

## Summary of File Changes

### New Files
1. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/dto/GitHubRepositoryDTO.java`
2. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/util/GitHubRepositoryParser.java`
3. `/home/heathcliff/Delivery/AREA/server/src/main/resources/db/migration/V2__add_github_repository_field.sql` (if using Flyway)

### Modified Files
1. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/dto/GitHubApiResponse.java`
   - Add `RepositoryResponse` nested class

2. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/GitHubService.java`
   - Add `getUserRepositories()` method
   - Add `mapToRepositoryDTO()` helper method

3. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/repository/ServiceConnectionRepository.java`
   - Add `findByUserAndType()` method
   - Add `findFirstByUserAndType()` method

4. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/ServiceConnectionService.java`
   - Add `findByUserAndType()` method
   - Add `findFirstByUserAndType()` method

5. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/controller/GitHubOAuthController.java`
   - Add `getRepositories()` endpoint method

6. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/FieldDefinition.java`
   - Add `options` field
   - Add `metadata` field
   - Add `SelectOption` nested class
   - Add new constructor

7. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/service/integration/GitHubIntegration.java`
   - Update `getActions()` to use single `repository` field with metadata
   - Update `getReactions()` similarly (optional)
   - Add imports for `HashMap` and `Map`

8. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/model/GitHubActionConfig.java`
   - Add `repository` field
   - Add backward-compatible getter/setter methods
   - Mark old fields as `@Deprecated`
   - Add `setFullRepositoryName()` helper

9. `/home/heathcliff/Delivery/AREA/server/src/main/java/com/area/server/model/GitHubReactionConfig.java`
   - Apply same changes as GitHubActionConfig

### No Changes Required
- GitHubIssueActionExecutor.java (backward-compatible getters handle new format)
- GitHubPullRequestActionExecutor.java (same as above)
- GitHub reaction executors (same as above)

---

## API Contract

### GET /api/services/github/repositories

**Description:** Fetch repositories accessible by the authenticated user

**Authentication:** Required (JWT token)

**Query Parameters:**
- `page` (optional): Page number, default: 1
- `per_page` (optional): Results per page, default: 100, max: 100

**Response 200 (Success):**
```json
{
  "status": "success",
  "data": [
    {
      "fullName": "octocat/Hello-World",
      "description": "My first repository",
      "private": false,
      "htmlUrl": "https://github.com/octocat/Hello-World"
    },
    {
      "fullName": "organization/project",
      "description": "Organization project",
      "private": true,
      "htmlUrl": "https://github.com/organization/project"
    }
  ]
}
```

**Response 401 (Unauthorized):**
```json
{
  "status": "error",
  "message": "Authentication required. Please log in."
}
```

**Response 404 (No Connection):**
```json
{
  "status": "error",
  "message": "No GitHub connection found. Please connect your GitHub account first."
}
```

**Response 500 (Server Error):**
```json
{
  "status": "error",
  "message": "Failed to fetch repositories: [error details]"
}
```

---

## Frontend Integration Guide

The frontend should:

1. **Detect dynamic field:**
   - Check if field has `type: "select"` and `metadata.dynamicOptionsEndpoint`

2. **Fetch options:**
   ```javascript
   const endpoint = field.metadata.dynamicOptionsEndpoint;
   const response = await fetch(endpoint, {
     headers: { Authorization: `Bearer ${token}` }
   });
   const { data: repositories } = await response.json();
   ```

3. **Populate dropdown:**
   ```javascript
   const options = repositories.map(repo => ({
     value: repo.fullName,
     label: repo.fullName,
     description: repo.description // Optional: show as subtitle
   }));
   ```

4. **Enable search:**
   - Use searchable dropdown component (e.g., Select2, React-Select)
   - Filter options client-side for responsive UX

5. **Handle errors:**
   - Show "Connect GitHub" button if 404
   - Show "Reconnect" button if 401
   - Show error message for 500

---

## Security Considerations

1. **Authentication:** All endpoints require user authentication
2. **Authorization:** Users can only access their own GitHub repositories (enforced by OAuth token)
3. **Token Security:** Access tokens never exposed to frontend (used server-side only)
4. **Input Validation:** Repository format validated to prevent injection attacks
5. **Rate Limiting:** GitHub API rate limits handled with retry logic
6. **Error Messages:** Sensitive information (tokens, internal errors) not exposed to users

---

## Performance Considerations

1. **Pagination:** Default 100 repos per page to reduce API calls
2. **Caching:** Consider adding cache for repository list (TTL: 5-10 minutes)
3. **Async Loading:** Frontend should fetch options asynchronously (not blocking form load)
4. **Permission Filtering:** Only repos with push access returned (reduces data transfer)
5. **Retry Logic:** Exponential backoff prevents hammering GitHub API on failures

---

## Migration Timeline

### Phase 1 (Immediate)
- Deploy all code changes
- New Areas use new `repository` field
- Old Areas continue working via backward compatibility

### Phase 2 (After 1-2 weeks)
- Monitor logs for any issues
- Verify all new Areas use new format
- Create data migration script to update old Areas

### Phase 3 (After 1-2 months)
- Deploy cleanup migration to remove deprecated fields
- Remove `@Deprecated` annotations
- Simplify GitHubActionConfig code

---

## Rollback Plan

If issues arise:
1. **Code rollback:** Previous version still works (no breaking changes)
2. **Database rollback:** Old columns remain intact
3. **Data integrity:** No data loss during transition
4. **Frontend compatibility:** Can switch back to text inputs if needed

The backward-compatible design ensures zero-downtime migration.
