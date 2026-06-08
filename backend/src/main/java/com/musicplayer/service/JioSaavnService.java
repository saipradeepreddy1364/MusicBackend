package com.musicplayer.service;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Service
@SuppressWarnings("null")
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    private final WebClient webClient;

    public JioSaavnService(
            @Value("${jiosaavn.base-url:https://jiosaavn-api.pradeepreddypalagiri.workers.dev}") String baseUrl) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8_000)
                .responseTimeout(Duration.ofSeconds(20));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36")
                .build();

        log.info("JioSaavnService initialized with base URL: {}", baseUrl);
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    public Map<String, Object> search(String query, int page, int limit) {
        log.info("search | query={}, page={}, limit={}", query, page, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("search failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> searchSongs(String query, int page, int limit) {
        log.info("searchSongs | query={}, page={}, limit={}", query, page, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search/songs")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("searchSongs failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        log.info("searchAlbums | query={}, page={}, limit={}", query, page, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search/albums")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("searchAlbums failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> searchArtists(String query, int page, int limit) {
        log.info("searchArtists | query={}, page={}, limit={}", query, page, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search/artists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("searchArtists failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        log.info("searchPlaylists | query={}, page={}, limit={}", query, page, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search/playlists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("searchPlaylists failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── SONGS ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getSongById(String id) {
        log.info("getSongById | songId={}", id);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/songs")
                            .queryParam("ids", id)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getSongById failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> getSongSuggestions(String id, int limit) {
        log.info("getSongSuggestions | songId={}, limit={}", id, limit);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/songs/" + id + "/suggestions")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getSongSuggestions failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── STREAM DIRECT URL PROXY ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String getDirectAudioStreamUrl(String id) {
        log.info("getDirectAudioStreamUrl | songId={}", id);
        Map<String, Object> songData = getSongById(id);
        if (songData == null || !Boolean.TRUE.equals(songData.get("success"))) {
            return null;
        }
        Object dataObj = songData.get("data");
        if (dataObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> songMap) {
                Object downloadUrlObj = songMap.get("downloadUrl");
                if (downloadUrlObj instanceof List<?> urlList && !urlList.isEmpty()) {
                    // Pick the highest quality url (last one)
                    Object last = urlList.get(urlList.size() - 1);
                    if (last instanceof Map<?, ?> urlMap) {
                        return (String) urlMap.get("url");
                    }
                }
            }
        }
        return null;
    }

    // ── LYRICS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getSongLyrics(String songId) {
        log.info("getSongLyrics | songId={}", songId);
        // Stub/fallback since worker does not expose lyrics endpoint
        return Collections.emptyMap();
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getAlbum(String id, String link) {
        log.info("getAlbum | id={}, link={}", id, link);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/albums");
                        if (id != null && !id.isBlank()) {
                            uriBuilder.queryParam("id", id);
                        } else if (link != null && !link.isBlank()) {
                            uriBuilder.queryParam("link", link);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getAlbum failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    public Map<String, Object> getPlaylist(String id, String link) {
        log.info("getPlaylist | id={}, link={}", id, link);
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/playlists");
                        if (id != null && !id.isBlank()) {
                            uriBuilder.queryParam("id", id);
                        } else if (link != null && !link.isBlank()) {
                            uriBuilder.queryParam("link", link);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getPlaylist failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    public Map<String, Object> getArtist(String id) {
        log.info("getArtist | channelId={}", id);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/artists/" + id).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getArtist failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> getArtistSongs(String id, int page,
                                               String sortBy, String sortOrder) {
        log.info("getArtistSongs | channelId={}, page={}", id, page);
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/artists/" + id + "/songs")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("getArtistSongs failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getArtistAlbums(String id, int page) {
        log.info("getArtistAlbums | channelId={}", id);
        try {
            Map<String, Object> artistData = getArtist(id);
            List<Object> albums = Collections.emptyList();
            if (artistData != null && Boolean.TRUE.equals(artistData.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) artistData.get("data");
                if (data != null && data.get("topAlbums") instanceof List<?>) {
                    albums = (List<Object>) data.get("topAlbums");
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", albums.size());
            data.put("start", 1);
            data.put("results", albums);
            result.put("success", true);
            result.put("data", data);
            return result;
        } catch (Exception e) {
            log.error("getArtistAlbums failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── CHARTS ────────────────────────────────────────────────────────────────

    public Map<String, Object> getCharts() {
        log.info("getCharts | fetching trending");
        // Fallback directly to searching trending songs
        return searchSongs("trending hindi songs 2026", 1, 50);
    }

    // ── AUDIO DOWNLOAD / PROXY ────────────────────────────────────────────────

    public byte[] fetchAudioBytes(String songId) {
        log.info("fetchAudioBytes | songId={}", songId);
        String audioUrl = getDirectAudioStreamUrl(songId);
        if (audioUrl == null || audioUrl.isEmpty()) {
            log.warn("fetchAudioBytes | No audio URL found for songId={}", songId);
            return null;
        }
        log.info("fetchAudioBytes | Downloading from url={}", audioUrl);
        try {
            WebClient raw = WebClient.builder()
                    .defaultHeader("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36")
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
                    .build();
            return raw.get()
                    .uri(URI.create(audioUrl))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(90))
                    .block();
        } catch (Exception e) {
            log.error("fetchAudioBytes | Failed to download audio for songId={}: {}", songId, e.getMessage());
            return null;
        }
    }

    public String resolveAudioUrl(String songId) {
        return getDirectAudioStreamUrl(songId);
    }

    public List<Map<String, Object>> getVideoStreamUrls(String videoId) {
        return Collections.emptyList();
    }
}