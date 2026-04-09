package com.musicplayer.service;

import com.musicplayer.model.Session;
import com.musicplayer.model.User;
import com.musicplayer.repository.SessionRepository;
import com.musicplayer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository        userRepository;
    private final SessionRepository     sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom          secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository    = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Register a new user.
     * Returns a map with: success, message, token, user (id/email/username).
     */
    @Transactional
    public Map<String, Object> register(String email, String password, String username) {
        if (email == null || email.isBlank())    return error("Email is required");
        if (password == null || password.isBlank()) return error("Password is required");
        if (username == null || username.isBlank()) return error("Username is required");
        if (password.length() < 6) return error("Password must be at least 6 characters");

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            return error("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user = userRepository.save(user);

        log.info("register | new user id={} email={}", user.getId(), user.getEmail());

        // Auto-login: create session immediately after registration
        String token   = generateToken();
        Session session = new Session();
        session.setUser(user);
        session.setToken(token);
        sessionRepository.save(session);

        return success(token, user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> login(String email, String password) {
        if (email == null || password == null) return error("Email and password are required");

        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) return error("Invalid email or password");

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return error("Invalid email or password");
        }

        // Clean up expired sessions for this user before creating a new one
        sessionRepository.deleteExpiredSessions(java.time.Instant.now());

        String token    = generateToken();
        Session session = new Session();
        session.setUser(user);
        session.setToken(token);
        sessionRepository.save(session);

        log.info("login | user id={}", user.getId());
        return success(token, user);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> logout(String token) {
        if (token == null || token.isBlank()) return Map.of("success", true);
        sessionRepository.deleteByToken(token);
        log.info("logout | token revoked");
        return Map.of("success", true);
    }

    // ── Verify token ──────────────────────────────────────────────────────────

    public Map<String, Object> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Map.of("valid", false);
        }
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) return Map.of("valid", false);

        Session session = sessionOpt.get();
        if (session.isExpired()) {
            sessionRepository.delete(session);
            return Map.of("valid", false);
        }

        User user = session.getUser();
        return Map.of(
            "valid", true,
            "user",  userDto(user)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<String, Object> success(String token, User user) {
        return Map.of(
            "success", true,
            "token",   token,
            "user",    userDto(user)
        );
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }

    private Map<String, Object> userDto(User user) {
        return Map.of(
            "id",       user.getId().toString(),
            "email",    user.getEmail(),
            "username", user.getUsername()
        );
    }
}