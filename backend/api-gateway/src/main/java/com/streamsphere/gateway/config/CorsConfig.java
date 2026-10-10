package com.streamsphere.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        String[] origins = Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        if (origins.length > 0) {
            corsConfig.setAllowedOrigins(Arrays.asList(origins));
            // Vite may select a different localhost port when its default port is occupied.
            corsConfig.addAllowedOriginPattern("http://localhost:*");
            corsConfig.addAllowedOriginPattern("http://127.0.0.1:*");
            corsConfig.addAllowedOriginPattern("http://[::1]:*");
            corsConfig.setAllowCredentials(true);
        } else {
            corsConfig.addAllowedOriginPattern("*");
            corsConfig.setAllowCredentials(false);
        }

        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
