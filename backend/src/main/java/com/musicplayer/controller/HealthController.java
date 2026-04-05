package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health checks")
public class HealthController {

    private final WebClient jiosaavnClient;

    // ✅ FIXED (separate declaration properly)
    @Value("${saavn.base-url:https://saavn.me}")
    private String saavnBaseUrl;

    @GetMapping
    @Operation(summary = "Backend health status")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP", "service", "music-player-backend")));
    }

    @GetMapping("/saavn")
    @Operation(summary = "Check connectivity to saavn API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSaavn() {
        try {
            jiosaavnClient.get()
                    .uri(u -> u.path("/search/songs")
                            .queryParam("query", "test")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("status", "UP", "upstream", saavnBaseUrl)));

        } catch (Exception ex) {
            return ResponseEntity.status(502).body(ApiResponse.error(
                    "saavn API unreachable: " + ex.getMessage()));
        }
    }
}