package com.area.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class GmailApiResponse {

    public static class MessageListResponse {
        private List<MessageRef> messages;
        private String nextPageToken;
        private Integer resultSizeEstimate;

        public List<MessageRef> getMessages() {
            return messages;
        }

        public void setMessages(List<MessageRef> messages) {
            this.messages = messages;
        }

        public String getNextPageToken() {
            return nextPageToken;
        }

        public void setNextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
        }

        public Integer getResultSizeEstimate() {
            return resultSizeEstimate;
        }

        public void setResultSizeEstimate(Integer resultSizeEstimate) {
            this.resultSizeEstimate = resultSizeEstimate;
        }
    }

    public static class MessageRef {
        private String id;
        private String threadId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
    }

    public static class MessageDetail {
        private String id;
        private String threadId;
        private List<String> labelIds;
        private String snippet;
        private Payload payload;
        private Long internalDate;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public List<String> getLabelIds() {
            return labelIds;
        }

        public void setLabelIds(List<String> labelIds) {
            this.labelIds = labelIds;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public Payload getPayload() {
            return payload;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }

        public Long getInternalDate() {
            return internalDate;
        }

        public void setInternalDate(Long internalDate) {
            this.internalDate = internalDate;
        }
    }

    public static class Payload {
        private List<Header> headers;

        public List<Header> getHeaders() {
            return headers;
        }

        public void setHeaders(List<Header> headers) {
            this.headers = headers;
        }
    }

    public static class Header {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
