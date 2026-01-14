package com.area.server.service.integration;

import com.area.server.model.ServiceConnection;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub integration service providing repository monitoring and management.
 * Implements the ServiceIntegration interface to enable auto-discovery.
 *
 * GitHub uses OAuth 2.0 authentication and provides both actions (triggers)
 * for monitoring repository events and reactions for creating issues and pull requests.
 */
@Service
public class GitHubIntegration implements ServiceIntegration {

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.GITHUB;
    }

    @Override
    public String getName() {
        return "GitHub";
    }

    @Override
    public String getDescription() {
        return "Monitor repositories and automate issue/PR creation workflows";
    }

    @Override
    public List<ActionDefinition> getActions() {
        // Metadata for dynamic repository selection
        Map<String, Object> repositoryMetadata = new HashMap<>();
        repositoryMetadata.put("dynamicOptionsEndpoint", "/api/services/github/repositories");
        repositoryMetadata.put("optionsValueField", "full_name");
        repositoryMetadata.put("optionsLabelField", "full_name");
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

    @Override
    public List<ReactionDefinition> getReactions() {
        // Metadata for dynamic repository selection
        Map<String, Object> repositoryMetadata = new HashMap<>();
        repositoryMetadata.put("dynamicOptionsEndpoint", "/api/services/github/repositories");
        repositoryMetadata.put("optionsValueField", "full_name");
        repositoryMetadata.put("optionsLabelField", "full_name");
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

    @Override
    public boolean requiresAuthentication() {
        return true; // GitHub requires OAuth 2.0 authentication
    }
}

