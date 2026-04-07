package com.musicplayer.service;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    private final WebClient webClient;

    public JioSaavnService(@Value("${jiosaavn.base-url}") @NonNull String baseUrl) {
        log.info("JioSaavnService initialized with baseUrl={}", baseUrl);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(25));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        java.util.Objects.requireNonNull(httpClient)))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36")
                .build();
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    public Map<String, Object> search(String query, int page, int limit) {
        log.info("search | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    public Map<String, Object> searchSongs(String query, int page, int limit) {
        log.info("searchSongs | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/songs")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        log.info("searchAlbums | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/albums")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    public Map<String, Object> searchArtists(String query, int page, int limit) {
        log.info("searchArtists | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/artists")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        log.info("searchPlaylists | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/playlists")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    // ── SONGS ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getSongById(String id) {
        log.info("getSongById | id={}", id);
        return getMap(u -> u.path("/songs/" + id).build());
    }

    public Map<String, Object> getSongSuggestions(String id, int limit) {
        log.info("getSongSuggestions | id={} limit={}", id, limit);
        return getMap(u -> u.path("/songs/" + id + "/suggestions")
                .queryParam("limit", limit)
                .build());
    }

    /**
     * Fetches lyrics for the given song ID.
     *
     * Strategy:
     *   1. Try the dedicated /songs/{id}/lyrics endpoint on saavn.dev
     *   2. If that returns nothing (song has no standalone lyrics resource),
     *      fall back to fetching the full song object and pulling the
     *      "lyrics" / "lyrics_snippet" field that saavn.dev sometimes embeds there.
     *
     * Never throws — always returns a map or empty map so the controller
     * can return 404 cleanly instead of propagating a 500.
     */
    public Map<String, Object> getSongLyrics(String id) {
        log.info("getSongLyrics | id={}", id);

        // ── Attempt 1: dedicated lyrics endpoint ─────────────────────────────
        Map<String, Object> lyricsResult = getMapAllowNotFound(
                u -> u.path("/songs/" + id + "/lyrics").build());

        if (!isDataEmpty(lyricsResult)) {
            log.info("getSongLyrics | found via lyrics endpoint | id={}", id);
            return lyricsResult;
        }

        // ── Attempt 2: extract from song details ─────────────────────────────
        // saavn.dev embeds lyrics inside the song object for some tracks.
        log.info("getSongLyrics | lyrics endpoint empty, trying song details | id={}", id);
        Map<String, Object> songResult = getMapAllowNotFound(
                u -> u.path("/songs/" + id).build());

        String lyrics = extractLyricsFromSongObject(songResult);
        if (lyrics != null && !lyrics.isBlank()) {
            log.info("getSongLyrics | found via song details | id={}", id);
            return Map.of("lyrics", lyrics);
        }

        log.warn("getSongLyrics | no lyrics found for id={}", id);
        return Collections.emptyMap();
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getAlbum(String id, String link) {
        log.info("getAlbum | id={} link={}", id, link);
        return getMap(u -> {
            var b = u.path("/albums");
            if (id != null && !id.isBlank())     b = b.queryParam("id", id);
            if (link != null && !link.isBlank()) b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    public Map<String, Object> getPlaylist(String id, String link) {
        log.info("getPlaylist | id={} link={}", id, link);
        return getMap(u -> {
            var b = u.path("/playlists");
            if (id != null && !id.isBlank())     b = b.queryParam("id", id);
            if (link != null && !link.isBlank()) b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    public Map<String, Object> getArtist(String id) {
        log.info("getArtist | id={}", id);
        return getMap(u -> u.path("/artists/" + id).build());
    }

    public Map<String, Object> getArtistSongs(String id, int page,
                                               String sortBy, String sortOrder) {
        log.info("getArtistSongs | id={} page={} sortBy={} sortOrder={}",
                id, page, sortBy, sortOrder);
        String sb = (sortBy    != null && !sortBy.isBlank())    ? sortBy    : "latest";
        String so = (sortOrder != null && !sortOrder.isBlank()) ? sortOrder : "desc";
        return getMap(u -> u.path("/artists/" + id + "/songs")
                .queryParam("page",      page)
                .queryParam("sortBy",    sb)
                .queryParam("sortOrder", so)
                .build());
    }

    public Map<String, Object> getArtistAlbums(String id, int page) {
        log.info("getArtistAlbums | id={} page={}", id, page);
        return getMap(u -> u.path("/artists/" + id + "/albums")
                .queryParam("page", page)
                .build());
    }

    // ── CHARTS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getCharts() {
        log.info("getCharts | search fallback");
        Map<String, Object> result = searchSongs("top hindi hits 2025", 1, 50);
        if (isDataEmpty(result)) {
            log.warn("getCharts | primary empty, trying fallback");
            result = searchSongs("bollywood hits", 1, 50);
        }
        return result;
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Standard GET — treats any non-2xx as an error and returns empty map.
     */
    private Map<String, Object> getMap(
            @NonNull Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .block();
            return response != null ? response : Collections.emptyMap();
        } catch (WebClientResponseException e) {
            log.error("getMap | HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("getMap | error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Like getMap but treats 404 as a normal "not found" (logs WARN, not ERROR).
     * Use for endpoints where absence of data is expected (e.g. lyrics not available).
     */
    private Map<String, Object> getMapAllowNotFound(
            @NonNull Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .block();
            return response != null ? response : Collections.emptyMap();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("getMapAllowNotFound | 404 Not Found (expected for missing lyrics)");
            return Collections.emptyMap();
        } catch (WebClientResponseException e) {
            log.error("getMapAllowNotFound | HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("getMapAllowNotFound | error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Tries to pull a lyrics string out of a saavn.dev song-details response.
     * saavn.dev wraps data in: { success, data: [ { lyrics, lyrics_snippet, ... } ] }
     * or sometimes:            { success, data: { lyrics, ... } }
     */
    private String extractLyricsFromSongObject(Map<String, Object> songMap) {
        if (songMap == null || songMap.isEmpty()) return null;

        Object dataObj = songMap.get("data");

        // data is a List — take first element
        if (dataObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> songData) {
                return getLyricsFromMap(songData);
            }
        }

        // data is a Map directly
        if (dataObj instanceof Map<?, ?> songData) {
            return getLyricsFromMap(songData);
        }

        // top-level (no wrapper)
        return getLyricsFromMap(songMap);
    }

    private String getLyricsFromMap(Map<?, ?> map) {
        if (map == null) return null;

        // Full lyrics field
        Object lyrics = map.get("lyrics");
        if (lyrics instanceof String s && !s.isBlank()) return s;

        // Snippet as last resort
        Object snippet = map.get("lyrics_snippet");
        if (snippet instanceof String s && !s.isBlank()) return s;

        return null;
    }

    private boolean isDataEmpty(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return true;
        Object data = map.get("data");
        if (data == null) return true;
        if (data instanceof Map<?, ?> m && m.isEmpty()) return true;
        if (data instanceof List<?> l && l.isEmpty()) return true;
        return false;
    }
}