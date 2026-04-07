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

    @GetMapping
    @Operation(summary = "Global search across all categories")
    public ResponseEntity<ApiResponse<Object>> searchAll(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.search(query, page, limit)));
    }

    @GetMapping("/songs")
    @Operation(summary = "Search songs")
    public ResponseEntity<ApiResponse<Object>> searchSongs(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchSongs(query, page, limit)));
    }

    @GetMapping("/albums")
    @Operation(summary = "Search albums")
    public ResponseEntity<ApiResponse<Object>> searchAlbums(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchAlbums(query, page, limit)));
    }

    @GetMapping("/artists")
    @Operation(summary = "Search artists")
    public ResponseEntity<ApiResponse<Object>> searchArtists(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.searchArtists(query, page, limit)));
    }

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