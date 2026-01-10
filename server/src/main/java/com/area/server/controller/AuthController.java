package com.area.server.controller;

import com.area.server.dto.auth.*;
import com.area.server.dto.response.ApiResponse;
import com.area.server.security.CustomUserDetailsService;
import com.area.server.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Handles user registration, login, token refresh, logout, and current user retrieval.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(AuthService authService, CustomUserDetailsService userDetailsService) {
        this.authService = authService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Register a new user account.
     *
     * POST /auth/register
     * Body: { "email", "username", "password", "fullName" }
     *
     * @param request registration details
     * @return authentication response with JWT tokens
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * Login with email and password.
     *
     * POST /auth/login
     * Body: { "email", "password" }
     *
     * @param request login credentials
     * @return authentication response with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Refresh access token using a valid refresh token.
     *
     * POST /auth/refresh
     * Body: { "refreshToken" }
     *
     * @param request refresh token
     * @return authentication response with new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Token refresh request received");
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    /**
     * Logout by revoking the refresh token.
     *
     * POST /auth/logout
     * Body: { "refreshToken" }
     *
     * @param request refresh token to revoke
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Logout request received");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    /**
     * Get current authenticated user information.
     *
     * GET /auth/me
     * Requires: Valid JWT token in Authorization header
     *
     * @return current user information
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        logger.info("Current user request for email: {}", email);

        var user = userDetailsService.loadUserEntityByEmail(email);
        UserResponse userResponse = UserResponse.fromUser(user);

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
