package com.musicplayer.controller;

import com.musicplayer.dto.ApiResponse;
import com.musicplayer.service.JioSaavnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints that serve audio data for offline download.
 * All download *tracking* (library management) is handled by the frontend via Supabase.
 *
 * Two endpoints are provided:
 *  - GET /api/downloads/{videoId}/audio      → raw audio bytes (app saves to local storage)
 *  - GET /api/downloads/{videoId}/stream-url → direct CDN URL (for external download managers)
 */
@RestController
@RequestMapping("/downloads")
@CrossOrigin("*")
@Tag(name = "Downloads", description = "Audio download endpoints for offline listening")
public class DownloadController {

    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    private final JioSaavnService jiosaavnService;

    public DownloadController(JioSaavnService jiosaavnService) {
        this.jiosaavnService = jiosaavnService;
    }

    /**
     * Fetches raw audio bytes from YouTube via Piped and streams them back
     * to the Android client. The app saves the file locally for offline playback.
     *
     * No authentication required — the videoId is not secret.
     * All library tracking (saved songs list, metadata) is managed by the frontend via Supabase.
     */
    @GetMapping("/{videoId}/audio")
    @Operation(summary = "Download raw audio bytes for offline playback")
    public ResponseEntity<byte[]> downloadAudio(@PathVariable String videoId) {
        log.info("downloadAudio | videoId={}", videoId);

        byte[] audioBytes = jiosaavnService.fetchAudioBytes(videoId);

        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("downloadAudio | No audio found for videoId={}", videoId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(videoId + ".webm")
                        .build());
        headers.setContentLength(audioBytes.length);
        headers.set("X-Video-Id", videoId);

        log.info("downloadAudio | Returning {} bytes for videoId={}", audioBytes.length, videoId);
        return ResponseEntity.ok().headers(headers).body(audioBytes);
    }

    /**
     * Returns the direct YouTube CDN audio URL without proxying.
     * Use this if the Android app has its own download manager and
     * can fetch the file directly without going through our server.
     *
     * Note: YouTube CDN URLs expire after ~6 hours, so download immediately.
     */
    @GetMapping("/{videoId}/stream-url")
    @Operation(summary = "Get direct expiring audio URL for client-side download")
    public ResponseEntity<ApiResponse<Object>> getStreamUrl(@PathVariable String videoId) {
        log.info("getStreamUrl | videoId={}", videoId);
        String url = jiosaavnService.resolveAudioUrl(videoId);
        if (url == null || url.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Audio URL not available for this video"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("videoId", videoId, "audioUrl", url,
                       "note", "URL expires in ~6 hours. Download immediately.")));
    }
}
