package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
@Tag(name = "Albums", description = "Get album details and tracklists")
public class AlbumController {

    private final JioSaavnService jiosaavnService;

    /**
     * GET /api/albums?id=abc123
     * GET /api/albums?link=https://www.jiosaavn.com/album/xxx
     */
    @GetMapping
    @Operation(summary = "Get album by ID or JioSaavn link")
    public ResponseEntity<ApiResponse<Object>> getAlbum(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String link) {
        if (id == null && link == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Provide either 'id' or 'link' query parameter"));
        }
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getAlbum(id, link)));
    }
}