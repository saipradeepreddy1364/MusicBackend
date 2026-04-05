package com.musicplayer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicPlayerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Player API")
                        .description("REST API for Music Player APK powered by JioSaavn Unofficial API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Music Player Team")
                                .email("dev@musicplayer.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local"),
                        new Server().url("https://api.yourapp.com/api").description("Production")
                ));
    }
}
