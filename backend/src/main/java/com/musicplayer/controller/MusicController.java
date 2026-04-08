package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/music")
@CrossOrigin("*")
public class MusicController {

    private final JioSaavnService service;

    public MusicController(JioSaavnService service) {
        this.service = service;
    }

    // ✅ SEARCH
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = service.searchSongs(query, page, limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ✅ SONG BY ID
    @GetMapping("/song/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSong(@PathVariable String id) {

        Map<String, Object> result = service.getSongById(id);

        if (result == null || result.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Song not found"));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}