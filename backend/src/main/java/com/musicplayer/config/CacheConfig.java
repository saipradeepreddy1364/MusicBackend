package com.musicplayer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @SuppressWarnings("null")
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        List<String> cacheNames = Arrays.asList(
                "search",
                "searchSongs",
                "searchAlbums",
                "searchArtists",
                "searchPlaylists",
                "song",
                "songSuggestions",
                "album",
                "artist",
                "artistSongs",
                "artistAlbums",
                "playlist",
                "charts"
        );
        manager.setCacheNames(cacheNames);

        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());

        return manager;
    }
}