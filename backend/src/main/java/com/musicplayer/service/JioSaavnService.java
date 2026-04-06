package com.musicplayer.service;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

@Service
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    private static final String BASE_URL =
            "https://jiosaavn-api.pradeepreddypalagiri.workers.dev/api";

    private final WebClient webClient;

    public JioSaavnService() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(25));

        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    /** GET /search?query=&page=&limit= */
    public Map<String, Object> search(String query, int page, int limit) {
        log.info("search | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    /** GET /search/songs?query=&page=&limit= */
    public Map<String, Object> searchSongs(String query, int page, int limit) {
        log.info("searchSongs | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/songs")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    /** GET /search/albums?query=&page=&limit= */
    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        log.info("searchAlbums | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/albums")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    /** GET /search/artists?query=&page=&limit= */
    public Map<String, Object> searchArtists(String query, int page, int limit) {
        log.info("searchArtists | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/artists")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    /** GET /search/playlists?query=&page=&limit= */
    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        log.info("searchPlaylists | query={} page={} limit={}", query, page, limit);
        return getMap(u -> u.path("/search/playlists")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build());
    }

    // ── SONGS ─────────────────────────────────────────────────────────────────

    /** GET /songs/{id} */
    public Map<String, Object> getSongById(String id) {
        log.info("getSongById | id={}", id);
        return getMap(u -> u.path("/songs/" + id).build());
    }

    /** GET /songs/{id}/suggestions?limit= */
    public Map<String, Object> getSongSuggestions(String id, int limit) {
        log.info("getSongSuggestions | id={} limit={}", id, limit);
        return getMap(u -> u.path("/songs/" + id + "/suggestions")
                .queryParam("limit", limit)
                .build());
    }

    /** GET /songs/{id}/lyrics */
    public Map<String, Object> getSongLyrics(String id) {
        log.info("getSongLyrics | id={}", id);
        return getMap(u -> u.path("/songs/" + id + "/lyrics").build());
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    /**
     * GET /albums?id= OR /albums?link=
     * Called by AlbumController as getAlbum(id, link) — both params accepted.
     */
    public Map<String, Object> getAlbum(String id, String link) {
        log.info("getAlbum | id={} link={}", id, link);
        return getMap(u -> {
            var b = u.path("/albums");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank()) b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    /**
     * GET /playlists?id= OR /playlists?link=
     * Called by PlaylistController as getPlaylist(id, link).
     * page/limit intentionally omitted — upstream ignores them and they cause 502s.
     */
    public Map<String, Object> getPlaylist(String id, String link) {
        log.info("getPlaylist | id={} link={}", id, link);
        return getMap(u -> {
            var b = u.path("/playlists");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank()) b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    /** GET /artists/{id} */
    public Map<String, Object> getArtist(String id) {
        log.info("getArtist | id={}", id);
        return getMap(u -> u.path("/artists/" + id).build());
    }

    /** GET /artists/{id}/songs?page=&sortBy=&sortOrder= */
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

    /** GET /artists/{id}/albums?page= */
    public Map<String, Object> getArtistAlbums(String id, int page) {
        log.info("getArtistAlbums | id={} page={}", id, page);
        return getMap(u -> u.path("/artists/" + id + "/albums")
                .queryParam("page", page)
                .build());
    }

    // ── CHARTS / HOME ─────────────────────────────────────────────────────────

    /** Returns trending songs via search fallback. Used by ChartsController. */
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

    private Map<String, Object> getMap(Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(25))
                    .block();

            return response != null ? response : Collections.emptyMap();

        } catch (WebClientResponseException e) {
            log.error("getMap | HTTP {} – {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("getMap | error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isDataEmpty(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return true;
        Object data = map.get("data");
        if (data == null) return true;
        if (data instanceof Map<?, ?> m && m.isEmpty()) return true;
        if (data instanceof List<?> l && l.isEmpty()) return true;
        return false;
    }
}