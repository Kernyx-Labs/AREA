package com.area.server.service;

import com.area.server.dto.auth.*;
import com.area.server.exception.AuthenticationException;
import com.area.server.exception.InvalidTokenException;
import com.area.server.exception.UserAlreadyExistsException;
import com.area.server.model.RefreshToken;
import com.area.server.model.User;
import com.area.server.repository.RefreshTokenRepository;
import com.area.server.repository.UserRepository;
import com.area.server.security.JwtService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Service handling authentication operations including registration, login,
 * token refresh, and logout functionality.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user with email and password.
     *
     * @param request registration request with user details
     * @return authentication response with tokens and user info
     * @throws UserAlreadyExistsException if email or username already exists
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        validatePassword(request.getPassword());

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
            request.getEmail(),
            request.getUsername(),
            hashedPassword,
            request.getFullName()
        );

        user = userRepository.save(user);
        logger.info("Successfully registered user with ID: {}", user.getId());

        return generateAuthResponse(user, null);
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param request login request with credentials
     * @return authentication response with tokens and user info
     * @throws AuthenticationException if credentials are invalid or account is locked
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new AuthenticationException("Account is locked due to multiple failed login attempts");
        }

        if (user.getPasswordHash() == null) {
            throw new AuthenticationException("This account uses OAuth login. Please login with your OAuth provider.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationException("Invalid email or password");
        }

        handleSuccessfulLogin(user);

        logger.info("Successful login for user ID: {}", user.getId());
        return generateAuthResponse(user, null);
    }

    /**
     * Refresh access token using a valid refresh token.
     *
     * @param request refresh token request
     * @return authentication response with new access token
     * @throws InvalidTokenException if refresh token is invalid or expired
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Attempting to refresh access token");

        if (!jwtService.validateToken(request.getRefreshToken(), "refresh")) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is revoked or expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        logger.info("Successfully refreshed access token for user ID: {}", user.getId());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(request.getRefreshToken());
        response.setExpiresIn(jwtService.getAccessTokenExpiration() / 1000);
        response.setUser(UserResponse.fromUser(user));

        return response;
    }

    /**
     * Logout a user by revoking their refresh token.
     *
     * @param refreshTokenString the refresh token to revoke
     * @throws InvalidTokenException if refresh token not found
     */
    @Transactional
    public void logout(String refreshTokenString) {
        logger.info("Attempting to logout user");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        logger.info("Successfully logged out user ID: {}", refreshToken.getUser().getId());
    }

    /**
     * Generate authentication response with JWT tokens.
     *
     * @param user the authenticated user
     * @param deviceInfo optional device information
     * @return authentication response
     */
    private AuthResponse generateAuthResponse(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);

        Instant refreshTokenExpiry = Instant.now()
            .plusMillis(jwtService.getRefreshTokenExpiration());

        RefreshToken refreshToken = new RefreshToken(
            user,
            refreshTokenString,
            refreshTokenExpiry,
            deviceInfo
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            refreshTokenString,
            jwtService.getAccessTokenExpiration() / 1000,
            UserResponse.fromUser(user)
        );
    }

    /**
     * Handle successful login by resetting failed attempts and updating last login time.
     *
     * @param user the user who successfully logged in
     */
    private void handleSuccessfulLogin(User user) {
        user.resetFailedLoginAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * Handle failed login by incrementing failed attempts counter.
     * Locks account after 5 failed attempts.
     *
     * @param user the user who failed to login
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();
        userRepository.save(user);

        logger.warn("Failed login attempt for user ID: {}. Total attempts: {}",
            user.getId(), user.getFailedLoginAttempts());
    }

    /**
     * Validate password meets security requirements.
     * Requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters long and contain at least one uppercase letter, " +
                "one lowercase letter, one digit, and one special character (@$!%*?&)"
            );
        }
    }
}
