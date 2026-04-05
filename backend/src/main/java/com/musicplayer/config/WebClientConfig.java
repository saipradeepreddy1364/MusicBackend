package com.musicplayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * saavn.dev — public JioSaavn unofficial API.
     * Spring Boot calls it directly; no Node.js proxy needed.
     * Override via SAAVN_BASE_URL env var if you self-host later.
     */
    @Value("${saavn.base-url:https://saavn.dev/api}")
    private String saavnBaseUrl;

    @Bean("jiosaavnClient")
    public WebClient jiosaavnWebClient(WebClient.Builder builder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return builder
                .baseUrl(saavnBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // saavn.dev requires a browser-like User-Agent
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (compatible; MusicPlayerApp/1.0)")
                .exchangeStrategies(strategies)
                .build();
    }
}