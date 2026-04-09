package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Register, login, logout and token verification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user — returns token + user on success")
    public ResponseEntity<ApiResponse<Object>> register(
            @RequestBody Map<String, String> body) {

        String email    = body.get("email");
        String password = body.get("password");
        String username = body.get("username");

        Map<String, Object> result = authService.register(email, password, username);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error((String) result.get("message")));
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email + password — returns token + user")
    public ResponseEntity<ApiResponse<Object>> login(
            @RequestBody Map<String, String> body) {

        String email    = body.get("email");
        String password = body.get("password");

        Map<String, Object> result = authService.login(email, password);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.status(401)
                .body(ApiResponse.error((String) result.get("message")));
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Logout — revokes the session token")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> result = authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── GET /api/auth/verify ──────────────────────────────────────────────────

    @GetMapping("/verify")
    @Operation(summary = "Verify a session token — returns valid + user if still active")
    public ResponseEntity<ApiResponse<Object>> verify(HttpServletRequest request) {
        String token  = extractToken(request);
        Map<String, Object> result = authService.verifyToken(token);

        if (Boolean.TRUE.equals(result.get("valid"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.status(401)
                .body(ApiResponse.error("Invalid or expired token"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Extracts the Bearer token from the Authorization header,
     * or falls back to the "token" query param for convenience.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        String param = request.getParameter("token");
        return param != null ? param.trim() : null;
    }
}