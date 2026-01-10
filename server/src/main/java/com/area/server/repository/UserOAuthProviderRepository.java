package com.area.server.repository;

import com.area.server.model.User;
import com.area.server.model.UserOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserOAuthProvider entity.
 * Manages OAuth2 provider connections for users.
 */
@Repository
public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProvider, Long> {

    /**
     * Find OAuth provider connection by provider and provider user ID.
     * Used when authenticating with OAuth to find existing user accounts.
     *
     * @param provider the OAuth provider (GOOGLE, GITHUB, etc.)
     * @param providerUserId the user's ID at the provider
     * @return Optional containing the OAuth provider connection if found
     */
    Optional<UserOAuthProvider> findByProviderAndProviderUserId(
        UserOAuthProvider.Provider provider,
        String providerUserId
    );

    /**
     * Find all OAuth connections for a specific user.
     *
     * @param user the user
     * @return List of OAuth provider connections
     */
    List<UserOAuthProvider> findByUser(User user);

    /**
     * Find OAuth provider connection by user and provider.
     *
     * @param user the user
     * @param provider the OAuth provider
     * @return Optional containing the OAuth provider connection if found
     */
    Optional<UserOAuthProvider> findByUserAndProvider(User user, UserOAuthProvider.Provider provider);
}
