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
        return getMap(u -> u.path("/search")
                .queryParam("query", query).queryParam("page", page).queryParam("limit", limit).build());
    }

    public Map<String, Object> searchSongs(String query, int page, int limit) {
        int safePage  = Math.max(1, Math.min(page, MAX_SEARCH_PAGES));
        int safeLimit = Math.max(1, Math.min(limit, DEFAULT_PAGE_SIZE));
        return getMap(u -> u.path("/search/songs")
                .queryParam("query", query).queryParam("page", safePage).queryParam("limit", safeLimit).build());
    }

    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        return getMap(u -> u.path("/search/albums")
                .queryParam("query", query).queryParam("page", page).queryParam("limit", limit).build());
    }

    public Map<String, Object> searchArtists(String query, int page, int limit) {
        return getMap(u -> u.path("/search/artists")
                .queryParam("query", query).queryParam("page", page).queryParam("limit", limit).build());
    }

    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        return getMap(u -> u.path("/search/playlists")
                .queryParam("query", query).queryParam("page", page).queryParam("limit", limit).build());
    }

    // ── SONGS ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getSongById(String id) {
        return getMap(u -> u.path("/songs/" + id).build());
    }

    public Map<String, Object> getSongSuggestions(String id, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, DEFAULT_PAGE_SIZE));
        return getMap(u -> u.path("/songs/" + id + "/suggestions")
                .queryParam("limit", safeLimit).build());
    }

    // ── LYRICS ────────────────────────────────────────────────────────────────

    /**
     * Fetch lyrics using an aggressive 5-path strategy.
     *
     * We intentionally do NOT check hasLyrics — that flag is unreliable
     * in the JioSaavn unofficial API and causes many false negatives.
     *
     *   Path 1 — /songs/{songId}/lyrics
     *   Path 2 — /lyrics?id={songId}
     *   Path 3 — Extract embedded lyrics from /songs/{songId} detail
     *   Path 4 — /songs/{lyricsId}/lyrics   (lyricsId from song detail)
     *   Path 5 — /lyrics?id={lyricsId}
     *
     * Returns Map.of("lyrics", text) or Collections.emptyMap(). Never throws.
     */
    public Map<String, Object> getSongLyrics(String songId) {
        log.info("getSongLyrics | songId={}", songId);

        // Path 1
        String found = tryLyricsPath(u -> u.path("/songs/" + songId + "/lyrics").build());
        if (found != null) { log.info("getSongLyrics | path1 hit | songId={}", songId); return Map.of("lyrics", found); }

        // Path 2
        found = tryLyricsPath(u -> u.path("/lyrics").queryParam("id", songId).build());
        if (found != null) { log.info("getSongLyrics | path2 hit | songId={}", songId); return Map.of("lyrics", found); }

        // Fetch song details for embedded lyrics + lyricsId
        Map<String, Object> songDetails = getMapAllowNotFound(u -> u.path("/songs/" + songId).build());

        // Path 3 — embedded
        String embedded = extractLyricsFromSongObject(songDetails);
        if (embedded != null && !embedded.isBlank()) {
            log.info("getSongLyrics | path3 embedded | songId={}", songId);
            return Map.of("lyrics", embedded);
        }

        // Extract lyricsId
        final String lyricsId = extractLyricsId(songDetails);

        if (lyricsId != null && !lyricsId.equals(songId)) {
            // Path 4
            found = tryLyricsPath(u -> u.path("/songs/" + lyricsId + "/lyrics").build());
            if (found != null) { log.info("getSongLyrics | path4 hit | lyricsId={}", lyricsId); return Map.of("lyrics", found); }

            // Path 5
            found = tryLyricsPath(u -> u.path("/lyrics").queryParam("id", lyricsId).build());
            if (found != null) { log.info("getSongLyrics | path5 hit | lyricsId={}", lyricsId); return Map.of("lyrics", found); }
        }

        log.info("getSongLyrics | not found | songId={}", songId);
        return Collections.emptyMap();
    }

    /** Try a single lyrics endpoint. Returns text on success, null otherwise. */
    private String tryLyricsPath(Function<UriBuilder, URI> uriFunction) {
        try {
            return extractLyricsText(getMapAllowNotFound(uriFunction));
        } catch (Exception e) {
            log.warn("tryLyricsPath | {}", e.getMessage());
            return null;
        }
    }

    /** Extract lyricsId from a song detail response. Does NOT check hasLyrics. */
    private String extractLyricsId(Map<String, Object> songDetails) {
        if (songDetails == null || songDetails.isEmpty()) return null;
        Object dataObj = songDetails.get("data");
        Map<?, ?> songData = null;
        if (dataObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            songData = m;
        } else if (dataObj instanceof Map<?, ?> m) {
            songData = m;
        }
        if (songData == null) return null;
        Object lid = songData.get("lyricsId");
        if (lid == null) lid = songData.get("lyrics_id");
        if (lid instanceof String s && !s.isBlank()) return s;
        return null;
    }

    /**
     * Exhaustive lyrics text extractor — handles every known response shape:
     *   { lyrics: "..." }
     *   { data: { lyrics: "..." } }
     *   { data: { lyrics: { snippet: "..." } } }
     *   { data: [ { lyrics: "..." } ] }
     *   { snippet / lyricsSnippet / lyricsText / fullLyrics / text: "..." }
     */
    private String extractLyricsText(Map<String, Object> response) {
        if (response == null || response.isEmpty()) return null;

        String[] keys = {"lyrics", "lyric", "snippet", "lyricsSnippet",
                         "lyrics_snippet", "lyricsText", "fullLyrics", "text"};

        // Top-level flat keys
        for (String key : keys) {
            Object val = response.get(key);
            if (val instanceof String s && !s.isBlank()) return s;
        }

        Object dataObj = response.get("data");

        if (dataObj instanceof String ds && !ds.isBlank()) return ds;

        if (dataObj instanceof Map<?, ?> dm) {
            for (String key : keys) {
                Object val = dm.get(key);
                if (val instanceof String s && !s.isBlank()) return s;
            }
            // Nested lyrics object
            if (dm.get("lyrics") instanceof Map<?, ?> lm) {
                if (lm.get("snippet")    instanceof String s && !s.isBlank()) return s;
                if (lm.get("lyrics")     instanceof String s && !s.isBlank()) return s;
                if (lm.get("fullLyrics") instanceof String s && !s.isBlank()) return s;
            }
        }

        if (dataObj instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> first) {
            for (String key : keys) {
                Object val = first.get(key);
                if (val instanceof String s && !s.isBlank()) return s;
            }
        }

        return null;
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getAlbum(String id, String link) {
        return getMap(u -> {
            UriBuilder b = u.path("/albums");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank())  b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    public Map<String, Object> getPlaylist(String id, String link) {
        return getMap(u -> {
            UriBuilder b = u.path("/playlists");
            if (id   != null && !id.isBlank())   b = b.queryParam("id",   id);
            if (link != null && !link.isBlank())  b = b.queryParam("link", link);
            return b.build();
        });
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    public Map<String, Object> getArtist(String id) {
        return getMap(u -> u.path("/artists/" + id).build());
    }

    public Map<String, Object> getArtistSongs(String id, int page,
                                               String sortBy, String sortOrder) {
        String sb = (sortBy    != null && !sortBy.isBlank())    ? sortBy    : "latest";
        String so = (sortOrder != null && !sortOrder.isBlank()) ? sortOrder : "desc";
        return getMap(u -> u.path("/artists/" + id + "/songs")
                .queryParam("page",      page)
                .queryParam("sortBy",    sb)
                .queryParam("sortOrder", so)
                .build());
    }

    public Map<String, Object> getArtistAlbums(String id, int page) {
        return getMap(u -> u.path("/artists/" + id + "/albums")
                .queryParam("page", page).build());
    }

    // ── CHARTS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getCharts() {
        String[] fallbacks = {"top hindi hits 2025", "bollywood hits 2025", "trending songs india 2025"};
        for (String q : fallbacks) {
            Map<String, Object> result = searchSongs(q, 1, DEFAULT_PAGE_SIZE);
            if (!isDataEmpty(result)) return result;
        }
        return Collections.emptyMap();
    }

    // ── PRIVATE HTTP HELPERS ──────────────────────────────────────────────────

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
            log.error("getMap | {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> getMapAllowNotFound(Function<UriBuilder, URI> uriFunction) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> reactor.core.publisher.Mono.empty()))
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
        if (dataObj instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> map) {
            return getLyricsFromMap(map);
        }
        if (dataObj instanceof Map<?, ?> map) return getLyricsFromMap(map);
        return getLyricsFromMap(songMap);
    }

    private String getLyricsFromMap(Map<?, ?> map) {
        if (map == null) return null;
        for (String key : new String[]{"lyrics", "lyric", "lyrics_snippet", "snippet", "lyricsText", "fullLyrics", "text"}) {
            Object val = map.get(key);
            if (val instanceof String s && !s.isBlank()) return s;
        }
        Object lyricsObj = map.get("lyrics");
        if (lyricsObj instanceof Map<?, ?> lMap) {
            Object snippet = lMap.get("snippet");
            if (snippet instanceof String s && !s.isBlank()) return s;
            Object full = lMap.get("lyrics");
            if (full instanceof String s && !s.isBlank()) return s;
        }
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