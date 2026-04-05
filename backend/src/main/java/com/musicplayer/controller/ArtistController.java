package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
@Validated
@Tag(name = "Artists", description = "Get artist profiles, songs and albums")
public class ArtistController {

    private final JioSaavnService jiosaavnService;

    @GetMapping("/{id}")
    @Operation(summary = "Get artist profile by ID")
    public ResponseEntity<ApiResponse<Object>> getArtist(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getArtist(id)));
    }

    @GetMapping("/{id}/songs")
    @Operation(summary = "Get artist's songs")
    public ResponseEntity<ApiResponse<Object>> getArtistSongs(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.getArtistSongs(id, page, sortBy, sortOrder)));
    }

    @GetMapping("/{id}/albums")
    @Operation(summary = "Get artist's albums")
    public ResponseEntity<ApiResponse<Object>> getArtistAlbums(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getArtistAlbums(id, page)));
    }
}