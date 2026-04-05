package com.musicplayer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Core service that calls saavn.dev (JioSaavn unofficial API) DIRECTLY.
 * No separate Node.js proxy needed — single deployment on Render free tier.
 * All responses are cached with Caffeine for 5 minutes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JioSaavnService {

    private final WebClient jiosaavnClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // ──────────────────────────────────────────────────────────────────────────
    // SEARCH
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "search", key = "#query + '_' + #page + '_' + #limit")
    public Object search(String query, int page, int limit) {
        log.info("Searching: query={}, page={}, limit={}", query, page, limit);
        return get("/search", Map.of("query", query, "page", page, "limit", limit));
    }

    @Cacheable(value = "searchSongs", key = "#query + '_' + #page + '_' + #limit")
    public Object searchSongs(String query, int page, int limit) {
        log.info("Searching songs: {}", query);
        return get("/search/songs", Map.of("query", query, "page", page, "limit", limit));
    }

    @Cacheable(value = "searchAlbums", key = "#query + '_' + #page + '_' + #limit")
    public Object searchAlbums(String query, int page, int limit) {
        log.info("Searching albums: {}", query);
        return get("/search/albums", Map.of("query", query, "page", page, "limit", limit));
    }

    @Cacheable(value = "searchArtists", key = "#query + '_' + #page + '_' + #limit")
    public Object searchArtists(String query, int page, int limit) {
        log.info("Searching artists: {}", query);
        return get("/search/artists", Map.of("query", query, "page", page, "limit", limit));
    }

    @Cacheable(value = "searchPlaylists", key = "#query + '_' + #page + '_' + #limit")
    public Object searchPlaylists(String query, int page, int limit) {
        log.info("Searching playlists: {}", query);
        return get("/search/playlists", Map.of("query", query, "page", page, "limit", limit));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // SONGS
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "song", key = "#songId")
    public Object getSongById(String songId) {
        log.info("Fetching song: {}", songId);
        return get("/songs/" + songId, Map.of());
    }

    @Cacheable(value = "songSuggestions", key = "#songId + '_' + #limit")
    public Object getSongSuggestions(String songId, int limit) {
        log.info("Fetching suggestions for song: {}", songId);
        return get("/songs/" + songId + "/suggestions", Map.of("limit", limit));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ALBUMS
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "album", key = "#albumId ?: #albumLink")
    public Object getAlbum(String albumId, String albumLink) {
        log.info("Fetching album: id={}, link={}", albumId, albumLink);
        if (albumId != null) {
            return get("/albums", Map.of("id", albumId));
        }
        return get("/albums", Map.of("link", albumLink));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ARTISTS
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "artist", key = "#artistId")
    public Object getArtist(String artistId) {
        log.info("Fetching artist: {}", artistId);
        return get("/artists/" + artistId, Map.of());
    }

    @Cacheable(value = "artistSongs", key = "#artistId + '_' + #page + '_' + #sortBy + '_' + #sortOrder")
    public Object getArtistSongs(String artistId, int page, String sortBy, String sortOrder) {
        log.info("Fetching songs for artist: {}", artistId);
        return get("/artists/" + artistId + "/songs",
                Map.of("page", page, "sortBy", sortBy, "sortOrder", sortOrder));
    }

    @Cacheable(value = "artistAlbums", key = "#artistId + '_' + #page")
    public Object getArtistAlbums(String artistId, int page) {
        log.info("Fetching albums for artist: {}", artistId);
        return get("/artists/" + artistId + "/albums", Map.of("page", page));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PLAYLISTS
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "playlist", key = "#playlistId ?: #playlistLink + '_' + #page + '_' + #limit")
    public Object getPlaylist(String playlistId, String playlistLink, int page, int limit) {
        log.info("Fetching playlist: id={}, link={}", playlistId, playlistLink);
        if (playlistId != null) {
            return get("/playlists", Map.of("id", playlistId, "page", page, "limit", limit));
        }
        return get("/playlists", Map.of("link", playlistLink, "page", page, "limit", limit));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CHARTS
    // ──────────────────────────────────────────────────────────────────────────

    @Cacheable(value = "charts")
    public Object getCharts() {
        log.info("Fetching charts");
        return get("/search/playlists", Map.of("query", "Top Charts India", "limit", 10));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNAL HTTP
    // ──────────────────────────────────────────────────────────────────────────

    private Object get(String path, Map<String, Object> params) {
        try {
            return jiosaavnClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        params.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("saavn.dev error: {} {}", ex.getStatusCode(), ex.getMessage());
                        return Mono.error(new RuntimeException("saavn.dev API error: " + ex.getMessage()));
                    })
                    .block();
        } catch (Exception ex) {
            log.error("Failed to call saavn.dev at {}: {}", path, ex.getMessage());
            throw new RuntimeException("Failed to fetch data: " + ex.getMessage(), ex);
        }
    }
}