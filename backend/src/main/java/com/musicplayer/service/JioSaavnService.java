package com.musicplayer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class JioSaavnService {

    private static final Logger log = LoggerFactory.getLogger(JioSaavnService.class);

    private final WebClient webClient;

    public JioSaavnService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://jiosaavn-api.pradeepreddypalagiri.workers.dev/api")
                .build();
    }

    private Map<String, Object> getUri(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    public Map<String, Object> search(String query, int page, int limit) {
        try {
            log.info("Global search: query={} page={} limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on search: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search");
        } catch (Exception e) {
            log.error("Unexpected error on search", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> searchSongs(String query, int page, int limit) {
        try {
            log.info("Search songs: query={} page={} limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/songs")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchSongs: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search songs");
        } catch (Exception e) {
            log.error("Unexpected error on searchSongs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> searchAlbums(String query, int page, int limit) {
        try {
            log.info("Search albums: query={} page={} limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/albums")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchAlbums: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search albums");
        } catch (Exception e) {
            log.error("Unexpected error on searchAlbums", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> searchArtists(String query, int page, int limit) {
        try {
            log.info("Search artists: query={} page={} limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/artists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchArtists: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search artists");
        } catch (Exception e) {
            log.error("Unexpected error on searchArtists", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> searchPlaylists(String query, int page, int limit) {
        try {
            log.info("Search playlists: query={} page={} limit={}", query, page, limit);
            return webClient.get()
                    .uri(u -> u.path("/search/playlists")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on searchPlaylists: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search playlists");
        } catch (Exception e) {
            log.error("Unexpected error on searchPlaylists", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getSongById(String id) {
        try {
            log.info("Get song by id: {}", id);
            return getUri("/songs/" + id);
        } catch (WebClientResponseException e) {
            log.error("API error on getSongById: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch song");
        } catch (Exception e) {
            log.error("Unexpected error on getSongById", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getSongSuggestions(String id, int limit) {
        try {
            log.info("Get suggestions for song id: {} limit: {}", id, limit);
            return webClient.get()
                    .uri(u -> u.path("/songs/" + id + "/suggestions")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getSongSuggestions: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch song suggestions");
        } catch (Exception e) {
            log.error("Unexpected error on getSongSuggestions", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getAlbum(String id, String link) {
        try {
            log.info("Get album: id={} link={}", id, link);
            return webClient.get()
                    .uri(u -> {
                        var b = u.path("/albums");
                        if (id != null)   b = b.queryParam("id", id);
                        if (link != null) b = b.queryParam("link", link);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getAlbum: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch album");
        } catch (Exception e) {
            log.error("Unexpected error on getAlbum", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getArtist(String id) {
        try {
            log.info("Get artist: id={}", id);
            return getUri("/artists/" + id);
        } catch (WebClientResponseException e) {
            log.error("API error on getArtist: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch artist");
        } catch (Exception e) {
            log.error("Unexpected error on getArtist", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getArtistSongs(String id, int page, String sortBy, String sortOrder) {
        try {
            log.info("Get artist songs: id={} page={} sortBy={} sortOrder={}", id, page, sortBy, sortOrder);
            return webClient.get()
                    .uri(u -> u.path("/artists/" + id + "/songs")
                            .queryParam("page", page)
                            .queryParam("sortBy", sortBy)
                            .queryParam("sortOrder", sortOrder)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getArtistSongs: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch artist songs");
        } catch (Exception e) {
            log.error("Unexpected error on getArtistSongs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getArtistAlbums(String id, int page) {
        try {
            log.info("Get artist albums: id={} page={}", id, page);
            return webClient.get()
                    .uri(u -> u.path("/artists/" + id + "/albums")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getArtistAlbums: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch artist albums");
        } catch (Exception e) {
            log.error("Unexpected error on getArtistAlbums", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getPlaylist(String id, String link, int page, int limit) {
        try {
            log.info("Get playlist: id={} link={} page={} limit={}", id, link, page, limit);
            return webClient.get()
                    .uri(u -> {
                        var b = u.path("/playlists");
                        if (id != null)   b = b.queryParam("id", id);
                        if (link != null) b = b.queryParam("link", link);
                        return b.queryParam("page", page)
                                .queryParam("limit", limit)
                                .build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getPlaylist: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch playlist");
        } catch (Exception e) {
            log.error("Unexpected error on getPlaylist", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    public Map<String, Object> getCharts() {
        try {
            log.info("Fetching charts");
            return webClient.get()
                    .uri(u -> u.path("/search/songs")
                            .queryParam("query", "top hindi hits 2025")
                            .queryParam("page", 1)
                            .queryParam("limit", 20)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("API error on getCharts: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch charts");
        } catch (Exception e) {
            log.error("Unexpected error on getCharts", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }
}