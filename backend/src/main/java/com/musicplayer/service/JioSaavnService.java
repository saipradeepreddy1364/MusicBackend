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

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSongLyrics(String songId) {
        log.info("getSongLyrics | songId={}", songId);
        Map<String, Object> songData = getSongById(songId);
        if (songData == null || !Boolean.TRUE.equals(songData.get("success"))) {
            log.warn("getSongLyrics | Failed to fetch song details for ID={}", songId);
            return Collections.emptyMap();
        }
        
        Object dataObj = songData.get("data");
        if (!(dataObj instanceof List<?> list) || list.isEmpty()) {
            log.warn("getSongLyrics | No data list in song details for ID={}", songId);
            return Collections.emptyMap();
        }
        
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> songMap)) {
            log.warn("getSongLyrics | First item in song details is not a Map for ID={}", songId);
            return Collections.emptyMap();
        }

        String trackName = (String) songMap.get("name");
        if (trackName == null || trackName.trim().isEmpty()) {
            log.warn("getSongLyrics | Track name is empty in details for ID={}", songId);
            return Collections.emptyMap();
        }

        // Extract album name
        String albumName = "";
        Object albumObj = songMap.get("album");
        if (albumObj instanceof Map<?, ?> albumMap) {
            albumName = (String) albumMap.get("name");
        }

        // Extract duration
        int duration = 0;
        Object durationObj = songMap.get("duration");
        if (durationObj instanceof Number number) {
            duration = number.intValue();
        }

        // Extract artist name
        String artistName = "";
        Object artistsObj = songMap.get("artists");
        if (artistsObj instanceof Map<?, ?> artistsMap) {
            Object primaryObj = artistsMap.get("primary");
            if (primaryObj instanceof List<?> primaryList && !primaryList.isEmpty()) {
                List<String> artistNames = new ArrayList<>();
                for (Object art : primaryList) {
                    if (art instanceof Map<?, ?> artMap) {
                        String name = (String) artMap.get("name");
                        if (name != null && !name.trim().isEmpty()) {
                            artistNames.add(name);
                        }
                    }
                }
                if (!artistNames.isEmpty()) {
                    artistName = String.join(", ", artistNames);
                }
            }
        }

        log.info("getSongLyrics | Resolved metadata: trackName='{}', artistName='{}', albumName='{}', duration={}", 
                trackName, artistName, albumName, duration);

        WebClient lrcClient = WebClient.builder()
                .baseUrl("https://lrclib.net")
                .defaultHeader("User-Agent", "RhythmWeaver/1.0 (https://github.com/saipradeepreddy1364/rhythm-weaver)")
                .build();

        // Try GET /api/get
        try {
            final String finalArtist = artistName;
            final String finalAlbum = albumName;
            final int finalDuration = duration;
            Map<String, Object> lrcResponse = lrcClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/get")
                            .queryParam("track_name", trackName)
                            .queryParam("artist_name", finalArtist)
                            .queryParam("album_name", finalAlbum)
                            .queryParam("duration", finalDuration)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (lrcResponse != null) {
                Map<String, Object> formatted = formatLyricsResponse(lrcResponse);
                if (!formatted.isEmpty()) {
                    log.info("getSongLyrics | Lyrics successfully retrieved via GET /api/get");
                    return formatted;
                }
            }
        } catch (Exception e) {
            log.warn("getSongLyrics | LRCLIB exact match failed for '{}' - '{}': {}", trackName, artistName, e.getMessage());
        }

        // Fallback: search query with trackName and artistName
        try {
            final String query = trackName + " " + artistName;
            List<Map<String, Object>> searchResults = lrcClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/search")
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (searchResults != null && !searchResults.isEmpty()) {
                // Return the best match (first one)
                Map<String, Object> bestMatch = searchResults.get(0);
                Map<String, Object> formatted = formatLyricsResponse(bestMatch);
                if (!formatted.isEmpty()) {
                    log.info("getSongLyrics | Lyrics successfully retrieved via search fallback query '{}'", query);
                    return formatted;
                }
            }
        } catch (Exception e) {
            log.error("getSongLyrics | LRCLIB search fallback failed for '{}': {}", trackName, e.getMessage());
        }

        log.warn("getSongLyrics | No lyrics found on LRCLIB for ID={}", songId);
        return Collections.emptyMap();
    }

    private Map<String, Object> formatLyricsResponse(Map<String, Object> lrc) {
        if (lrc == null) {
            return Collections.emptyMap();
        }
        String plain = (String) lrc.get("plainLyrics");
        if (plain == null || plain.trim().isEmpty()) {
            plain = (String) lrc.get("syncedLyrics");
        }
        if (plain == null || plain.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("lyrics", plain);
        return response;
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