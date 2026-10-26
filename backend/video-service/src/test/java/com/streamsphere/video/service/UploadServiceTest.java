package com.streamsphere.video.service;

import com.streamsphere.video.client.central.CentralApiConnectionService;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class UploadServiceTest {

    @Test
    void createsImageKitAuthorizationWithUnixExpiryUnderOneHour() {
        UploadService uploadService = new UploadService();
        CentralApiConnectionService centralApi = mock(CentralApiConnectionService.class);
        ImageKit imageKit = mock(ImageKit.class);
        Configuration configuration = new Configuration(
                "public-key", "private-key", "https://ik.imagekit.io/account");
        Map<String, String> authentication = Map.of(
                "token", "upload-token",
                "expire", "123",
                "signature", "signed");
        UUID channelId = UUID.randomUUID();

        ReflectionTestUtils.setField(uploadService, "centralApiConnectionService", centralApi);
        ReflectionTestUtils.setField(uploadService, "imageKit", imageKit);
        ReflectionTestUtils.setField(uploadService, "maxUploadBytes", 500L * 1024 * 1024);
        when(centralApi.canUploadToChannel(channelId, null, "Bearer token")).thenReturn(true);
        when(imageKit.getConfig()).thenReturn(configuration);
        when(imageKit.getAuthenticationParameters(isNull(), anyLong())).thenReturn(authentication);

        var result = uploadService.createImageKitUploadAuthorization(
                channelId, "clip.mp4", 1024, null, "Bearer token");

        ArgumentCaptor<Long> expiresAtCaptor = ArgumentCaptor.forClass(Long.class);
        verify(imageKit).getAuthenticationParameters(isNull(), expiresAtCaptor.capture());
        long now = Instant.now().getEpochSecond();
        long expiresAt = expiresAtCaptor.getValue();
        assertTrue(expiresAt > now, "expiry must be in the future");
        assertTrue(expiresAt < now + 3600, "expiry must be less than one hour away");
        assertEquals(authentication, result.getAuthentication());
    }
}
