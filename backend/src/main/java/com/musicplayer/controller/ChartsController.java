package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/charts")
@RequiredArgsConstructor
@Tag(name = "Charts", description = "Trending charts and top playlists")
public class ChartsController {

    private final JioSaavnService jiosaavnService;

    @GetMapping
    @Operation(summary = "Get trending charts / top playlists")
    public ResponseEntity<ApiResponse<Object>> getCharts() {
        return ResponseEntity.ok(ApiResponse.success(jiosaavnService.getCharts()));
    }
}