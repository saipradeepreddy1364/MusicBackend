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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
@SuppressWarnings("null")
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    /**
     * Ordered list of Piped API mirrors to try, from most preferred to least.
     * The service tries each in order and rotates on failure.
     */
    private static final List<String> PIPED_MIRRORS = List.of(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://api.piped.yt"
    );

    private final List<WebClient> mirrorClients;
    private final AtomicInteger mirrorIndex = new AtomicInteger(0);

    public JioSaavnService(
            @Value("${jiosaavn.base-url:https://api.piped.private.coffee}") String baseUrl) {

        // Build a WebClient per mirror for fallback support
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8_000)
                .responseTimeout(Duration.ofSeconds(20));

        List<WebClient> clients = new ArrayList<>();
        for (String mirror : PIPED_MIRRORS) {
            clients.add(WebClient.builder()
                    .baseUrl(mirror)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36")
                    .build());
        }
        this.mirrorClients = Collections.unmodifiableList(clients);

        log.info("JioSaavnService initialized with {} Piped API mirrors. Primary={}",
                PIPED_MIRRORS.size(), PIPED_MIRRORS.get(0));
    }

    // ── DYNAMIC BASE URL FOR STREAMS ──────────────────────────────────────────

    private String getBaseUrl() {
        try {
            org.springframework.web.context.request.RequestAttributes attrs = 
                    RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = request.getContextPath(); // e.g., "/api"
                
                StringBuilder sb = new StringBuilder();
                sb.append(scheme).append("://").append(serverName);
                if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
                    sb.append(":").append(serverPort);
                }
                sb.append(contextPath);
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("getBaseUrl | Failed to extract dynamic server URL: {}", e.getMessage());
        }
        return "http://localhost:8080/api";
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> search(String query, int page, int limit) {
        log.info("search | query={}, page={}, limit={}", query, page, limit);
        Map<String, Object> pipedResult = getMap(u -> u.path("/search")
                .queryParam("q", query)
                .queryParam("filter", "all")
                .build());

        List<Map<String, Object>> songsList = new ArrayList<>();
        List<Map<String, Object>> albumsList = new ArrayList<>();
        List<Map<String, Object>> artistsList = new ArrayList<>();
        List<Map<String, Object>> playlistsList = new ArrayList<>();

        Object itemsObj = pipedResult.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    String type = (String) item.get("type");
                    if ("video".equals(type) || "stream".equals(type)) {
                        songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                    } else if ("playlist".equals(type)) {
                        albumsList.add(mapPipedPlaylistToJioSaavnPlaylistOrAlbum(item, "album"));
                        playlistsList.add(mapPipedPlaylistToJioSaavnPlaylistOrAlbum(item, "playlist"));
                    } else if ("channel".equals(type)) {
                        artistsList.add(mapPipedChannelToJioSaavnArtist(item));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        
        Map<String, Object> songsMap = new LinkedHashMap<>();
        songsMap.put("results", songsList);
        data.put("songs", songsMap);

        Map<String, Object> albumsMap = new LinkedHashMap<>();
        albumsMap.put("results", albumsList);
        data.put("albums", albumsMap);

        Map<String, Object> artistsMap = new LinkedHashMap<>();
        artistsMap.put("results", artistsList);
        data.put("artists", artistsMap);

        Map<String, Object> playlistsMap = new LinkedHashMap<>();
        playlistsMap.put("results", playlistsList);
        data.put("playlists", playlistsMap);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchSongs(String query, int page, int limit) {
        log.info("searchSongs | query={}, page={}, limit={}", query, page, limit);
        Map<String, Object> pipedResult = getMap(u -> u.path("/search")
                .queryParam("q", query)
                .queryParam("filter", "videos")
                .build());

        List<Map<String, Object>> songsList = new ArrayList<>();
        Object itemsObj = pipedResult.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    String t = (String) item.get("type");
                    if ("video".equals(t) || "stream".equals(t)) {
                        songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", songsList.size());
        data.put("start", (page - 1) * limit + 1);
        data.put("results", songsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        log.info("searchAlbums | query={}, page={}, limit={}", query, page, limit);
        Map<String, Object> pipedResult = getMap(u -> u.path("/search")
                .queryParam("q", query)
                .queryParam("filter", "playlists")
                .build());

        List<Map<String, Object>> albumsList = new ArrayList<>();
        Object itemsObj = pipedResult.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    if ("playlist".equals(item.get("type"))) {
                        albumsList.add(mapPipedPlaylistToJioSaavnPlaylistOrAlbum(item, "album"));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", albumsList.size());
        data.put("start", (page - 1) * limit + 1);
        data.put("results", albumsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchArtists(String query, int page, int limit) {
        log.info("searchArtists | query={}, page={}, limit={}", query, page, limit);
        Map<String, Object> pipedResult = getMap(u -> u.path("/search")
                .queryParam("q", query)
                .queryParam("filter", "channels")
                .build());

        List<Map<String, Object>> artistsList = new ArrayList<>();
        Object itemsObj = pipedResult.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    if ("channel".equals(item.get("type"))) {
                        artistsList.add(mapPipedChannelToJioSaavnArtist(item));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", artistsList.size());
        data.put("start", (page - 1) * limit + 1);
        data.put("results", artistsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        log.info("searchPlaylists | query={}, page={}, limit={}", query, page, limit);
        Map<String, Object> pipedResult = getMap(u -> u.path("/search")
                .queryParam("q", query)
                .queryParam("filter", "playlists")
                .build());

        List<Map<String, Object>> playlistsList = new ArrayList<>();
        Object itemsObj = pipedResult.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    if ("playlist".equals(item.get("type"))) {
                        playlistsList.add(mapPipedPlaylistToJioSaavnPlaylistOrAlbum(item, "playlist"));
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", playlistsList.size());
        data.put("start", (page - 1) * limit + 1);
        data.put("results", playlistsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    // ── SONGS ─────────────────────────────────────────────────────────────────

    public Map<String, Object> getSongById(String id) {
        log.info("getSongById | songId={}", id);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + id).build());
        if (streamData == null || streamData.isEmpty()) {
            return Collections.emptyMap();
        }

        String directAudioUrl = extractBestAudioStream(streamData);
        Map<String, Object> songDetails = mapPipedVideoToJioSaavnSong(streamData, directAudioUrl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", List.of(songDetails));
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSongSuggestions(String id, int limit) {
        log.info("getSongSuggestions | songId={}, limit={}", id, limit);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + id).build());
        if (streamData == null || streamData.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> suggestions = new ArrayList<>();
        Object related = streamData.get("relatedStreams");
        if (related instanceof List<?> list) {
            for (Object itemObj : list) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    if ("video".equals(item.get("type")) || "stream".equals(item.get("type"))) {
                        suggestions.add(mapPipedVideoToJioSaavnSong(item, null));
                        if (suggestions.size() >= limit) break;
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", suggestions);
        return result;
    }

    // ── STREAM DIRECT URL PROXY ────────────────────────────────────────────────

    public String getDirectAudioStreamUrl(String id) {
        log.info("getDirectAudioStreamUrl | videoId={}", id);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + id).build());
        return extractBestAudioStream(streamData);
    }

    // ── LYRICS ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSongLyrics(String songId) {
        log.info("getSongLyrics | songId={}", songId);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + songId).build());
        if (streamData == null || streamData.isEmpty()) {
            return Collections.emptyMap();
        }

        Object subtitlesObj = streamData.get("subtitles");
        if (subtitlesObj instanceof List<?> list && !list.isEmpty()) {
            for (Object subObj : list) {
                if (subObj instanceof Map<?, ?> subMap) {
                    String subUrl = (String) subMap.get("url");
                    if (subUrl != null && !subUrl.isEmpty()) {
                        try {
                            WebClient activeClient = mirrorClients.get(mirrorIndex.get() % mirrorClients.size());
                            String vttContent = activeClient.get()
                                    .uri(URI.create(subUrl))
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .timeout(Duration.ofSeconds(10))
                                    .block();
                            if (vttContent != null) {
                                String cleanLyrics = cleanVttSubtitles(vttContent);
                                return Map.of("lyrics", cleanLyrics);
                            }
                        } catch (Exception e) {
                            log.warn("getSongLyrics | Failed to fetch/parse subtitles from {}: {}", subUrl, e.getMessage());
                        }
                    }
                }
            }
        }

        return Collections.emptyMap();
    }

    private String cleanVttSubtitles(String vtt) {
        String cleaned = vtt.replaceAll("WEBVTT", "")
                .replaceAll("(?m)^\\d+:\\d+:\\d+\\.\\d+ --> \\d+:\\d+:\\d+\\.\\d+\\s*$", "")
                .replaceAll("(?m)^\\d+\\s*$", "")
                .replaceAll("<[^>]*>", "")
                .trim();
        
        return cleaned.replaceAll("(?m)^[ \t]*\r?\n", "");
    }

    // ── ALBUMS ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAlbum(String id, String link) {
        log.info("getAlbum | id={}, link={}", id, link);
        String playlistId = id;
        if ((playlistId == null || playlistId.isBlank()) && link != null) {
            if (link.contains("list=")) {
                playlistId = link.substring(link.indexOf("list=") + 5);
                int amp = playlistId.indexOf('&');
                if (amp != -1) playlistId = playlistId.substring(0, amp);
            }
        }

        if (playlistId == null || playlistId.isBlank()) {
            return Collections.emptyMap();
        }

        final String finalPlaylistId = playlistId;
        Map<String, Object> playlistData = getMapAllowNotFound(u -> u.path("/playlists/" + finalPlaylistId).build());
        if (playlistData == null || playlistData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> album = new LinkedHashMap<>();
        album.put("id", playlistId);
        
        String name = (String) playlistData.getOrDefault("name", "YouTube Album");
        album.put("name", name);
        album.put("description", playlistData.getOrDefault("description", ""));
        album.put("year", "2026");
        album.put("language", "english");

        String thumbnailUrl = (String) playlistData.getOrDefault("thumbnailUrl", "");
        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "150x150", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "500x500", "url", thumbnailUrl));
        album.put("image", imageList);

        String uploader = (String) playlistData.getOrDefault("uploader", "YouTube");
        Map<String, Object> artists = new LinkedHashMap<>();
        artists.put("primary", List.of(Map.of("id", "youtube", "name", uploader, "role", "music", "type", "artist")));
        album.put("artists", artists);

        List<Map<String, Object>> songsList = new ArrayList<>();
        Object related = playlistData.get("relatedStreams");
        if (related instanceof List<?> list) {
            for (Object itemObj : list) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                }
            }
        }
        album.put("songs", songsList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", album);
        return result;
    }

    // ── PLAYLISTS ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlaylist(String id, String link) {
        log.info("getPlaylist | id={}, link={}", id, link);
        String playlistId = id;
        if ((playlistId == null || playlistId.isBlank()) && link != null) {
            if (link.contains("list=")) {
                playlistId = link.substring(link.indexOf("list=") + 5);
                int amp = playlistId.indexOf('&');
                if (amp != -1) playlistId = playlistId.substring(0, amp);
            }
        }

        if (playlistId == null || playlistId.isBlank()) {
            return Collections.emptyMap();
        }

        final String finalPlaylistId = playlistId;
        Map<String, Object> playlistData = getMapAllowNotFound(u -> u.path("/playlists/" + finalPlaylistId).build());
        if (playlistData == null || playlistData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> playlist = new LinkedHashMap<>();
        playlist.put("id", playlistId);
        
        String name = (String) playlistData.getOrDefault("name", "YouTube Playlist");
        playlist.put("name", name);
        playlist.put("description", playlistData.getOrDefault("description", ""));

        String thumbnailUrl = (String) playlistData.getOrDefault("thumbnailUrl", "");
        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "150x150", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "500x500", "url", thumbnailUrl));
        playlist.put("image", imageList);

        List<Map<String, Object>> songsList = new ArrayList<>();
        Object related = playlistData.get("relatedStreams");
        if (related instanceof List<?> list) {
            playlist.put("songCount", list.size());
            for (Object itemObj : list) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                }
            }
        } else {
            playlist.put("songCount", 0);
        }
        playlist.put("songs", songsList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", playlist);
        return result;
    }

    // ── ARTISTS ───────────────────────────────────────────────────────────────

    public Map<String, Object> getArtist(String id) {
        log.info("getArtist | channelId={}", id);
        Map<String, Object> channelData = getMapAllowNotFound(u -> u.path("/channel/" + id).build());
        if (channelData == null || channelData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> artist = new LinkedHashMap<>();
        artist.put("id", id);
        
        String name = (String) channelData.getOrDefault("name", "YouTube Artist");
        artist.put("name", name);
        
        String avatarUrl = (String) channelData.getOrDefault("avatarUrl", "");
        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", avatarUrl));
        imageList.add(Map.of("quality", "150x150", "url", avatarUrl));
        imageList.add(Map.of("quality", "500x500", "url", avatarUrl));
        artist.put("image", imageList);

        artist.put("followerCount", channelData.getOrDefault("subscriberCount", 0));
        artist.put("isVerified", channelData.getOrDefault("verified", false));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", artist);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getArtistSongs(String id, int page,
                                               String sortBy, String sortOrder) {
        log.info("getArtistSongs | channelId={}, page={}", id, page);
        Map<String, Object> channelData = getMapAllowNotFound(u -> u.path("/channel/" + id).build());
        if (channelData == null || channelData.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> songsList = new ArrayList<>();
        Object related = channelData.get("relatedStreams");
        if (related instanceof List<?> list) {
            for (Object itemObj : list) {
                if (itemObj instanceof Map<?, ?> itemMap) {
                    Map<String, Object> item = (Map<String, Object>) itemMap;
                    songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", songsList.size());
        data.put("start", (page - 1) * 50 + 1);
        data.put("results", songsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> getArtistAlbums(String id, int page) {
        log.info("getArtistAlbums | channelId={}", id);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", 0);
        data.put("start", 1);
        data.put("results", Collections.emptyList());
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    // ── CHARTS ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCharts() {
        log.info("getCharts | fetching trending");
        Map<String, Object> pipedResult = getMapAllowNotFound(u -> u.path("/trending")
                .queryParam("region", "IN")
                .build());

        List<Map<String, Object>> songsList = new ArrayList<>();
        if (pipedResult != null && !pipedResult.isEmpty()) {
            if (pipedResult instanceof List<?> list) {
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> itemMap) {
                        Map<String, Object> item = (Map<String, Object>) itemMap;
                        songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                    }
                }
            } else {
                Object itemsObj = pipedResult.get("items");
                if (itemsObj instanceof List<?> items) {
                    for (Object itemObj : items) {
                        if (itemObj instanceof Map<?, ?> itemMap) {
                            Map<String, Object> item = (Map<String, Object>) itemMap;
                            songsList.add(mapPipedVideoToJioSaavnSong(item, null));
                        }
                    }
                }
            }
        }

        if (songsList.isEmpty()) {
            return searchSongs("trending hindi songs 2026", 1, 50);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", songsList.size());
        data.put("start", 1);
        data.put("results", songsList);

        result.put("success", true);
        result.put("data", data);
        return result;
    }

    // ── MAPPING HELPERS ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapPipedVideoToJioSaavnSong(Map<String, Object> item, String directAudioUrl) {
        if (item == null) return Collections.emptyMap();
        Map<String, Object> song = new LinkedHashMap<>();

        String videoId = "";
        String url = (String) item.get("url");
        if (url != null) {
            if (url.contains("v=")) {
                videoId = url.substring(url.indexOf("v=") + 2);
                int amp = videoId.indexOf('&');
                if (amp != -1) videoId = videoId.substring(0, amp);
            } else {
                videoId = url.replace("/watch?v=", "").replace("watch?v=", "");
            }
        }
        if (videoId.isEmpty() && item.containsKey("videoId")) {
            videoId = (String) item.get("videoId");
        }
        if (videoId.isEmpty() && item.containsKey("id")) {
            videoId = (String) item.get("id");
        }

        String title = (String) item.getOrDefault("title", "");
        if (title.isEmpty() && item.containsKey("name")) {
            title = (String) item.get("name");
        }

        int duration = 0;
        Object durObj = item.get("duration");
        if (durObj instanceof Number n) {
            duration = n.intValue();
        }

        String uploaderName = (String) item.getOrDefault("uploaderName", "");
        if (uploaderName.isEmpty() && item.containsKey("uploader")) {
            uploaderName = (String) item.get("uploader");
        }
        if (uploaderName.isEmpty()) {
            uploaderName = "YouTube Creator";
        }

        String uploaderUrl = (String) item.getOrDefault("uploaderUrl", "");
        String channelId = "";
        if (uploaderUrl != null && uploaderUrl.contains("/channel/")) {
            channelId = uploaderUrl.substring(uploaderUrl.indexOf("/channel/") + 9);
        }
        if (channelId.isEmpty() && item.containsKey("uploaderId")) {
            channelId = (String) item.get("uploaderId");
        }

        String thumbnailUrl = (String) item.getOrDefault("thumbnail", "");
        if (thumbnailUrl.isEmpty() && item.containsKey("thumbnailUrl")) {
            thumbnailUrl = (String) item.get("thumbnailUrl");
        }

        song.put("id", videoId);
        song.put("name", title);
        song.put("duration", duration);

        String year = "2026";
        String uploadedDate = (String) item.get("uploadedDate");
        if (uploadedDate != null && uploadedDate.length() >= 4) {
            year = uploadedDate.substring(0, 4);
        } else if (item.containsKey("uploaded")) {
            Object uploaded = item.get("uploaded");
            if (uploaded instanceof Number n) {
                java.time.Instant instant = java.time.Instant.ofEpochMilli(n.longValue());
                year = String.valueOf(java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).getYear());
            }
        }
        song.put("year", year);
        song.put("language", "english");
        song.put("hasLyrics", false);

        long playCount = 0;
        Object viewsObj = item.get("views");
        if (viewsObj instanceof Number n) {
            playCount = n.longValue();
        }
        song.put("playCount", playCount);
        song.put("label", uploaderName);

        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "150x150", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "500x500", "url", thumbnailUrl));
        song.put("image", imageList);

        List<Map<String, String>> downloadUrls = new ArrayList<>();
        if (directAudioUrl != null && !directAudioUrl.isEmpty()) {
            downloadUrls.add(Map.of("quality", "96kbps", "url", directAudioUrl));
            downloadUrls.add(Map.of("quality", "160kbps", "url", directAudioUrl));
            downloadUrls.add(Map.of("quality", "320kbps", "url", directAudioUrl));
        } else {
            String proxyUrl = getBaseUrl() + "/songs/" + videoId + "/stream";
            downloadUrls.add(Map.of("quality", "96kbps", "url", proxyUrl));
            downloadUrls.add(Map.of("quality", "160kbps", "url", proxyUrl));
            downloadUrls.add(Map.of("quality", "320kbps", "url", proxyUrl));
        }
        song.put("downloadUrl", downloadUrls);

        Map<String, Object> artists = new LinkedHashMap<>();
        List<Map<String, Object>> primaryList = new ArrayList<>();
        Map<String, Object> primaryArtist = new LinkedHashMap<>();
        primaryArtist.put("id", channelId);
        primaryArtist.put("name", uploaderName);
        primaryArtist.put("role", "music");
        primaryArtist.put("image", Collections.emptyList());
        primaryArtist.put("type", "artist");
        primaryList.add(primaryArtist);

        artists.put("primary", primaryList);
        artists.put("featured", Collections.emptyList());
        artists.put("all", primaryList);
        song.put("artists", artists);

        song.put("album", Map.of("id", "youtube", "name", "YouTube"));

        return song;
    }

    private Map<String, Object> mapPipedPlaylistToJioSaavnPlaylistOrAlbum(Map<String, Object> item, String type) {
        Map<String, Object> res = new LinkedHashMap<>();
        
        String url = (String) item.get("url");
        String playlistId = "";
        if (url != null) {
            playlistId = url.replace("/playlist?list=", "").replace("playlist?list=", "");
        }
        if (playlistId.isEmpty() && item.containsKey("playlistId")) {
            playlistId = (String) item.get("playlistId");
        }

        String name = (String) item.getOrDefault("name", "");
        if (name.isEmpty() && item.containsKey("title")) {
            name = (String) item.get("title");
        }

        String thumbnailUrl = (String) item.getOrDefault("thumbnail", "");
        if (thumbnailUrl.isEmpty() && item.containsKey("thumbnailUrl")) {
            thumbnailUrl = (String) item.get("thumbnailUrl");
        }

        res.put("id", playlistId);
        res.put("name", name);
        res.put("description", item.getOrDefault("description", "YouTube Playlist"));
        
        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "150x150", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "500x500", "url", thumbnailUrl));
        res.put("image", imageList);

        if ("album".equals(type)) {
            res.put("artist", item.getOrDefault("uploaderName", "YouTube"));
            res.put("year", "2026");
            res.put("songs", Collections.emptyList());
        } else {
            res.put("songCount", item.getOrDefault("videos", 0));
            res.put("songs", Collections.emptyList());
        }

        return res;
    }

    private Map<String, Object> mapPipedChannelToJioSaavnArtist(Map<String, Object> item) {
        Map<String, Object> res = new LinkedHashMap<>();
        
        String url = (String) item.get("url");
        String channelId = "";
        if (url != null) {
            channelId = url.replace("/channel/", "").replace("channel/", "");
        }
        if (channelId.isEmpty() && item.containsKey("channelId")) {
            channelId = (String) item.get("channelId");
        }

        String name = (String) item.getOrDefault("name", "");
        if (name.isEmpty() && item.containsKey("title")) {
            name = (String) item.get("title");
        }

        String thumbnailUrl = (String) item.getOrDefault("thumbnail", "");
        if (thumbnailUrl.isEmpty() && item.containsKey("thumbnailUrl")) {
            thumbnailUrl = (String) item.get("thumbnailUrl");
        }

        res.put("id", channelId);
        res.put("name", name);
        
        List<Map<String, String>> imageList = new ArrayList<>();
        imageList.add(Map.of("quality", "50x50", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "150x150", "url", thumbnailUrl));
        imageList.add(Map.of("quality", "500x500", "url", thumbnailUrl));
        res.put("image", imageList);

        res.put("followerCount", item.getOrDefault("subscribers", 0));
        res.put("isVerified", false);
        
        return res;
    }

    @SuppressWarnings("unchecked")
    private String extractBestAudioStream(Map<String, Object> streamData) {
        if (streamData == null) return null;
        Object audioStreamsObj = streamData.get("audioStreams");
        if (audioStreamsObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                return (String) m.get("url");
            }
        }
        return null;
    }

    // ── AUDIO DOWNLOAD / PROXY ────────────────────────────────────────────────

    /**
     * Fetches and returns the raw audio bytes of a YouTube video via Piped.
     * Used by the download endpoint to stream audio directly to the Android client
     * for offline caching. Returns null if audio URL cannot be resolved.
     */
    public byte[] fetchAudioBytes(String videoId) {
        log.info("fetchAudioBytes | videoId={}", videoId);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + videoId).build());
        if (streamData == null || streamData.isEmpty()) {
            log.warn("fetchAudioBytes | No stream data for videoId={}", videoId);
            return null;
        }
        String audioUrl = extractBestAudioStream(streamData);
        if (audioUrl == null || audioUrl.isEmpty()) {
            log.warn("fetchAudioBytes | No audio URL found for videoId={}", videoId);
            return null;
        }
        log.info("fetchAudioBytes | Downloading from url={}", audioUrl);
        try {
            // Use a raw WebClient without base URL for absolute audio URLs
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
            log.error("fetchAudioBytes | Failed to download audio for videoId={}: {}", videoId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the direct audio stream URL for the given video ID
     * without fetching actual bytes — used for redirect-based streaming.
     */
    public String resolveAudioUrl(String videoId) {
        log.info("resolveAudioUrl | videoId={}", videoId);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + videoId).build());
        return extractBestAudioStream(streamData);
    }

    /**
     * Returns all available video stream URLs for a YouTube video, grouped by quality.
     * Includes 360p, 480p, 720p, 1080p etc. where available.
     * Returns an empty list if no video streams are found.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVideoStreamUrls(String videoId) {
        log.info("getVideoStreamUrls | videoId={}", videoId);
        Map<String, Object> streamData = getMapAllowNotFound(u -> u.path("/streams/" + videoId).build());
        if (streamData == null || streamData.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        Object videoStreamsObj = streamData.get("videoStreams");
        if (videoStreamsObj instanceof List<?> videoStreams) {
            for (Object vsObj : videoStreams) {
                if (vsObj instanceof Map<?, ?> vs) {
                    String url      = (String) vs.get("url");
                    String quality  = (String) vs.get("quality");
                    String mimeType = (String) vs.get("mimeType");
                    boolean videoOnly = Boolean.TRUE.equals(vs.get("videoOnly"));
                    if (url == null || url.isEmpty()) continue;
                    // Skip video-only streams (no audio track) unless no others exist
                    if (videoOnly) continue;
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("quality",  quality  != null ? quality  : "unknown");
                    entry.put("mimeType", mimeType != null ? mimeType : "video/mp4");
                    entry.put("url",      url);
                    result.add(entry);
                }
            }
            // Fallback: if all streams are videoOnly, include them anyway
            if (result.isEmpty()) {
                for (Object vsObj : videoStreams) {
                    if (vsObj instanceof Map<?, ?> vs) {
                        String url      = (String) vs.get("url");
                        String quality  = (String) vs.get("quality");
                        String mimeType = (String) vs.get("mimeType");
                        if (url == null || url.isEmpty()) continue;
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("quality",  quality  != null ? quality  : "unknown");
                        entry.put("mimeType", mimeType != null ? mimeType : "video/mp4");
                        entry.put("url",      url);
                        entry.put("videoOnly", true);
                        result.add(entry);
                    }
                }
            }
        }
        log.info("getVideoStreamUrls | Found {} video streams for videoId={}", result.size(), videoId);
        return result;
    }

    // ── PRIVATE HTTP HELPERS ──────────────────────────────────────────────────

    /**
     * Fetches a JSON map from Piped, trying each mirror in order.
     * On deserialization failure (e.g. HTML page returned), rotates to next mirror.
     */
    private Map<String, Object> getMap(Function<UriBuilder, URI> uriFunction) {
        for (int attempt = 0; attempt < mirrorClients.size(); attempt++) {
            int idx = (mirrorIndex.get() + attempt) % mirrorClients.size();
            WebClient client = mirrorClients.get(idx);
            try {
                Map<String, Object> response = client.get()
                        .uri(uriFunction)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(20))
                        .block();
                if (response != null && !response.isEmpty()) {
                    mirrorIndex.set(idx); // remember working mirror
                    return response;
                }
            } catch (Exception e) {
                log.warn("getMap | Mirror {} failed: {}", PIPED_MIRRORS.get(idx), e.getMessage());
            }
        }
        log.error("getMap | All {} mirrors failed", mirrorClients.size());
        return Collections.emptyMap();
    }

    private Map<String, Object> getMapAllowNotFound(Function<UriBuilder, URI> uriFunction) {
        for (int attempt = 0; attempt < mirrorClients.size(); attempt++) {
            int idx = (mirrorIndex.get() + attempt) % mirrorClients.size();
            WebClient client = mirrorClients.get(idx);
            try {
                Map<String, Object> response = client.get()
                        .uri(uriFunction)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), clientResponse ->
                                clientResponse.bodyToMono(String.class)
                                         .flatMap(body -> reactor.core.publisher.Mono.empty()))
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(20))
                        .block();
                if (response != null) {
                    mirrorIndex.set(idx);
                    return response;
                }
            } catch (Exception e) {
                log.warn("getMapAllowNotFound | Mirror {} failed: {}", PIPED_MIRRORS.get(idx), e.getMessage());
            }
        }
        return Collections.emptyMap();
    }
}