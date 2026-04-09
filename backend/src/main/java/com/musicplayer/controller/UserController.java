package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Liked songs per user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── GET /api/user/liked ───────────────────────────────────────────────────

    @GetMapping("/liked")
    @Operation(summary = "Get all liked songs for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> getLikedSongs(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> result = userService.getLikedSongs(token);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result.get("data")));
        }
        return ResponseEntity.status(401).body(ApiResponse.error((String) result.get("message")));
    }

    // ── POST /api/user/like ───────────────────────────────────────────────────

    @PostMapping("/like")
    @Operation(summary = "Like a song for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> likeSong(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        String token      = extractToken(request);
        String songId     = body.get("songId");
        String songTitle  = body.getOrDefault("songTitle", "");
        String songImage  = body.getOrDefault("songImage", "");

        Map<String, Object> result = userService.likeSong(token, songId, songTitle, songImage);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.status(401).body(ApiResponse.error((String) result.get("message")));
    }

    // ── DELETE /api/user/unlike ───────────────────────────────────────────────

    @DeleteMapping("/unlike")
    @Operation(summary = "Unlike a song for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> unlikeSong(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        String token  = extractToken(request);
        String songId = body.get("songId");

        Map<String, Object> result = userService.unlikeSong(token, songId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.status(401).body(ApiResponse.error((String) result.get("message")));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}