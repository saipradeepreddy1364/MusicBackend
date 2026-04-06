package com.musicplayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${saavn.base-url:https://saavn.dev/api}")
    private String saavnBaseUrl;

    @Bean("jiosaavnClient")
    public WebClient jiosaavnWebClient(@NonNull WebClient.Builder builder) {

        // ✅ Ensure non-null base URL
        final String baseUrl = (saavnBaseUrl != null) ? saavnBaseUrl : "https://saavn.dev/api";

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (compatible; MusicPlayerApp/1.0)")
                .exchangeStrategies(strategies)
                .build();
    }
}