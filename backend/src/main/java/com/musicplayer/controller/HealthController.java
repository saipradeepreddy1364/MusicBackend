package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Service health checks")
public class HealthController {

    private final JioSaavnService jiosaavnService;

    public HealthController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    @GetMapping
    @Operation(summary = "Backend health status")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP", "service", "music-player-backend")));
    }

    @GetMapping("/saavn")
    @Operation(summary = "Check connectivity to JioSaavn upstream API")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkSaavn() {
        try {
            Map<String, Object> result = jiosaavnService.searchSongs("test", 1, 1);
            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(502).body(ApiResponse.error(
                        "JioSaavn API returned empty response"));
            }
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("status", "UP",
                           "upstream", "https://jiosaavn-api.pradeepreddypalagiri.workers.dev/api")));
        } catch (Exception ex) {
            return ResponseEntity.status(502).body(ApiResponse.error(
                    "JioSaavn API unreachable: " + ex.getMessage()));
        }
    }
}