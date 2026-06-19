package com.youtube.central.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toArray(String[]::new);

                var registration = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");

                if (origins.length == 0 || (origins.length == 1 && "*".equals(origins[0]))) {
                    registration.allowedOriginPatterns("*");
                    // when allowing any origin, do not allow credentials
                    registration.allowCredentials(false);
                } else {
                    registration.allowedOrigins(origins);
                    // Allow credentials (cookies/authorization headers) from the gateway origin
                    registration.allowCredentials(true);
                }
            }
        };
    }
}

