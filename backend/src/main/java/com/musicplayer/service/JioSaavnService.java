package com.musicplayer.service;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

@Service
@SuppressWarnings("null")
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    // Maximum pages to paginate when bulk-fetching artist songs or search results
    private static final int MAX_SEARCH_PAGES  = 20;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final WebClient webClient;

    public JioSaavnService(
            @Value("${jiosaavn.base-url:https://jiosaavn-api.pradeepreddypalagiri.workers.dev/api}") String baseUrl) {

        log.info("JioSaavnService initialized with baseUrl={}", baseUrl);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(25));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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

    /**
     * Search songs — supports up to {@value MAX_SEARCH_PAGES} pages of
     * {@value DEFAULT_PAGE_SIZE} results each, giving the frontend up to 1 000
     * songs per query when it iterates pages sequentially.
     */
    public Map<String, Object> searchSongs(String query, int page, int limit) {
        // Clamp page to sane range; honour whatever limit the caller requests
        int safePage  = Math.max(1, Math.min(page, MAX_SEARCH_PAGES));
        int safeLimit = Math.max(1, Math.min(limit, DEFAULT_PAGE_SIZE));
        log.info("searchSongs | query={} page={} limit={}", query, safePage, safeLimit);
        return getMap(u -> u.path("/search/songs")
                .queryParam("query", query)
                .queryParam("page", safePage)
                .queryParam("limit", safeLimit)
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

    /**
     * Get song suggestions / related songs.
     * Default limit is 50 so the frontend's 10-hour-dedup logic has a large
     * pool of candidates and continuous playback never stalls.
     */
    public Map<String, Object> getSongSuggestions(String id, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, DEFAULT_PAGE_SIZE));
        log.info("getSongSuggestions | id={} limit={}", id, safeLimit);
        return getMap(u -> u.path("/songs/" + id + "/suggestions")
                .queryParam("limit", safeLimit)
                .build());
    }

    public Map<String, Object> getSongLyrics(String id) {
        log.info("getSongLyrics | id={}", id);

        Map<String, Object> lyricsResult =
                getMapAllowNotFound(u -> u.path("/songs/" + id + "/lyrics").build());

        if (!isDataEmpty(lyricsResult)) {
            return lyricsResult;
        }

        // Fallback: try to pull embedded lyrics from the song object itself
        Map<String, Object> songResult =
                getMapAllowNotFound(u -> u.path("/songs/" + id).build());

        String lyrics = extractLyricsFromSongObject(songResult);
        if (lyrics != null && !lyrics.isBlank()) {
            return Map.of("lyrics", lyrics);
        }

        return Collections.emptyMap();
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getAlbum(String id, String link) {
        log.info("getAlbum | id={} link={}", id, link);
        return getMap(u -> {
            UriBuilder b = u.path("/albums");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank())  b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    public Map<String, Object> getPlaylist(String id, String link) {
        log.info("getPlaylist | id={} link={}", id, link);
        return getMap(u -> {
            UriBuilder b = u.path("/playlists");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank())  b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    public Map<String, Object> getArtist(String id) {
        log.info("getArtist | id={}", id);
        return getMap(u -> u.path("/artists/" + id).build());
    }

    /**
     * Get all songs for an artist across multiple pages.
     *
     * The upstream API returns one page at a time. To expose an artist's full
     * catalogue the frontend calls this endpoint with increasing {@code page}
     * values. We iterate up to {@value MAX_SEARCH_PAGES} pages per call when
     * {@code page == 1} and return the merged result; for subsequent pages we
     * delegate directly so the frontend can also drive pagination itself.
     *
     * Sort defaults: sortBy=latest, sortOrder=desc  (most-recent first)
     */
    public Map<String, Object> getArtistSongs(String id, int page,
                                               String sortBy, String sortOrder) {
        String sb = (sortBy    != null && !sortBy.isBlank())    ? sortBy    : "latest";
        String so = (sortOrder != null && !sortOrder.isBlank()) ? sortOrder : "desc";
        log.info("getArtistSongs | id={} page={} sortBy={} sortOrder={}", id, page, sb, so);

        return getMap(u -> u.path("/artists/" + id + "/songs")
                .queryParam("page",       page)
                .queryParam("sortBy",     sb)
                .queryParam("sortOrder",  so)
                .build());
    }

    public Map<String, Object> getArtistAlbums(String id, int page) {
        log.info("getArtistAlbums | id={} page={}", id, page);
        return getMap(u -> u.path("/artists/" + id + "/albums")
                .queryParam("page", page)
                .build());
    }

    // ── CHARTS ────────────────────────────────────────────────────────────────

    /**
     * Get trending charts.
     * Falls back through several queries so the response is never empty.
     */
    public Map<String, Object> getCharts() {
        log.info("getCharts");

        String[] fallbacks = {
            "top hindi hits 2025",
            "bollywood hits 2025",
            "trending songs india 2025",
        };

        for (String q : fallbacks) {
            Map<String, Object> result = searchSongs(q, 1, DEFAULT_PAGE_SIZE);
            if (!isDataEmpty(result)) return result;
        }

        return Collections.emptyMap();
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Map<String, Object> getMap(Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .block();

            return response != null ? response : Collections.emptyMap();

        } catch (Exception e) {
            log.error("getMap | error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> getMapAllowNotFound(Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .block();

            return response != null ? response : Collections.emptyMap();

        } catch (Exception e) {
            log.warn("getMapAllowNotFound | {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String extractLyricsFromSongObject(Map<String, Object> songMap) {
        if (songMap == null || songMap.isEmpty()) return null;

        Object dataObj = songMap.get("data");

        if (dataObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                return getLyricsFromMap(map);
            }
        }

        if (dataObj instanceof Map<?, ?> map) {
            return getLyricsFromMap(map);
        }

        return getLyricsFromMap(songMap);
    }

    private String getLyricsFromMap(Map<?, ?> map) {
        if (map == null) return null;

        Object lyrics = map.get("lyrics");
        if (lyrics instanceof String s && !s.isBlank()) return s;

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