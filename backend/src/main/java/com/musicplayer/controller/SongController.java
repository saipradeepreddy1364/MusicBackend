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
     * JioSaavn uses a separate "lyricsId" field (returned inside the song's
     * detail response) to key the lyrics endpoint — passing the song ID
     * directly always returns 404. We therefore:
     *   1. Fetch song details to extract lyricsId (and check hasLyrics).
     *   2. Call getSongLyrics(lyricsId) only when a valid lyricsId exists.
     *
     * Returns 404 with a clean JSON body when lyrics are unavailable.
     */
    @GetMapping("/{id}/lyrics")
    @Operation(summary = "Get lyrics for a song by ID")
    public ResponseEntity<?> getSongLyrics(
            @PathVariable @Parameter(description = "JioSaavn song ID") String id) {
        try {
            String lyricsId = resolveLyricsId(id);
            if (lyricsId == null || lyricsId.isBlank()) {
                return notFound();
            }

            Map<String, Object> result = jiosaavnService.getSongLyrics(lyricsId);
            if (result == null || result.isEmpty()) {
                return notFound();
            }

            // Normalise: ensure the lyrics field is always called "lyrics" at top level
            // so the frontend extractLyricsText() always finds it regardless of shape.
            Object lyricText = result.get("lyrics");
            if (lyricText == null) lyricText = result.get("snippet");
            if (lyricText == null) lyricText = result.get("lyric");
            if (lyricText == null) lyricText = result.get("lyricsSnippet");

            if (lyricText == null || lyricText.toString().isBlank()) {
                return notFound();
            }

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("lyrics", lyricText.toString())));

        } catch (Exception e) {
            return notFound();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body(
                Map.of("success", false, "message", "Lyrics not available for this song"));
    }

    /**
     * Fetch song details and extract the lyricsId needed by JioSaavn's
     * lyrics endpoint. Returns null if the song has no lyrics or if the
     * lyricsId field cannot be found in the response.
     *
     * Handles both list and map shapes of the "data" field:
     *   { data: [ { id, name, lyricsId, hasLyrics, ... } ] }
     *   { data: { id, name, lyricsId, hasLyrics, ... } }
     */
    @SuppressWarnings("unchecked")
    private String resolveLyricsId(String songId) {
        Map<String, Object> details = jiosaavnService.getSongById(songId);
        if (details == null) return null;

        Object dataObj = details.get("data");
        Map<String, Object> songData = null;

        if (dataObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?,?>) songData = (Map<String, Object>) first;
        } else if (dataObj instanceof Map<?,?>) {
            songData = (Map<String, Object>) dataObj;
        } else {
            // Flat response — details itself is the song map
            songData = details;
        }

        if (songData == null) return null;

        // Honour hasLyrics=false so we don't make a pointless lyrics call
        Object hasLyrics = songData.get("hasLyrics");
        if (Boolean.FALSE.equals(hasLyrics) || "false".equalsIgnoreCase(String.valueOf(hasLyrics))) {
            return null;
        }

        // Try both known field names for the lyrics identifier
        Object lyricsId = songData.get("lyricsId");
        if (lyricsId == null) lyricsId = songData.get("lyrics_id");

        // Some API versions set hasLyrics=true but omit the lyricsId;
        // in that case fall back to the song ID itself (works on some tracks).
        if (lyricsId == null && Boolean.TRUE.equals(hasLyrics)) {
            return songId;
        }

        return lyricsId instanceof String s && !s.isBlank() ? s : null;
    }
}