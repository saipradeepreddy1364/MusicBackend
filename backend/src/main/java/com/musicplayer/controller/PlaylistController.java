package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/playlists")
@Tag(name = "Playlists", description = "Get playlist details and songs")
public class PlaylistController {

    private final JioSaavnService jiosaavnService;

    public PlaylistController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    @GetMapping
    @Operation(summary = "Get playlist by ID or JioSaavn link")
    public ResponseEntity<ApiResponse<Object>> getPlaylist(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String link) {
        if (id == null && link == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Provide either 'id' or 'link' query parameter"));
        }
        // page/limit removed — the upstream Workers API does not support them on playlists
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getPlaylist(id, link)));
    }
}