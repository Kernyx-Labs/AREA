package com.area.server.util;

import com.area.server.model.GitHubActionConfig;
import com.area.server.model.GitHubReactionConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for GitHub action and reaction configurations.
 * Ensures all required fields are present and valid.
 */
@Component
public class GitHubValidator {

    // GitHub repository name pattern: alphanumeric, hyphens, underscores, dots
    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    // GitHub username/org pattern: alphanumeric and hyphens
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

    // Branch name pattern: alphanumeric, hyphens, underscores, slashes
    // Must start with alphanumeric, no ".." sequences, no backslashes
    private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^(?!.*\\.\\.)(?!.*\\\\)[a-zA-Z0-9][a-zA-Z0-9._/-]*$");

    /**
     * Validate GitHub action configuration
     */
    public ValidationResult validateActionConfig(GitHubActionConfig config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("GitHub action configuration is required");
            return new ValidationResult(false, errors);
        }

        // Validate action type
        if (config.getActionType() == null || config.getActionType().isBlank()) {
            errors.add("GitHub action type is required");
        } else if (!isValidActionType(config.getActionType())) {
            errors.add("Invalid GitHub action type: " + config.getActionType() +
                      ". Valid types: issue_created, pr_created");
        }

        // Validate repository owner
        if (config.getRepositoryOwner() == null || config.getRepositoryOwner().isBlank()) {
            errors.add("GitHub repository owner is required");
        } else if (!USERNAME_PATTERN.matcher(config.getRepositoryOwner()).matches()) {
            errors.add("Invalid GitHub repository owner format. Use only alphanumeric characters and hyphens.");
        }

        // Validate repository name
        if (config.getRepositoryName() == null || config.getRepositoryName().isBlank()) {
            errors.add("GitHub repository name is required");
        } else if (!REPO_NAME_PATTERN.matcher(config.getRepositoryName()).matches()) {
            errors.add("Invalid GitHub repository name format. Use only alphanumeric characters, hyphens, underscores, and dots.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate GitHub reaction configuration
     */
    public ValidationResult validateReactionConfig(GitHubReactionConfig config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("GitHub reaction configuration is required");
            return new ValidationResult(false, errors);
        }

        // Validate reaction type
        if (config.getReactionType() == null || config.getReactionType().isBlank()) {
            errors.add("GitHub reaction type is required");
        } else if (!isValidReactionType(config.getReactionType())) {
            errors.add("Invalid GitHub reaction type: " + config.getReactionType() +
                      ". Valid types: create_issue, create_pr");
        }

        // Validate repository owner
        if (config.getRepositoryOwner() == null || config.getRepositoryOwner().isBlank()) {
            errors.add("GitHub repository owner is required");
        } else if (!USERNAME_PATTERN.matcher(config.getRepositoryOwner()).matches()) {
            errors.add("Invalid GitHub repository owner format. Use only alphanumeric characters and hyphens.");
        }

        // Validate repository name
        if (config.getRepositoryName() == null || config.getRepositoryName().isBlank()) {
            errors.add("GitHub repository name is required");
        } else if (!REPO_NAME_PATTERN.matcher(config.getRepositoryName()).matches()) {
            errors.add("Invalid GitHub repository name format. Use only alphanumeric characters, hyphens, underscores, and dots.");
        }

        // Validate reaction-specific fields
        if ("create_issue".equals(config.getReactionType())) {
            errors.addAll(validateCreateIssueConfig(config));
        } else if ("create_pr".equals(config.getReactionType())) {
            errors.addAll(validateCreatePrConfig(config));
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate create_issue specific fields
     */
    private List<String> validateCreateIssueConfig(GitHubReactionConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getIssueTitle() == null || config.getIssueTitle().isBlank()) {
            errors.add("GitHub issue title is required");
        } else if (config.getIssueTitle().length() > 256) {
            errors.add("GitHub issue title is too long (max 256 characters)");
        }

        if (config.getIssueBody() != null && config.getIssueBody().length() > 65536) {
            errors.add("GitHub issue body is too long (max 65536 characters)");
        }

        // Validate labels format (comma-separated)
        if (config.getLabels() != null && !config.getLabels().isBlank()) {
            String[] labels = config.getLabels().split(",");
            for (String label : labels) {
                String trimmed = label.trim();
                if (trimmed.length() > 50) {
                    errors.add("GitHub label is too long: " + trimmed + " (max 50 characters)");
                }
            }
        }

        return errors;
    }

    /**
     * Validate create_pr specific fields
     */
    private List<String> validateCreatePrConfig(GitHubReactionConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getPrTitle() == null || config.getPrTitle().isBlank()) {
            errors.add("GitHub PR title is required");
        } else if (config.getPrTitle().length() > 256) {
            errors.add("GitHub PR title is too long (max 256 characters)");
        }

        if (config.getPrBody() != null && config.getPrBody().length() > 65536) {
            errors.add("GitHub PR body is too long (max 65536 characters)");
        }

        if (config.getSourceBranch() == null || config.getSourceBranch().isBlank()) {
            errors.add("GitHub source branch is required");
        } else if (!BRANCH_NAME_PATTERN.matcher(config.getSourceBranch()).matches()) {
            errors.add("Invalid GitHub source branch format");
        }

        // Target branch defaults to "main" if not specified
        if (config.getTargetBranch() != null && !config.getTargetBranch().isBlank()) {
            if (!BRANCH_NAME_PATTERN.matcher(config.getTargetBranch()).matches()) {
                errors.add("Invalid GitHub target branch format");
            }
        }

        if (config.getFilePath() == null || config.getFilePath().isBlank()) {
            errors.add("GitHub file path is required");
        } else if (config.getFilePath().contains("..") ||
                   config.getFilePath().startsWith("/") ||
                   config.getFilePath().startsWith("~") ||
                   config.getFilePath().matches(".*%[0-9a-fA-F]{2}.*")) {
            errors.add("GitHub file path must be a relative path without traversal sequences");
        }

        if (config.getFileContent() != null && config.getFileContent().length() > 100 * 1024) {
            errors.add("GitHub file content is too large (max 100KB)");
        }

        // Commit message is optional, defaults to PR title if not provided
        if (config.getCommitMessage() != null && !config.getCommitMessage().isBlank()) {
            if (config.getCommitMessage().length() > 256) {
                errors.add("GitHub commit message is too long (max 256 characters)");
            }
        }

        return errors;
    }

    /**
     * Check if action type is valid
     */
    private boolean isValidActionType(String actionType) {
        return "issue_created".equals(actionType) || "pr_created".equals(actionType);
    }

    /**
     * Check if reaction type is valid
     */
    private boolean isValidReactionType(String reactionType) {
        return "create_issue".equals(reactionType) || "create_pr".equals(reactionType);
    }

    /**
     * Result of validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
