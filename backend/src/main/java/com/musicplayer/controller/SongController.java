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

import java.util.List;
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

    /**
     * Get song details by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get song details by ID")
    public ResponseEntity<ApiResponse<Object>> getSong(
            @PathVariable @Parameter(description = "JioSaavn song ID") String id) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getSongById(id)));
    }

    /**
     * Get song suggestions / related songs.
     */
    @GetMapping("/{id}/suggestions")
    @Operation(summary = "Get song suggestions / related songs — returns up to 50 by default")
    public ResponseEntity<ApiResponse<Object>> getSongSuggestions(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getSongSuggestions(id, limit)));
    }

    /**
     * Get lyrics for a song by ID.
     *
     * JioSaavn requires a separate "lyricsId" (not the song ID) to fetch
     * lyrics. We first fetch the song details to extract that lyricsId, then
     * call the lyrics endpoint with it.  If either step fails, or the song
     * has no lyricsId, we return 404 — never 500.
     */
    @GetMapping("/{id}/lyrics")
    @Operation(summary = "Get lyrics for a song by ID")
    public ResponseEntity<?> getSongLyrics(
            @PathVariable @Parameter(description = "JioSaavn song ID") String id) {

        try {
            // ── Step 1: fetch song details to get lyricsId ────────────────────
            String lyricsId = resolveLyricsId(id);

            if (lyricsId == null || lyricsId.isBlank()) {
                return ResponseEntity.status(404).body(
                        Map.of("success", false, "message", "Lyrics not available for this song"));
            }

            // ── Step 2: fetch lyrics using lyricsId ───────────────────────────
            Map<String, Object> result = jiosaavnService.getSongLyrics(lyricsId);

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

    // ── Helper: extract lyricsId from song details ────────────────────────────
    //
    // JioSaavn song detail response shape (via our proxy) looks like:
    //   { data: [ { id, name, ..., lyricsId: "...", hasLyrics: true, ... } ] }
    //   or sometimes:
    //   { data: { id, name, ..., lyricsId: "..." } }
    //
    // We walk both shapes and pull out `lyricsId` or `lyrics_id`.
    // If the song has hasLyrics=false or no lyricsId we return null early.

    @SuppressWarnings("unchecked")
    private String resolveLyricsId(String songId) {
        Map<String, Object> details = jiosaavnService.getSongById(songId);
        if (details == null) return null;

        // Unwrap data field — may be a List or a Map
        Object dataObj = details.get("data");
        Map<String, Object> songData = null;

        if (dataObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?>) songData = (Map<String, Object>) first;
        } else if (dataObj instanceof Map<?, ?>) {
            songData = (Map<String, Object>) dataObj;
        } else {
            // Flat response — details itself is the song
            songData = details;
        }

        if (songData == null) return null;

        // hasLyrics guard
        Object hasLyrics = songData.get("hasLyrics");
        if (Boolean.FALSE.equals(hasLyrics) || "false".equals(String.valueOf(hasLyrics))) {
            return null;
        }

        // Try both field names
        Object lyricsId = songData.get("lyricsId");
        if (lyricsId == null) lyricsId = songData.get("lyrics_id");

        // Some responses embed the lyricsId directly as the song id when hasLyrics=true
        // In that case fall back to the original songId
        if (lyricsId == null && Boolean.TRUE.equals(hasLyrics)) {
            return songId;
        }

        return lyricsId instanceof String s ? s : null;
    }
}