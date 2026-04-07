package com.musicplayer.config;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration.
 *
 * We use WebMvcConfigurer instead of a CorsFilter @Bean because:
 *  - There is no Spring Security in this project.
 *  - A raw CorsFilter bean requires Spring Security to call
 *    http.cors() to be picked up for non-Security filter chains.
 *  - WebMvcConfigurer.addCorsMappings() is always applied by
 *    Spring MVC regardless of Security, so it is the correct
 *    approach for a plain Spring Boot REST app.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")          // every endpoint under /api/**
                .allowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "https://rhythm-weaver-two.vercel.app",
                        "https://*.vercel.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}