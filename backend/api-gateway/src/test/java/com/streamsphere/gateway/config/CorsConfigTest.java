package com.streamsphere.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void allowsLocalVitePreflightWhenConfiguredOriginsAreOverridden() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://admin.example.com");
        CorsWebFilter filter = config.corsWebFilter();

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/central/user/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5176")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE)
        );
        assertEquals("http://localhost:5176", exchange.getRequest().getHeaders().getOrigin());
        var source = (CorsConfigurationSource) ReflectionTestUtils.getField(filter, "configSource");
        var actualConfiguration = source.getCorsConfiguration(exchange);
        assertNotNull(actualConfiguration);
        assertEquals("http://localhost:5176", actualConfiguration.checkOrigin("http://localhost:5176"));
        assertEquals(List.of(HttpMethod.POST), actualConfiguration.checkHttpMethod(HttpMethod.POST));
        assertEquals(List.of(HttpHeaders.CONTENT_TYPE),
                actualConfiguration.checkHeaders(List.of(HttpHeaders.CONTENT_TYPE)));
        assertTrue(actualConfiguration.getAllowCredentials());
    }
}
