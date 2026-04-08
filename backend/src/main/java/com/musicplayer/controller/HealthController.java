package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class HealthController {

    private final JioSaavnService jiosaavnService;

    public HealthController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, String>>> root() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP", "service", "music-player-backend")));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP", "service", "music-player-backend")));
    }

    @GetMapping("/health/saavn")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkSaavn() {
        try {
            Map<String, Object> result = jiosaavnService.searchSongs("test", 1, 1);
            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(502).body(ApiResponse.error("Empty response"));
            }
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("status", "UP")));
        } catch (Exception ex) {
            return ResponseEntity.status(502).body(ApiResponse.error(ex.getMessage()));
        }
    }
}