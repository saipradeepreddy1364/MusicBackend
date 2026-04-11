package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/songs")
@Validated
@Tag(name = "Songs", description = "Get songs and suggestions")
public class SongController {

    private final JioSaavnService jiosaavnService;

    public SongController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    /** Get song details by ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Get song details by ID")
    public ResponseEntity<ApiResponse<Object>> getSong(
            @PathVariable @Parameter(description = "JioSaavn song ID") String id) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getSongById(id)));
    }

    /** Get song suggestions / related songs. */
    @GetMapping("/{id}/suggestions")
    @Operation(summary = "Get song suggestions — returns up to 50 by default")
    public ResponseEntity<ApiResponse<Object>> getSongSuggestions(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getSongSuggestions(id, limit)));
    }

    /**
     * Get lyrics for a song by ID.
     *
     * All resolution logic (lyricsId lookup, hasLyrics check, fallbacks) lives
     * in JioSaavnService.getSongLyrics() — the controller just calls it and
     * returns the result. Returns 404 when lyrics are unavailable.
     */
    @GetMapping("/{id}/lyrics")
    @Operation(summary = "Get lyrics for a song by ID")
    public ResponseEntity<?> getSongLyrics(
            @PathVariable @Parameter(description = "JioSaavn song ID") String id) {
        try {
            Map<String, Object> result = jiosaavnService.getSongLyrics(id);
            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(404).body(
                        Map.of("success", false, "message", "Lyrics not available for this song"));
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "Lyrics not available for this song"));
        }
    }
}