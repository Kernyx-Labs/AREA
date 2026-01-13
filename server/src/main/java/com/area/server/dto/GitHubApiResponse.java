package com.area.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTOs for GitHub API calls
 */
public class GitHubApiResponse {

    /**
     * Response from creating a GitHub issue
     */
    public static class CreateIssueResponse {
        private Long number;
        private String title;
        @JsonProperty("html_url")
        private String htmlUrl;
        private String state;

        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    /**
     * Response from creating a GitHub pull request
     */
    public static class CreatePullRequestResponse {
        private Long number;
        private String title;
        @JsonProperty("html_url")
        private String htmlUrl;
        private String state;

        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    /**
     * Response from getting a Git reference (branch)
     */
    public static class GitRefResponse {
        private String ref;
        private GitObject object;

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public GitObject getObject() {
            return object;
        }

        public void setObject(GitObject object) {
            this.object = object;
        }

        public static class GitObject {
            private String sha;
            private String type;

            public String getSha() {
                return sha;
            }

            public void setSha(String sha) {
                this.sha = sha;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }
    }

    /**
     * Response from creating or updating file content
     */
    public static class FileContentResponse {
        private FileContent content;
        private FileCommit commit;

        public FileContent getContent() {
            return content;
        }

        public void setContent(FileContent content) {
            this.content = content;
        }

        public FileCommit getCommit() {
            return commit;
        }

        public void setCommit(FileCommit commit) {
            this.commit = commit;
        }

        public static class FileContent {
            private String name;
            private String path;
            private String sha;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public String getSha() {
                return sha;
            }

            public void setSha(String sha) {
                this.sha = sha;
            }
        }

        public static class FileCommit {
            private String sha;
            private String message;

            public String getSha() {
                return sha;
            }

            public void setSha(String sha) {
                this.sha = sha;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }
        }
    }

    /**
     * Response from GitHub OAuth token exchange
     */
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        private String scope;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    /**
     * Response from GitHub user API
     */
    public static class UserResponse {
        private String login;
        private String name;
        private String email;
        @JsonProperty("html_url")
        private String htmlUrl;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }
    }

    /**
     * Generic error response from GitHub API
     */
    public static class ErrorResponse {
        private String message;
        @JsonProperty("documentation_url")
        private String documentationUrl;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDocumentationUrl() {
            return documentationUrl;
        }

        public void setDocumentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
        }
    }
}
