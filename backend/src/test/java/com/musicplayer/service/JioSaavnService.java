package com.musicplayer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JioSaavnService {

    private final WebClient webClient;

    public JioSaavnService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://saavn.dev/api")
                .build();
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private Map<String, Object> get(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Global search — called by SearchController.searchAll()
     */
    public Map<String, Object> search(String query, int page, int limit) {
        try {
            log.info("Global search: query={}, page={}, limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on search: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to search");
        } catch (Exception e) {
            log.error("Unexpected error on search", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Search songs — called by SearchController.searchSongs()
     * NOTE: signature is (String, int, int) — page + limit added vs old single-arg version
     */
    public Map<String, Object> searchSongs(String query, int page, int limit) {
        try {
            log.info("Search songs: query={}, page={}, limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/songs")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchSongs: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to search songs");
        } catch (Exception e) {
            log.error("Unexpected error on searchSongs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Search albums — called by SearchController.searchAlbums()
     */
    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        try {
            log.info("Search albums: query={}, page={}, limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/albums")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchAlbums: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to search albums");
        } catch (Exception e) {
            log.error("Unexpected error on searchAlbums", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Search artists — called by SearchController.searchArtists()
     */
    public Map<String, Object> searchArtists(String query, int page, int limit) {
        try {
            log.info("Search artists: query={}, page={}, limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/artists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchArtists: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to search artists");
        } catch (Exception e) {
            log.error("Unexpected error on searchArtists", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Search playlists — called by SearchController.searchPlaylists()
     */
    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        try {
            log.info("Search playlists: query={}, page={}, limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/playlists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchPlaylists: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to search playlists");
        } catch (Exception e) {
            log.error("Unexpected error on searchPlaylists", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Songs ─────────────────────────────────────────────────────────────────

    /**
     * Get song by ID — called by SongController.getSong()
     */
    public Map<String, Object> getSongById(String id) {
        try {
            log.info("Get song by ID: {}", id);
            return get("/songs/" + id);
        } catch (WebClientResponseException e) {
            log.error("API error on getSongById: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch song");
        } catch (Exception e) {
            log.error("Unexpected error on getSongById", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Get song suggestions — called by SongController.getSongSuggestions()
     */
    public Map<String, Object> getSongSuggestions(String id, int limit) {
        try {
            log.info("Get song suggestions: id={}, limit={}", id, limit);
            return webClient.get()
                    .uri(u -> u.path("/songs/" + id + "/suggestions")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getSongSuggestions: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch song suggestions");
        } catch (Exception e) {
            log.error("Unexpected error on getSongSuggestions", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Albums ────────────────────────────────────────────────────────────────

    /**
     * Get album by ID or link — called by AlbumController.getAlbum()
     * Second param is 'link' (not 'token') matching the controller's @RequestParam
     */
    public Map<String, Object> getAlbum(String id, String link) {
        try {
            log.info("Get album: id={}, link={}", id, link);
            return webClient.get()
                    .uri(u -> {
                        var builder = u.path("/albums");
                        if (id != null)   builder = builder.queryParam("id", id);
                        if (link != null) builder = builder.queryParam("link", link);
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getAlbum: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch album");
        } catch (Exception e) {
            log.error("Unexpected error on getAlbum", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Artists ───────────────────────────────────────────────────────────────

    /**
     * Get artist profile — called by ArtistController.getArtist()
     */
    public Map<String, Object> getArtist(String id) {
        try {
            log.info("Get artist: id={}", id);
            return get("/artists/" + id);
        } catch (WebClientResponseException e) {
            log.error("API error on getArtist: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch artist");
        } catch (Exception e) {
            log.error("Unexpected error on getArtist", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Get artist songs — called by ArtistController.getArtistSongs()
     */
    public Map<String, Object> getArtistSongs(String id, int page, String sortBy, String sortOrder) {
        try {
            log.info("Get artist songs: id={}, page={}, sortBy={}, sortOrder={}", id, page, sortBy, sortOrder);
            return webClient.get()
                    .uri(u -> u.path("/artists/" + id + "/songs")
                            .queryParam("page", page)
                            .queryParam("sortBy", sortBy)
                            .queryParam("sortOrder", sortOrder)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getArtistSongs: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch artist songs");
        } catch (Exception e) {
            log.error("Unexpected error on getArtistSongs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    /**
     * Get artist albums — called by ArtistController.getArtistAlbums()
     */
    public Map<String, Object> getArtistAlbums(String id, int page) {
        try {
            log.info("Get artist albums: id={}, page={}", id, page);
            return webClient.get()
                    .uri(u -> u.path("/artists/" + id + "/albums")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getArtistAlbums: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch artist albums");
        } catch (Exception e) {
            log.error("Unexpected error on getArtistAlbums", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Playlists ─────────────────────────────────────────────────────────────

    /**
     * Get playlist by ID or link — called by PlaylistController.getPlaylist()
     * Second param is 'link' matching the controller's @RequestParam
     */
    public Map<String, Object> getPlaylist(String id, String link, int page, int limit) {
        try {
            log.info("Get playlist: id={}, link={}, page={}, limit={}", id, link, page, limit);
            return webClient.get()
                    .uri(u -> {
                        var builder = u.path("/playlists");
                        if (id != null)   builder = builder.queryParam("id", id);
                        if (link != null) builder = builder.queryParam("link", link);
                        return builder
                                .queryParam("page", page)
                                .queryParam("limit", limit)
                                .build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getPlaylist: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch playlist");
        } catch (Exception e) {
            log.error("Unexpected error on getPlaylist", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    /**
     * Get charts — called by ChartsController.getCharts()
     */
    public Map<String, Object> getCharts() {
        try {
            log.info("Fetching charts");
            return get("/modules?language=english");
        } catch (WebClientResponseException e) {
            log.error("API error on getCharts: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch charts");
        } catch (Exception e) {
            log.error("Unexpected error on getCharts", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ── Health ────────────────────────────────────────────────────────────────

    /**
     * Health check — called by HealthController (legacy, kept for safety)
     */
    public Map<String, String> healthCheck() {
        log.info("Health check called");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "JioSaavnService");
        return response;
    }
}