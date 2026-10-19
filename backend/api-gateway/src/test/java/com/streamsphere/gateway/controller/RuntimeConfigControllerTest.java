package com.streamsphere.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeConfigControllerTest {

    @Test
    void returnsConfiguredPublicApiBaseUrlWithoutTrailingSlash() {
        RuntimeConfigController controller = new RuntimeConfigController("https://api.example.com///");

        WebTestClient.bindToController(controller)
                .build()
                .get()
                .uri("/api/config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiBaseUrl").isEqualTo("https://api.example.com");
    }

    @Test
    void failsExplicitlyWhenPublicApiBaseUrlIsNotConfigured() {
        RuntimeConfigController controller = new RuntimeConfigController("");

        assertThrows(ResponseStatusException.class, controller::getRuntimeConfig);
    }
}
