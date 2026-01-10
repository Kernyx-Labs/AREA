package com.area.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding using BCrypt.
 * BCrypt automatically salts passwords and uses a configurable work factor for security.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates a BCrypt password encoder bean with strength 12.
     * Higher strength means more secure but slower hashing.
     * Strength 12 is a good balance for production use.
     *
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
