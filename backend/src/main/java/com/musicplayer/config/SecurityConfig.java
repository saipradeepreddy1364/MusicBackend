package com.musicplayer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    /**
     * Provides BCrypt for password hashing in AuthService.
     *
     * spring-security-crypto is included in pom.xml but Spring Security's
     * HTTP filter chain is NOT active — all endpoints remain open and auth
     * is handled manually via session tokens in AuthController / AuthService.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}