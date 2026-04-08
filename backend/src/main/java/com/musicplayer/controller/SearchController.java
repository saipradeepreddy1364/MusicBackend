package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@Validated
@Tag(name = "Search", description = "Search songs, albums, artists, playlists")
public class SearchController {

    private final JioSaavnService jiosaavnService;

    public SearchController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    /**
     * Global search across all categories.
     * Increased default limit to 50 per page and supports up to page 20
     * so the frontend can paginate through 1000+ results.
     */
    @GetMapping
    @Operation(summary = "Global search across all categories")
    public ResponseEntity<ApiResponse<Object>> searchAll(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.search(query, page, limit)));
    }

    /**
     * Search songs.
     * Default limit raised to 50; page cap is enforced in the service layer.
     * The frontend fetches pages 1-20 sequentially, yielding up to 1000 songs
     * per search query (depending on what the upstream API has available).
     */
    @GetMapping("/songs")
    @Operation(summary = "Search songs — supports up to 20 pages of 50 results each")
    public ResponseEntity<ApiResponse<Object>> searchSongs(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchSongs(query, page, limit)));
    }

    /**
     * Search albums.
     * Limit raised to 20 so more album cards are returned.
     */
    @GetMapping("/albums")
    @Operation(summary = "Search albums")
    public ResponseEntity<ApiResponse<Object>> searchAlbums(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchAlbums(query, page, limit)));
    }

    /**
     * Search artists.
     * Limit raised to 20 for more artist results.
     */
    @GetMapping("/artists")
    @Operation(summary = "Search artists")
    public ResponseEntity<ApiResponse<Object>> searchArtists(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchArtists(query, page, limit)));
    }

    /**
     * Search playlists.
     */
    @GetMapping("/playlists")
    @Operation(summary = "Search playlists")
    public ResponseEntity<ApiResponse<Object>> searchPlaylists(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchPlaylists(query, page, limit)));
    }
}