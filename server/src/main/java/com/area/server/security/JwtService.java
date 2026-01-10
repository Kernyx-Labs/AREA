package com.area.server.security;

import com.area.server.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT token generation and validation.
 * Handles both access tokens (short-lived) and refresh tokens (long-lived).
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${app.jwt.issuer:area-platform}")
    private String issuer;

    /**
     * Generate an access token for a user.
     * Access tokens are short-lived (1 hour) and used for API authentication.
     *
     * @param user the user to generate token for
     * @return JWT access token string
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("username", user.getUsername());
        claims.put("type", "access");

        return generateToken(claims, user.getEmail(), accessTokenExpiration);
    }

    /**
     * Generate a refresh token for a user.
     * Refresh tokens are long-lived (7 days) and used to obtain new access tokens.
     *
     * @param user the user to generate token for
     * @return JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        return generateToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    /**
     * Generate a JWT token with specified claims and expiration.
     *
     * @param claims additional claims to include in the token
     * @param subject the subject (typically email or username)
     * @param expirationMs expiration time in milliseconds
     * @return JWT token string
     */
    private String generateToken(Map<String, Object> claims, String subject, long expirationMs) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extract email (subject) from JWT token.
     *
     * @param token JWT token string
     * @return email address
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token string
     * @return user ID
     */
    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract token type from JWT token.
     *
     * @param token JWT token string
     * @return token type (access or refresh)
     */
    public String extractTokenType(String token) {
        Claims claims = extractClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * Validate a JWT token.
     * Checks signature, expiration, and token type.
     *
     * @param token JWT token string
     * @param expectedType expected token type (access or refresh)
     * @return true if token is valid
     */
    public boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = extractClaims(token);
            String tokenType = claims.get("type", String.class);

            if (!expectedType.equals(tokenType)) {
                logger.warn("Invalid token type. Expected: {}, Got: {}", expectedType, tokenType);
                return false;
            }

            return !isTokenExpired(claims);
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from a JWT token.
     *
     * @param token JWT token string
     * @return Claims object
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Check if token is expired.
     *
     * @param claims token claims
     * @return true if token is expired
     */
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * Get the signing key for JWT tokens.
     * Uses the secret from application properties.
     *
     * @return SecretKey for signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get access token expiration time in milliseconds.
     *
     * @return expiration time in ms
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration time in milliseconds.
     *
     * @return expiration time in ms
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
