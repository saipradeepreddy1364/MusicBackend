package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import com.musicplayer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Liked songs per user")
public class UserController {

    private final UserService userService;
    private final JioSaavnService jiosaavnService;

    public UserController(UserService userService, JioSaavnService jiosaavnService) {
        this.userService = userService;
        this.jiosaavnService = jiosaavnService;
    }

    // ── GET /api/user/liked ───────────────────────────────────────────────────
    //
    // The LikedSong entity only stores songId, songTitle, and songImage —
    // it deliberately does NOT store audioUrl because JioSaavn stream URLs
    // expire within minutes. Storing them would give the frontend a broken URL.
    //
    // Instead, we enrich each liked song record here by fetching fresh song
    // details (including a live downloadUrl) from JioSaavn in parallel, then
    // merge the result back into the liked-song record before returning.
    // This way the frontend always receives a valid, playable audioUrl.

    @GetMapping("/liked")
    @Operation(summary = "Get all liked songs for the authenticated user, enriched with fresh stream URLs")
    public ResponseEntity<ApiResponse<Object>> getLikedSongs(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> result = userService.getLikedSongs(token);

        if (!Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error((String) result.get("message")));
        }

        // data is a List<Map<String,Object>> where each map has at least:
        //   songId, songTitle, songImage, likedAt
        Object raw = result.get("data");
        if (!(raw instanceof List<?> rawList) || rawList.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(raw));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> likedRows = (List<Map<String, Object>>) rawList;

        // ── Fetch full song details from JioSaavn in parallel ─────────────────
        // Use a virtual-thread-friendly executor so we don't block on I/O.
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        List<Future<Map<String, Object>>> futures = likedRows.stream()
                .map(row -> exec.submit(() -> enrichSong(row)))
                .collect(Collectors.toList());

        List<Map<String, Object>> enriched = new ArrayList<>(futures.size());
        for (Future<Map<String, Object>> f : futures) {
            try {
                enriched.add(f.get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                // If the JioSaavn fetch timed out or failed, include the bare
                // record anyway — the frontend will fall back to fetching the
                // URL itself when it tries to play the song.
                enriched.add((Map<String, Object>) futures.get(enriched.size())
                        .resultNow());
            }
        }

        exec.shutdown();

        return ResponseEntity.ok(ApiResponse.success(enriched));
    }

    /**
     * Merge a liked-song DB row with fresh JioSaavn song details.
     *
     * The merged map contains everything the frontend's backendDtoToSong()
     * needs: id/songId, title/songTitle, artist, albumArt/songImage,
     * downloadUrl (array), duration, album, movie, language.
     *
     * If the JioSaavn call fails we return the bare row so the song is still
     * shown in the UI (just not playable until the player retries).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichSong(Map<String, Object> row) {
        String songId = (String) row.get("songId");
        if (songId == null || songId.isBlank()) return row;

        try {
            Map<String, Object> details = jiosaavnService.getSongById(songId);
            if (details == null || details.isEmpty()) return row;

            // JioSaavn wraps the song inside a "data" → first element list
            Object dataObj = details.get("data");
            Map<String, Object> songData = null;

            if (dataObj instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?>) {
                    songData = (Map<String, Object>) first;
                }
            } else if (dataObj instanceof Map<?, ?>) {
                songData = (Map<String, Object>) dataObj;
            } else if (details.containsKey("id") || details.containsKey("downloadUrl")) {
                songData = details;
            }

            if (songData == null) return row;

            // Build the merged record — DB row fields take precedence for
            // identity fields (songId, songTitle, songImage) so that the
            // user's liked-song record is always authoritative for display,
            // while all stream/playback fields come fresh from JioSaavn.
            Map<String, Object> merged = new LinkedHashMap<>(songData);

            // Always override with DB values so identity is consistent
            merged.put("songId",    row.getOrDefault("songId",    songData.get("id")));
            merged.put("songTitle", row.getOrDefault("songTitle", songData.get("name")));
            merged.put("songImage", row.getOrDefault("songImage", songData.get("image")));
            merged.put("likedAt",   row.get("likedAt"));

            return merged;

        } catch (Exception e) {
            // Network error, timeout, etc. — return bare row, don't crash.
            return row;
        }
    }

    // ── POST /api/user/like ───────────────────────────────────────────────────

    @PostMapping("/like")
    @Operation(summary = "Like a song for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> likeSong(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        String token     = extractToken(request);
        String songId    = body.get("songId");
        String songTitle = body.getOrDefault("songTitle", "");
        String songImage = body.getOrDefault("songImage", "");

        Map<String, Object> result = userService.likeSong(token, songId, songTitle, songImage);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success(result));
        }
        return ResponseEntity.status(401)
                .body(ApiResponse.error((String) result.get("message")));
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
        return ResponseEntity.status(401)
                .body(ApiResponse.error((String) result.get("message")));
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