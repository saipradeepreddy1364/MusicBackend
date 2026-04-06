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

        // ✅ removed @NonNull from local variable (not allowed there)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(25));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(java.util.Objects.requireNonNull(httpClient)))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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

    public Map<String, Object> getSongLyrics(String id) {
        log.info("getSongLyrics | id={}", id);
        return getMap(u -> u.path("/songs/" + id + "/lyrics").build());
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

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

    private Map<String, Object> getMap(@NonNull Function<UriBuilder, URI> uriFunction) {
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

    private boolean isDataEmpty(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return true;
        Object data = map.get("data");
        if (data == null) return true;
        if (data instanceof Map<?, ?> m && m.isEmpty()) return true;
        if (data instanceof List<?> l && l.isEmpty()) return true;
        return false;
    }
}