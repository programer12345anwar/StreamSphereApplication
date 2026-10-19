package com.streamsphere.gateway.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RuntimeConfigController {

    private final String publicApiBaseUrl;

    public RuntimeConfigController(@Value("${app.public-api-base-url:}") String publicApiBaseUrl) {
        this.publicApiBaseUrl = publicApiBaseUrl;
    }

    @GetMapping("/api/config")
    public ResponseEntity<Map<String, String>> getRuntimeConfig() {
        if (publicApiBaseUrl == null || publicApiBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Public API base URL is not configured");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("apiBaseUrl", publicApiBaseUrl.replaceAll("/+$", "")));
    }
}
