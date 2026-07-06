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

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@SuppressWarnings("null")
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    private final WebClient webClient;
    private final ExecutorService homeExecutor = Executors.newFixedThreadPool(16);

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
        String audioUrl = resolveAudioUrl(songId);
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

    public String resolveYoutubeUrl(String videoId) {
        log.info("resolveYoutubeUrl | videoId={}", videoId);
        String[] cmd = {
            "python", "-c",
            "import yt_dlp; ydl = yt_dlp.YoutubeDL({'format': 'bestaudio/best', 'quiet': True, 'no_warnings': True}); print(ydl.extract_info('https://www.youtube.com/watch?v=" + videoId + "', download=False)['url'])"
        };
        if (new java.io.File("/opt/venv/bin/python").exists()) {
            cmd[0] = "/opt/venv/bin/python";
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            String lastUrl = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    lastUrl = line;
                }
            }
            process.waitFor();
            if (lastUrl != null) {
                log.info("resolveYoutubeUrl | Successfully resolved YouTube stream URL for videoId={}", videoId);
                return lastUrl;
            }
        } catch (Exception e) {
            log.error("resolveYoutubeUrl | Failed to resolve videoId={}: {}", videoId, e.getMessage());
        }
        return null;
    }

    public String resolveAudioUrl(String songId) {
        if (songId != null && songId.startsWith("yt-")) {
            return resolveYoutubeUrl(songId.replace("yt-", ""));
        }
        return getDirectAudioStreamUrl(songId);
    }

    public List<Map<String, Object>> getVideoStreamUrls(String videoId) {
        return Collections.emptyList();
    }

    // ── CACHED HOME DATA WITH FRONTEND-PARITY DYNAMIC LOGIC ──────────────────

    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
    private static final int PREV_YEAR = CURRENT_YEAR - 1;

    private static final List<String> HINDI_QUERIES = List.of(
        "trending hindi songs " + CURRENT_YEAR,
        "top hindi hits " + CURRENT_YEAR,
        "best new hindi songs " + CURRENT_YEAR,
        "viral hindi songs " + CURRENT_YEAR,
        "popular hindi songs " + CURRENT_YEAR,
        "latest hindi film songs " + CURRENT_YEAR,
        "hindi chartbusters " + CURRENT_YEAR,
        "super hit hindi songs " + CURRENT_YEAR
    );

    private static final List<String> TELUGU_QUERIES = List.of(
        "trending telugu songs " + CURRENT_YEAR,
        "top tollywood hits " + CURRENT_YEAR,
        "best telugu songs " + CURRENT_YEAR,
        "new telugu songs " + CURRENT_YEAR,
        "viral telugu songs " + CURRENT_YEAR,
        "popular telugu film songs " + CURRENT_YEAR,
        "telugu chartbusters " + CURRENT_YEAR,
        "super hit telugu songs " + CURRENT_YEAR
    );

    private static final List<String> TAMIL_QUERIES = List.of(
        "trending tamil songs " + CURRENT_YEAR,
        "top kollywood hits " + CURRENT_YEAR,
        "best tamil songs " + CURRENT_YEAR,
        "new tamil songs " + CURRENT_YEAR,
        "viral tamil songs " + CURRENT_YEAR,
        "popular tamil film songs " + CURRENT_YEAR,
        "tamil chartbusters " + CURRENT_YEAR,
        "super hit tamil songs " + CURRENT_YEAR
    );

    private static final List<String> BOLLYWOOD_QUERIES = List.of(
        "latest bollywood hits " + CURRENT_YEAR,
        "new bollywood songs " + CURRENT_YEAR,
        "bollywood blockbuster " + CURRENT_YEAR,
        "hit bollywood dance songs " + CURRENT_YEAR,
        "bollywood romantic " + CURRENT_YEAR,
        "bollywood party songs " + CURRENT_YEAR,
        "bollywood new release " + CURRENT_YEAR,
        "top bollywood songs " + CURRENT_YEAR
    );

    private static final List<String> PUNJABI_QUERIES = List.of(
        "top punjabi songs " + CURRENT_YEAR,
        "trending punjabi " + CURRENT_YEAR,
        "new punjabi hits " + CURRENT_YEAR,
        "best punjabi songs " + CURRENT_YEAR,
        "viral punjabi songs " + CURRENT_YEAR,
        "popular punjabi " + CURRENT_YEAR,
        "punjabi chartbusters " + CURRENT_YEAR,
        "super hit punjabi songs " + CURRENT_YEAR
    );

    private static final List<String> ROMANTIC_QUERIES = List.of(
        "hindi romantic songs " + CURRENT_YEAR,
        "love songs bollywood " + CURRENT_YEAR,
        "best romantic hindi songs",
        "heart touching songs hindi",
        "sad romantic songs hindi",
        "romantic duets bollywood",
        "romantic telugu songs " + CURRENT_YEAR,
        "best love songs indian"
    );

    private static final List<String> PARTY_QUERIES = List.of(
        "party songs hindi " + CURRENT_YEAR,
        "bollywood dance hits " + CURRENT_YEAR,
        "high energy hindi songs",
        "dj remix bollywood " + CURRENT_YEAR,
        "club songs bollywood",
        "dance floor hits india",
        "party anthems hindi",
        "bollywood bangers " + CURRENT_YEAR
    );

    private static final List<String> RETRO_QUERIES = List.of(
        "old hindi classic songs",
        "90s bollywood hits",
        "80s hindi songs superhit",
        "retro bollywood classics",
        "evergreen hindi songs",
        "golden era bollywood",
        "vintage hindi film songs",
        "old is gold hindi songs"
    );

    private static final List<String> KANNADA_QUERIES = List.of(
        "trending kannada songs " + CURRENT_YEAR,
        "top sandalwood hits " + CURRENT_YEAR,
        "best kannada songs " + CURRENT_YEAR,
        "new kannada songs " + CURRENT_YEAR,
        "popular kannada film songs",
        "viral kannada songs " + CURRENT_YEAR,
        "kannada chartbusters " + CURRENT_YEAR,
        "super hit kannada songs"
    );

    private static final List<String> MALAYALAM_QUERIES = List.of(
        "trending malayalam songs " + CURRENT_YEAR,
        "top mollywood hits " + CURRENT_YEAR,
        "best malayalam songs " + CURRENT_YEAR,
        "new malayalam songs " + CURRENT_YEAR,
        "popular malayalam film songs",
        "viral malayalam songs " + CURRENT_YEAR,
        "malayalam chartbusters " + CURRENT_YEAR,
        "super hit malayalam songs"
    );

    private static final List<String> HINDI_YEAR_QUERIES = List.of(
        "new hindi film songs " + CURRENT_YEAR,
        "hindi movie songs " + CURRENT_YEAR,
        "bollywood songs " + CURRENT_YEAR,
        "superhit hindi " + CURRENT_YEAR,
        "hindi blockbuster " + CURRENT_YEAR,
        "latest hindi songs " + CURRENT_YEAR,
        "hindi new release " + CURRENT_YEAR,
        "top hindi film " + CURRENT_YEAR
    );

    private static final List<String> TELUGU_YEAR_QUERIES = List.of(
        "new telugu film songs " + CURRENT_YEAR,
        "tollywood songs " + CURRENT_YEAR,
        "telugu movie songs " + CURRENT_YEAR,
        "superhit telugu " + CURRENT_YEAR,
        "telugu blockbuster " + CURRENT_YEAR,
        "latest telugu songs " + CURRENT_YEAR,
        "telugu new release " + CURRENT_YEAR,
        "top telugu film " + CURRENT_YEAR
    );

    // ── Telugu melody / classic pools (past-year hits for Quick Picks diversity) ─
    private static final List<String> TELUGU_MELODY_QUERIES = List.of(
        "telugu melody songs 2023",
        "telugu melody songs 2022",
        "telugu melody songs 2024",
        "best telugu melody hits 2021",
        "super hit telugu melody 2020",
        "telugu soft melody songs",
        "telugu heart touching songs",
        "telugu feel good songs",
        "best telugu love songs",
        "telugu melody songs sid sriram"
    );

    private static final List<String> TELUGU_CLASSIC_QUERIES = List.of(
        "sp balasubrahmanyam telugu songs",
        "old telugu classic songs",
        "evergreen telugu songs",
        "telugu retro hits",
        "80s 90s telugu superhit songs",
        "ilayaraja telugu songs",
        "mm keeravani telugu songs",
        "dsp telugu hits 2018 2019 2020",
        "thaman s telugu hits",
        "best telugu songs all time"
    );

    private static final List<String> FILM_DISCOVERY_QUERIES_POOL = List.of(
        "new hindi film songs " + CURRENT_YEAR,
        "latest bollywood movie " + CURRENT_YEAR,
        "hindi movie release " + CURRENT_YEAR,
        "bollywood new movie songs " + CURRENT_YEAR,
        "hindi film songs " + PREV_YEAR + " " + CURRENT_YEAR,
        "bollywood blockbuster songs " + CURRENT_YEAR,
        "new bollywood film " + CURRENT_YEAR,
        "hindi movie soundtrack " + CURRENT_YEAR,
        "bollywood superhit movie " + CURRENT_YEAR,
        "hindi action movie songs " + CURRENT_YEAR,
        "hindi romantic movie " + CURRENT_YEAR,
        "bollywood drama songs " + CURRENT_YEAR,
        "new telugu film songs " + CURRENT_YEAR,
        "latest tollywood movie " + CURRENT_YEAR,
        "telugu movie release " + CURRENT_YEAR,
        "tollywood new movie songs " + CURRENT_YEAR,
        "telugu film songs " + PREV_YEAR + " " + CURRENT_YEAR,
        "telugu blockbuster movie " + CURRENT_YEAR,
        "new tollywood film " + CURRENT_YEAR,
        "telugu movie soundtrack " + CURRENT_YEAR,
        "tollywood superhit movie " + CURRENT_YEAR,
        "telugu action movie songs " + CURRENT_YEAR,
        "telugu romantic movie " + CURRENT_YEAR,
        "tollywood drama songs " + CURRENT_YEAR,
        "new tamil film songs " + CURRENT_YEAR,
        "kollywood new movie songs " + CURRENT_YEAR,
        "tamil movie release " + CURRENT_YEAR,
        "tamil blockbuster " + CURRENT_YEAR,
        "new kannada film songs " + CURRENT_YEAR,
        "sandalwood new movie songs " + CURRENT_YEAR,
        "new malayalam film songs " + CURRENT_YEAR,
        "mollywood new movie songs " + CURRENT_YEAR
    );

    private static final List<String> ARTIST_DISCOVERY_QUERIES = List.of(
        "arijit singh songs",
        "shreya ghoshal songs",
        "sonu nigam songs",
        "ar rahman hit songs",
        "sid sriram songs",
        "anirudh ravichander songs",
        "neha kakkar songs",
        "atif aslam songs",
        "diljit dosanjh songs",
        "sp balasubrahmanyam songs",
        "kishore kumar songs",
        "lata mangeshkar songs",
        "armaan malik songs",
        "darshan raval songs",
        "b praak songs",
        "udit narayan songs",
        "kumar sanu songs",
        "asha bhosle songs",
        "sunidhi chauhan songs",
        "badshah songs",
        "guru randhawa songs"
    );

    private volatile Map<String, Object> cachedHomeData = null;
    private volatile long cacheExpiryTime = 0;
    private final Object cacheLock = new Object();

    /**
     * Pre-warm the home data cache on application startup so the first user request
     * is served instantly from cache instead of triggering a cold build.
     */
    @PostConstruct
    public void warmUpHomeDataCache() {
        log.info("warmUpHomeDataCache | Pre-warming home data cache in background...");
        CompletableFuture.runAsync(() -> {
            try {
                synchronized (cacheLock) {
                    if (cachedHomeData != null && System.currentTimeMillis() < cacheExpiryTime) {
                        log.info("warmUpHomeDataCache | Cache already warm.");
                        return;
                    }
                    Map<String, Object> data = buildHomeData();
                    cachedHomeData = data;
                    cacheExpiryTime = System.currentTimeMillis() + Duration.ofHours(3).toMillis();
                }
                log.info("warmUpHomeDataCache | Home data cache populated successfully.");
            } catch (Exception e) {
                log.warn("warmUpHomeDataCache | Failed to pre-warm cache: {}", e.getMessage());
            }
        });
    }

    public Map<String, Object> getHomeData() {
        synchronized (cacheLock) {
            if (cachedHomeData != null && System.currentTimeMillis() < cacheExpiryTime) {
                log.info("getHomeData | Returning cached home screen payload");
                return cachedHomeData;
            }
            
            log.info("getHomeData | Cache miss or expired. Building home data...");
            Map<String, Object> freshData = buildHomeData();
            cachedHomeData = freshData;
            cacheExpiryTime = System.currentTimeMillis() + Duration.ofHours(3).toMillis();
            return freshData;
        }
    }

    private int getTodaysSeed() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // 0-indexed
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return year * 10000 + month * 100 + day;
    }

    private <T> List<T> seededShuffle(List<T> list, int seed) {
        List<T> arr = new ArrayList<>(list);
        double curSeed = seed;
        for (int i = arr.size() - 1; i > 0; i--) {
            double x = Math.sin(curSeed++) * 10000;
            double randVal = x - Math.floor(x);
            int j = (int) Math.floor(randVal * (i + 1));
            T temp = arr.get(i);
            arr.set(i, arr.get(j));
            arr.set(j, temp);
        }
        return arr;
    }

    private String pickQuery(List<String> pool, int seed) {
        if (pool == null || pool.isEmpty()) return "";
        double indexVal = Math.floor(Math.abs(Math.sin(seed + getTodaysSeed())) * pool.size());
        int index = (int) indexVal;
        return pool.get(index % pool.size());
    }

    private boolean isDevotionalSong(Map<String, Object> song) {
        String title = ((String) song.getOrDefault("name", "")).toLowerCase();
        String albumName = "";
        Object albumObj = song.get("album");
        if (albumObj instanceof Map<?, ?> albumMap) {
            Object nameVal = albumMap.get("name");
            if (nameVal instanceof String ns) {
                albumName = ns.toLowerCase();
            }
        }
        String movie = ((String) song.getOrDefault("movie", "")).toLowerCase();
        
        List<String> keywords = List.of(
            "bhajan", "aarti", "chalisa", "devotional", "bhakti", "mantra", 
            "stotram", "dhun", "stotra", "shlok", "shloka", "kirtan", 
            "hanuman chalisa", "shri ram", "krishna bhajan", "ganesha bhajan",
            "shiv bhajan", "sai baba", "spiritual", "durga chalisa"
        );
        
        for (String kw : keywords) {
            if (title.contains(kw) || albumName.contains(kw) || movie.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String getArtistName(Map<String, Object> song) {
        String field = "";
        if (song.get("primaryArtists") instanceof String pa) {
            field = pa;
        } else if (song.get("singers") instanceof String si) {
            field = si;
        } else if (song.get("artists") instanceof Map<?, ?> artistsMap) {
            Object primaryObj = artistsMap.get("primary");
            if (primaryObj instanceof List<?> primaryList && !primaryList.isEmpty()) {
                Object first = primaryList.get(0);
                if (first instanceof Map<?, ?> artMap) {
                    field = (String) artMap.get("name");
                }
            }
        }
        if (field == null || field.isEmpty()) {
            field = (String) song.get("artist");
        }
        if (field == null || field.isEmpty()) {
            field = "";
        }
        String name = field.split(",")[0];
        return name != null ? name.trim() : "";
    }

    private List<Map<String, Object>> fetchSongsList(String query, int limit) {
        Map<String, Object> res = searchSongs(query, 1, limit);
        if (res == null || !Boolean.TRUE.equals(res.get("success"))) {
            return Collections.emptyList();
        }
        Object dataObj = res.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object resultsObj = dataMap.get("results");
            if (resultsObj instanceof List<?> resultsList) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Object obj : resultsList) {
                    if (obj instanceof Map<?, ?> songMap) {
                        list.add((Map<String, Object>) songMap);
                    }
                }
                return list;
            }
        }
        return Collections.emptyList();
    }

    private Map<String, Object> buildHomeData() {
        log.info("buildHomeData | Building dynamic home screen payload...");

        int todaySeed = getTodaysSeed();

        // 1. Build sections dynamically using pickQuery
        List<Map<String, Object>> sections = new ArrayList<>();
        
        List<Map.Entry<String, List<String>>> sectionConfigs = List.of(
            Map.entry("Hindi Hits " + CURRENT_YEAR, HINDI_YEAR_QUERIES),
            Map.entry("Telugu Hits " + CURRENT_YEAR, TELUGU_YEAR_QUERIES),
            Map.entry("Trending Hindi", HINDI_QUERIES),
            Map.entry("Trending Telugu", TELUGU_QUERIES),
            Map.entry("Telugu Melodies", TELUGU_MELODY_QUERIES),
            Map.entry("Telugu Classics", TELUGU_CLASSIC_QUERIES),
            Map.entry("Trending Tamil", TAMIL_QUERIES),
            Map.entry("Latest Bollywood", BOLLYWOOD_QUERIES),
            Map.entry("Top Punjabi", PUNJABI_QUERIES),
            Map.entry("Romantic Vibes", ROMANTIC_QUERIES),
            Map.entry("Party Hits", PARTY_QUERIES),
            Map.entry("Old is Gold", RETRO_QUERIES),
            Map.entry("Trending Kannada", KANNADA_QUERIES),
            Map.entry("Trending Malayalam", MALAYALAM_QUERIES)
        );

        List<CompletableFuture<Void>> sectionFutures = new ArrayList<>();
        for (int i = 0; i < sectionConfigs.size(); i++) {
            final int index = i;
            Map.Entry<String, List<String>> config = sectionConfigs.get(i);
            String title = config.getKey();
            int sectionSeed = i + 1;
            String query = pickQuery(config.getValue(), sectionSeed);
            
            sectionFutures.add(CompletableFuture.runAsync(() -> {
                List<Map<String, Object>> songs = fetchSongsList(query, 20);
                
                // Filter out devotional songs and those without audio URL
                List<Map<String, Object>> filteredSongs = songs.stream()
                    .filter(s -> !isDevotionalSong(s))
                    .toList();

                Map<String, Object> section = new LinkedHashMap<>();
                section.put("title", title);
                section.put("songs", filteredSongs);
                
                synchronized (sections) {
                    sections.add(section);
                }
            }, homeExecutor));
        }

        // 2. Build filmAlbums (using FILM_DISCOVERY_QUERIES_POOL shuffled with today's seed)
        log.info("buildHomeData | Building filmAlbums dynamically...");
        List<Map<String, Object>> filmAlbums = new ArrayList<>();
        List<String> shuffledFilmQueries = seededShuffle(FILM_DISCOVERY_QUERIES_POOL, todaySeed);
        List<String> selectedFilmQueries = shuffledFilmQueries.subList(0, Math.min(8, shuffledFilmQueries.size()));

        Map<String, List<Map<String, Object>>> albumGroups = new LinkedHashMap<>();
        Map<String, String> albumCovers = new HashMap<>();
        Set<String> seenSongIds = new HashSet<>();

        List<CompletableFuture<Void>> filmFutures = new ArrayList<>();
        for (String q : selectedFilmQueries) {
            filmFutures.add(CompletableFuture.runAsync(() -> {
                List<Map<String, Object>> songs = fetchSongsList(q, 20);
                for (Map<String, Object> song : songs) {
                    if (isDevotionalSong(song)) continue;
                    String songId = (String) song.get("id");
                    if (songId == null) continue;
                    
                    String albumName = "";
                    Object albumObj = song.get("album");
                    if (albumObj instanceof Map<?, ?> albumMap) {
                        albumName = (String) albumMap.get("name");
                    }
                    if (albumName == null || albumName.trim().isEmpty()) {
                        albumName = (String) song.get("movie");
                    }
                    if (albumName == null || albumName.trim().isEmpty()) continue;
                    
                    String key = albumName.toLowerCase().trim();
                    
                    synchronized (albumGroups) {
                        if (!albumGroups.containsKey(key)) {
                            albumGroups.put(key, new ArrayList<>());
                        }
                        if (!seenSongIds.contains(songId)) {
                            seenSongIds.add(songId);
                            albumGroups.get(key).add(song);
                        }
                    }
                    
                    String coverUrl = "";
                    Object imageObj = song.get("image");
                    if (imageObj instanceof List<?> imgList && !imgList.isEmpty()) {
                        Object last = imgList.get(imgList.size() - 1);
                        if (last instanceof Map<?, ?> urlMap) {
                            coverUrl = (String) urlMap.get("url");
                            if (coverUrl == null) coverUrl = (String) urlMap.get("link");
                        }
                    }
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        synchronized (albumCovers) {
                            albumCovers.put(key, coverUrl);
                        }
                    }
                }
            }, homeExecutor));
        }

        // 3. Build artistAlbums (using ARTIST_DISCOVERY_QUERIES shuffled with today's seed)
        log.info("buildHomeData | Building artistAlbums dynamically...");
        List<Map<String, Object>> artistAlbums = new ArrayList<>();
        List<String> shuffledArtistQueries = seededShuffle(ARTIST_DISCOVERY_QUERIES, todaySeed);
        List<String> selectedArtistQueries = shuffledArtistQueries.subList(0, Math.min(8, shuffledArtistQueries.size()));

        Map<String, List<Map<String, Object>>> artistGroups = new LinkedHashMap<>();
        Map<String, String> artistCovers = new HashMap<>();
        Set<String> artistSongSeen = new HashSet<>();

        List<CompletableFuture<Void>> artistFutures = new ArrayList<>();
        for (String q : selectedArtistQueries) {
            artistFutures.add(CompletableFuture.runAsync(() -> {
                List<Map<String, Object>> songs = fetchSongsList(q, 20);
                for (Map<String, Object> song : songs) {
                    if (isDevotionalSong(song)) continue;
                    String songId = (String) song.get("id");
                    if (songId == null) continue;
                    
                    String name = getArtistName(song);
                    if (name == null || name.length() < 2) continue;
                    
                    String key = name.toLowerCase().trim();
                    synchronized (artistGroups) {
                        if (!artistGroups.containsKey(key)) {
                            artistGroups.put(key, new ArrayList<>());
                        }
                        if (!artistSongSeen.contains(songId)) {
                            artistSongSeen.add(songId);
                            artistGroups.get(key).add(song);
                        }
                    }
                    
                    String coverUrl = "";
                    Object imageObj = song.get("image");
                    if (imageObj instanceof List<?> imgList && !imgList.isEmpty()) {
                        Object last = imgList.get(imgList.size() - 1);
                        if (last instanceof Map<?, ?> urlMap) {
                            coverUrl = (String) urlMap.get("url");
                            if (coverUrl == null) coverUrl = (String) urlMap.get("link");
                        }
                    }
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        synchronized (artistCovers) {
                            artistCovers.put(key, coverUrl);
                        }
                    }
                }
            }, homeExecutor));
        }

        // Join all futures concurrently
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        allFutures.addAll(sectionFutures);
        allFutures.addAll(filmFutures);
        allFutures.addAll(artistFutures);

        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("buildHomeData | Parallel processing failed: {}", e.getMessage());
        }

        // Maintain order of sections
        sections.sort(Comparator.comparingInt(s -> {
            String t = (String) s.get("title");
            for (int i = 0; i < sectionConfigs.size(); i++) {
                if (sectionConfigs.get(i).getKey().equals(t)) return i;
            }
            return sectionConfigs.size();
        }));

        // Convert film album groups to structured list
        albumGroups.forEach((key, songsList) -> {
            if (songsList.size() >= 3) {
                Map<String, Object> album = new LinkedHashMap<>();
                String displayName = "";
                Object firstSongAlbum = songsList.get(0).get("album");
                if (firstSongAlbum instanceof Map<?, ?> albumMap) {
                    displayName = (String) albumMap.get("name");
                }
                if (displayName == null || displayName.isEmpty()) {
                    displayName = (String) songsList.get(0).get("movie");
                }
                if (displayName == null || displayName.isEmpty()) {
                    displayName = songsList.get(0).get("albumName") != null ? (String) songsList.get(0).get("albumName") : key;
                }
                
                album.put("title", displayName);
                album.put("coverArt", albumCovers.getOrDefault(key, ""));
                album.put("songs", songsList);
                album.put("type", "movie");
                album.put("query", displayName + " movie songs");
                album.put("fullyLoaded", false);
                filmAlbums.add(album);
            }
        });
        
        filmAlbums.sort((a, b) -> ((List<?>) b.get("songs")).size() - ((List<?>) a.get("songs")).size());
        List<Map<String, Object>> finalFilmAlbums = filmAlbums.stream().limit(25).toList();

        // Convert artist groups to structured list
        List<Map.Entry<String, List<Map<String, Object>>>> topArtists = artistGroups.entrySet().stream()
            .filter(e -> e.getValue().size() >= 2)
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .limit(25)
            .toList();

        topArtists.stream().forEach(entry -> {
            String name = entry.getKey();
            List<Map<String, Object>> stubSongs = entry.getValue();
            
            String displayName = getArtistName(stubSongs.get(0));
            if (displayName == null || displayName.isEmpty()) {
                displayName = name;
            }
            
            Map<String, Object> artist = new LinkedHashMap<>();
            artist.put("title", displayName);
            artist.put("coverArt", artistCovers.getOrDefault(name, ""));
            artist.put("songs", stubSongs);
            artist.put("type", "artist");
            artist.put("query", displayName + " songs");
            artist.put("fullyLoaded", true);
            
            synchronized (artistAlbums) {
                artistAlbums.add(artist);
            }
        });

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sections", sections);
        payload.put("filmAlbums", finalFilmAlbums);
        payload.put("artistAlbums", artistAlbums);
        
        log.info("buildHomeData | Building completed successfully.");
        return payload;
    }
}