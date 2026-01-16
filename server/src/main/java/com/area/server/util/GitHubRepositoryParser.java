package com.area.server.util;

/**
 * Utility class for parsing and validating GitHub repository identifiers.
 * Repositories are expected in "owner/repo" format.
 */
public class GitHubRepositoryParser {

    private static final String REPOSITORY_REGEX = "^[a-zA-Z0-9-_.]+/[a-zA-Z0-9-_.]+$";

    /**
     * Parse a repository identifier in "owner/repo" format
     *
     * @param fullName The full repository name (e.g., "octocat/hello-world")
     * @return Array with [owner, name]
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String[] parse(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Repository cannot be empty");
        }

        String[] parts = fullName.split("/", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid repository format. Expected 'owner/repo', got: " + fullName
            );
        }

        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    /**
     * Validate a repository identifier format
     *
     * @param fullName The full repository name to validate
     * @return true if the format is valid, false otherwise
     */
    public static boolean isValid(String fullName) {
        return fullName != null && fullName.matches(REPOSITORY_REGEX);
    }

    /**
     * Extract the owner from a full repository name
     *
     * @param fullName The full repository name (e.g., "octocat/hello-world")
     * @return The owner portion (e.g., "octocat")
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String getOwner(String fullName) {
        return parse(fullName)[0];
    }

    /**
     * Extract the repository name from a full repository identifier
     *
     * @param fullName The full repository name (e.g., "octocat/hello-world")
     * @return The repository name portion (e.g., "hello-world")
     * @throws IllegalArgumentException if the format is invalid
     */
    public static String getName(String fullName) {
        return parse(fullName)[1];
    }

    /**
     * Combine owner and repository name into full identifier
     *
     * @param owner The repository owner
     * @param name The repository name
     * @return The combined "owner/repo" identifier
     * @throws IllegalArgumentException if either parameter is null or blank
     */
    public static String combine(String owner, String name) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repository name cannot be empty");
        }
        return owner.trim() + "/" + name.trim();
    }
}
