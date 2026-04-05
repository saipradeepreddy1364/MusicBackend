package com.musicplayer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Register all named caches used by @Cacheable
        manager.setCacheNames(Arrays.asList(
                "search", "searchSongs", "searchAlbums", "searchArtists", "searchPlaylists",
                "song", "songSuggestions",
                "album",
                "artist", "artistSongs", "artistAlbums",
                "playlist",
                "charts"
        ));

        // Default spec: max 2000 entries, expire 5 min after write
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
        );

        return manager;
    }
}
