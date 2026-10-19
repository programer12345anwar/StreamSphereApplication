package com.streamsphere.video.client.central;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ApiTemplateImplTest {

    private RestTemplate restTemplate;
    private ApiTemplateImpl apiTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        apiTemplate = new ApiTemplateImpl();
        ReflectionTestUtils.setField(apiTemplate, "restTemplate", restTemplate);
    }

    @Test
    void forwardsAuthorizationWhenSavingVideoDetails() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        apiTemplate.makePostCall(
                "https://central.example.com/api/v1/central",
                "/channel/123/video/upload",
                Map.of(),
                Map.of("name", "video"),
                "Bearer user-token");

        ArgumentCaptor<RequestEntity> request = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(request.capture(), eq(Object.class));
        assertEquals("Bearer user-token", request.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void forwardsAuthorizationWhenUpdatingVideoStatus() {
        when(restTemplate.exchange(any(java.net.URI.class), eq(HttpMethod.PUT), any(RequestEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        apiTemplate.makePutCall(
                "https://central.example.com/api/v1/central",
                "/channel/video/123/status",
                Map.of("status", "PUBLISHED"),
                null,
                "Bearer user-token");

        ArgumentCaptor<RequestEntity> request = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(
                any(java.net.URI.class), eq(HttpMethod.PUT), request.capture(), eq(Object.class));
        assertEquals("Bearer user-token", request.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
}
