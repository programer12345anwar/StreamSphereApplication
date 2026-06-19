package com.youtube.notification_api.configuration;

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
                    registration.allowCredentials(false);
                } else {
                    registration.allowedOrigins(origins);
                    registration.allowCredentials(true);
                }
            }
        };
    }
}
