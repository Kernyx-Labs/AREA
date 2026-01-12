package com.area.server.security;

import com.area.server.model.User;
import com.area.server.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details from the database for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user by email address (username in Spring Security context).
     * This method is called by Spring Security during authentication.
     *
     * @param email the user's email address
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
            .accountLocked(user.isAccountLocked())
            .disabled(!user.isEmailVerified() && user.getPasswordHash() != null)
            .build();
    }

    /**
     * Load user entity by email.
     * Used by authentication services to get the full user object.
     *
     * @param email the user's email address
     * @return User entity
     * @throws UsernameNotFoundException if user not found
     */
    public User loadUserEntityByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
