package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artists")
@Validated
@Tag(name = "Artists", description = "Get artist profiles, songs and albums")
public class ArtistController {

    private final JioSaavnService jiosaavnService;

    public ArtistController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artist profile by ID")
    public ResponseEntity<ApiResponse<Object>> getArtist(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getArtist(id)));
    }

    /**
     * Get artist's songs.
     *
     * The frontend calls this repeatedly with page=1, 2, 3 … up to 20 to
     * retrieve the artist's full catalogue (up to 1 000 songs).
     * Default sort is "latest" / "desc" so newest songs appear first.
     */
    @GetMapping("/{id}/songs")
    @Operation(summary = "Get artist's songs — paginate up to page 20 for full catalogue")
    public ResponseEntity<ApiResponse<Object>> getArtistSongs(
            @PathVariable String id,
            @RequestParam(defaultValue = "1")          @Min(1) int page,
            @RequestParam(defaultValue = "latest")              String sortBy,
            @RequestParam(defaultValue = "desc")                String sortOrder) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        jiosaavnService.getArtistSongs(id, page, sortBy, sortOrder)));
    }

    @GetMapping("/{id}/albums")
    @Operation(summary = "Get artist's albums")
    public ResponseEntity<ApiResponse<Object>> getArtistAlbums(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ApiResponse.success(jiosaavnService.getArtistAlbums(id, page)));
    }
}