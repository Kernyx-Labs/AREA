package com.area.server.repository;

import com.area.server.model.RefreshToken;
import com.area.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity.
 * Manages JWT refresh tokens for persistent user sessions.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by its token string.
     *
     * @param token the refresh token string
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a specific user.
     *
     * @param user the user
     * @return List of refresh tokens
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Find all valid (non-revoked, non-expired) refresh tokens for a user.
     *
     * @param user the user
     * @param now current timestamp
     * @return List of valid refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = ?1 AND rt.revoked = false AND rt.expiresAt > ?2")
    List<RefreshToken> findValidTokensByUser(User user, Instant now);

    /**
     * Delete all expired refresh tokens.
     * Should be called periodically to clean up the database.
     *
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < ?1")
    int deleteExpiredTokens(Instant now);

    /**
     * Revoke all refresh tokens for a user.
     * Used when user logs out from all devices.
     *
     * @param user the user
     * @param now current timestamp
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = ?2 WHERE rt.user = ?1 AND rt.revoked = false")
    void revokeAllUserTokens(User user, Instant now);
}
