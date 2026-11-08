package com.streamsphere.video.service;

import com.streamsphere.video.client.central.CentralApiConnectionService;
import com.streamsphere.video.dto.request.ImageKitUploadCompletionRequest;
import com.streamsphere.video.dto.request.VideoDetailsDTO;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.results.Result;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        ReflectionTestUtils.setField(uploadService, "maxUploadBytes", 100L * 1024 * 1024);
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

    @Test
    void rejectsFilesAboveConfiguredLimitBeforeCheckingChannelPermission() {
        UploadService uploadService = new UploadService();
        CentralApiConnectionService centralApi = mock(CentralApiConnectionService.class);
        ReflectionTestUtils.setField(uploadService, "centralApiConnectionService", centralApi);
        ReflectionTestUtils.setField(uploadService, "maxUploadBytes", 100L * 1024 * 1024);

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> uploadService.createImageKitUploadAuthorization(
                        UUID.randomUUID(), "large.mp4", 100L * 1024 * 1024 + 1, null, "Bearer token"));

        assertTrue(exception.getMessage().contains("104857600"));
        verifyNoInteractions(centralApi);
    }

    @Test
    void createsScopedThumbnailAuthorizationForOwner() {
        UploadService uploadService = new UploadService();
        CentralApiConnectionService centralApi = mock(CentralApiConnectionService.class);
        ImageKit imageKit = mock(ImageKit.class);
        UUID channelId = UUID.randomUUID();
        String videoId = UUID.randomUUID().toString();
        Map<String, String> authentication = Map.of("token", "signed");
        ReflectionTestUtils.setField(uploadService, "centralApiConnectionService", centralApi);
        ReflectionTestUtils.setField(uploadService, "imageKit", imageKit);
        ReflectionTestUtils.setField(uploadService, "maxThumbnailBytes", 5L * 1024 * 1024);
        when(centralApi.canUploadToChannel(channelId, videoId, "Bearer user"))
                .thenReturn(true);
        when(imageKit.getConfig()).thenReturn(
                new Configuration("public-key", "private-key", "https://ik.example"));
        when(imageKit.getAuthenticationParameters(isNull(), anyLong())).thenReturn(authentication);

        var result = uploadService.createThumbnailUploadAuthorization(
                channelId, videoId, "cover.webp", 2048, true, "Bearer user");

        assertEquals("public-key", result.getPublicKey());
        assertEquals(videoId, result.getVideoId());
        assertTrue(result.getFileName().startsWith(videoId + "_custom_"));
        assertEquals(".webp", result.getFileName().substring(result.getFileName().lastIndexOf('.')));
        assertEquals("/streamsphere/thumbnails/" + channelId, result.getFolder());
        assertEquals(authentication, result.getAuthentication());
    }

    @Test
    void rejectsUnsupportedThumbnailExtensionsBeforePermissionCheck() {
        UploadService uploadService = new UploadService();
        CentralApiConnectionService centralApi = mock(CentralApiConnectionService.class);
        ReflectionTestUtils.setField(uploadService, "centralApiConnectionService", centralApi);
        ReflectionTestUtils.setField(uploadService, "maxThumbnailBytes", 5L * 1024 * 1024);

        assertThrows(IllegalArgumentException.class, () -> uploadService.createThumbnailUploadAuthorization(
                UUID.randomUUID(), UUID.randomUUID().toString(), "cover.svg", 2048, false, "Bearer user"));

        verifyNoInteractions(centralApi);
    }

    @Test
    void validatesCustomThumbnailContentsBeforePersistingMetadata() throws Exception {
        UploadService uploadService = new UploadService();
        CentralApiConnectionService centralApi = mock(CentralApiConnectionService.class);
        ImageKit imageKit = mock(ImageKit.class);
        FfmpegMediaProcessor mediaProcessor = mock(FfmpegMediaProcessor.class);
        ImageKitAssetDownloader downloader = mock(ImageKitAssetDownloader.class);
        UUID channelId = UUID.randomUUID();
        String videoId = UUID.randomUUID().toString();
        ReflectionTestUtils.setField(uploadService, "centralApiConnectionService", centralApi);
        ReflectionTestUtils.setField(uploadService, "imageKit", imageKit);
        ReflectionTestUtils.setField(uploadService, "mediaProcessor", mediaProcessor);
        ReflectionTestUtils.setField(uploadService, "assetDownloader", downloader);
        ReflectionTestUtils.setField(uploadService, "maxUploadBytes", 100L * 1024 * 1024);
        ReflectionTestUtils.setField(uploadService, "maxThumbnailBytes", 5L * 1024 * 1024);
        ReflectionTestUtils.setField(uploadService, "minimumThumbnailWidth", 160);
        ReflectionTestUtils.setField(uploadService, "minimumThumbnailHeight", 90);
        ReflectionTestUtils.setField(uploadService, "maximumThumbnailWidth", 4096);
        ReflectionTestUtils.setField(uploadService, "maximumThumbnailHeight", 4096);
        ReflectionTestUtils.setField(uploadService, "maximumThumbnailPixels", 16_777_216L);
        ReflectionTestUtils.setField(uploadService, "thumbnailDownloadTimeoutMillis", 10000);
        when(centralApi.canUploadToChannel(channelId, null, "Bearer user")).thenReturn(true);
        when(imageKit.getConfig()).thenReturn(
                new Configuration("public-key", "private-key", "https://ik.example"));

        Result videoAsset = new Result();
        videoAsset.setUrl("https://ik.example/video.mp4");
        videoAsset.setFilePath("/streamsphere/videos/" + channelId + "/" + videoId + ".mp4");
        videoAsset.setName(videoId + ".mp4");
        videoAsset.setSize(1024);
        videoAsset.setMime("video/mp4");
        Result thumbnailAsset = new Result();
        thumbnailAsset.setUrl("https://ik.example/cover.png");
        thumbnailAsset.setFilePath("/streamsphere/thumbnails/" + channelId + "/" + videoId + "_custom_1.png");
        thumbnailAsset.setName(videoId + "_custom_1.png");
        thumbnailAsset.setSize(2048);
        thumbnailAsset.setMime("image/png");
        when(imageKit.getFileDetail("video-file")).thenReturn(videoAsset);
        when(imageKit.getFileDetail("thumbnail-file")).thenReturn(thumbnailAsset);
        when(mediaProcessor.inspectImage(any(Path.class)))
                .thenReturn(new FfmpegMediaProcessor.ImageMetadata("png", 1280, 720));
        doNothing().when(downloader).downloadTo(any(URI.class), any(Path.class), anyLong(), anyInt());

        ImageKitUploadCompletionRequest request = new ImageKitUploadCompletionRequest();
        request.setChannelId(channelId);
        request.setVideoId(videoId);
        request.setFileId("video-file");
        request.setThumbnailFileId("thumbnail-file");
        request.setName("A video");

        uploadService.completeImageKitUpload(request, "Bearer user");

        ArgumentCaptor<VideoDetailsDTO> detailsCaptor = ArgumentCaptor.forClass(VideoDetailsDTO.class);
        verify(centralApi).saveVideoDetails(eq(channelId), detailsCaptor.capture(), eq("Bearer user"));
        assertEquals("https://ik.example/cover.png", detailsCaptor.getValue().getThumbnailLink());
        assertEquals("CUSTOM", detailsCaptor.getValue().getThumbnailSource());
        verify(downloader).downloadTo(any(URI.class), any(Path.class), eq(5L * 1024 * 1024), eq(10000));
    }
}
