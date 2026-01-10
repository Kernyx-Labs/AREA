package com.area.server.repository;

import com.area.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 * Provides database access methods for user authentication and management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address.
     * Used for login and email uniqueness validation.
     *
     * @param email the user's email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username.
     * Used for username uniqueness validation.
     *
     * @param username the user's username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email already exists in the database.
     * Used for registration validation.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists in the database.
     * Used for registration validation.
     *
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);
}
