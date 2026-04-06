package com.musicplayer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;
import java.util.*;

@Service
public class JioSaavnService {

    private static final Logger logger = LoggerFactory.getLogger(JioSaavnService.class);

    private final WebClient webClient;

    // Primary Workers API base URL
    private static final String BASE_URL = "https://jiosaavn-api.pradeepreddypalagiri.workers.dev";

    public JioSaavnService() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(25))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    // ─────────────────────────────────────────────
    // Search Songs
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> searchSongs(String query, int page, int limit) {
        try {
            logger.info("Searching songs: query={}, page={}, limit={}", query, page, limit);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/search/songs")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .onErrorReturn(new HashMap<>())
                    .block();

            return extractSongs(response);

        } catch (Exception e) {
            logger.error("Error searching songs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────
    // Get Playlist by ID
    // ─────────────────────────────────────────────
    public Map<String, Object> getPlaylist(String playlistId) {
        try {
            logger.info("Fetching playlist: id={}", playlistId);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/playlists")
                            .queryParam("id", playlistId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .onErrorResume(e -> {
                        logger.error("Playlist fetch error: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();

            if (response == null || response.isEmpty()) {
                logger.warn("Empty response for playlist id={}", playlistId);
                return new HashMap<>();
            }

            return response;

        } catch (Exception e) {
            logger.error("Error fetching playlist {}: {}", playlistId, e.getMessage());
            return new HashMap<>();
        }
    }

    // ─────────────────────────────────────────────
    // Get Song by ID
    // ─────────────────────────────────────────────
    public Map<String, Object> getSongById(String songId) {
        try {
            logger.info("Fetching song: id={}", songId);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/songs/" + songId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .onErrorReturn(new HashMap<>())
                    .block();

            return response != null ? response : new HashMap<>();

        } catch (Exception e) {
            logger.error("Error fetching song {}: {}", songId, e.getMessage());
            return new HashMap<>();
        }
    }

    // ─────────────────────────────────────────────
    // Get Trending / Home Feed Songs
    // Fallback: search popular terms when no playlist works
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getTrendingSongs() {
        logger.info("Fetching trending songs via search fallback");
        List<Map<String, Object>> results = searchSongs("top hindi songs 2024", 1, 50);
        if (results.isEmpty()) {
            results = searchSongs("bollywood hits", 1, 50);
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // Get Songs from Album
    // ─────────────────────────────────────────────
    public Map<String, Object> getAlbum(String albumId) {
        try {
            logger.info("Fetching album: id={}", albumId);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/albums")
                            .queryParam("id", albumId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .onErrorReturn(new HashMap<>())
                    .block();

            return response != null ? response : new HashMap<>();

        } catch (Exception e) {
            logger.error("Error fetching album {}: {}", albumId, e.getMessage());
            return new HashMap<>();
        }
    }

    // ─────────────────────────────────────────────
    // Get Song Stream URL
    // ─────────────────────────────────────────────
    public String getSongStreamUrl(String songId) {
        try {
            Map<String, Object> song = getSongById(songId);
            return extractStreamUrl(song);
        } catch (Exception e) {
            logger.error("Error getting stream URL for {}: {}", songId, e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // Helper: Extract songs list from API response
    // Handles both { data: { results: [...] } } and { data: [...] }
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractSongs(Map<String, Object> response) {
        if (response == null || response.isEmpty()) return new ArrayList<>();

        try {
            Object data = response.get("data");

            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                Object results = dataMap.get("results");
                if (results instanceof List) {
                    return (List<Map<String, Object>>) results;
                }
                // Sometimes songs are directly in data
                Object songs = dataMap.get("songs");
                if (songs instanceof List) {
                    return (List<Map<String, Object>>) songs;
                }
            }

            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }

            // Top-level results key
            Object results = response.get("results");
            if (results instanceof List) {
                return (List<Map<String, Object>>) results;
            }

        } catch (Exception e) {
            logger.error("Error extracting songs from response: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────
    // Helper: Extract best quality stream URL
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String extractStreamUrl(Map<String, Object> songData) {
        if (songData == null) return null;

        try {
            // Try downloadUrl array (highest quality last)
            Object dlUrls = songData.get("downloadUrl");
            if (dlUrls instanceof List) {
                List<Map<String, Object>> urls = (List<Map<String, Object>>) dlUrls;
                if (!urls.isEmpty()) {
                    // Return the highest quality (last item)
                    Map<String, Object> best = urls.get(urls.size() - 1);
                    return (String) best.get("url");
                }
            }

            // Fallback: direct url field
            Object url = songData.get("url");
            if (url instanceof String) return (String) url;

            // Fallback: media_url field
            Object mediaUrl = songData.get("media_url");
            if (mediaUrl instanceof String) return (String) mediaUrl;

        } catch (Exception e) {
            logger.error("Error extracting stream URL: {}", e.getMessage());
        }

        return null;
    }
}