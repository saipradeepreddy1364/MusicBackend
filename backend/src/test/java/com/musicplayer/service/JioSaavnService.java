package com.musicplayer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JioSaavnService {

    private final WebClient webClient;

    public JioSaavnService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://saavn.dev/api")
                .build();
    }

    // 🔍 Search songs
    public Map<String, Object> searchSongs(String query) {
        try {
            log.info("Searching songs with query: {}", query);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/songs")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Search successful");
            return response;

        } catch (WebClientResponseException e) {
            log.error("API error while searching songs: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch songs from JioSaavn API");
        } catch (Exception e) {
            log.error("Unexpected error while searching songs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // 🎵 Get song details
    public Map<String, Object> getSongDetails(String songId) {
        try {
            log.info("Fetching song details for ID: {}", songId);

            Map<String, Object> response = webClient.get()
                    .uri("/songs/" + songId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Fetched song details successfully");
            return response;

        } catch (WebClientResponseException e) {
            log.error("API error while fetching song details: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch song details");
        } catch (Exception e) {
            log.error("Unexpected error while fetching song details", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // 📀 Get album details
    public Map<String, Object> getAlbumDetails(String albumId) {
        try {
            log.info("Fetching album details for ID: {}", albumId);

            Map<String, Object> response = webClient.get()
                    .uri("/albums/" + albumId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Fetched album details successfully");
            return response;

        } catch (WebClientResponseException e) {
            log.error("API error while fetching album details: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch album details");
        } catch (Exception e) {
            log.error("Unexpected error while fetching album details", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // 🎤 Get artist details
    public Map<String, Object> getArtistDetails(String artistId) {
        try {
            log.info("Fetching artist details for ID: {}", artistId);

            Map<String, Object> response = webClient.get()
                    .uri("/artists/" + artistId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Fetched artist details successfully");
            return response;

        } catch (WebClientResponseException e) {
            log.error("API error while fetching artist details: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch artist details");
        } catch (Exception e) {
            log.error("Unexpected error while fetching artist details", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // 🔥 Trending songs
    public Map<String, Object> getTrendingSongs() {
        try {
            log.info("Fetching trending songs");

            Map<String, Object> response = webClient.get()
                    .uri("/modules?language=english")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Fetched trending songs successfully");
            return response;

        } catch (WebClientResponseException e) {
            log.error("API error while fetching trending songs: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to fetch trending songs");
        } catch (Exception e) {
            log.error("Unexpected error while fetching trending songs", e);
            throw new RuntimeException("Unexpected error occurred");
        }
    }

    // ❤️ Health check
    public Map<String, String> healthCheck() {
        log.info("Health check endpoint called");

        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "JioSaavnService");

        return response;
    }
}