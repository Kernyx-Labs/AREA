package com.area.server.dto.auth;

import com.area.server.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Response DTO for user information.
 * Used in authentication responses and user profile endpoints.
 */
public class UserResponse {

    private Long id;
    private String email;
    private String username;
    private String fullName;
    private boolean emailVerified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String username, String fullName, boolean emailVerified, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    /**
     * Create UserResponse from User entity.
     *
     * @param user the user entity
     * @return UserResponse DTO
     */
    public static UserResponse fromUser(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getFullName(),
            user.isEmailVerified(),
            user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
