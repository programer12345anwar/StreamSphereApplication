package com.streamsphere.video.service;

import com.streamsphere.video.events.VideoThumbnailRequestedEvent;
import com.streamsphere.video.events.VideoThumbnailResultEvent;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThumbnailProcessingServiceTest {
    private FfmpegMediaProcessor mediaProcessor;
    private ImageKitAssetDownloader assetDownloader;
    private ImageKit imageKit;
    private RabbitTemplate rabbitTemplate;
    private ThumbnailProcessingService service;

    @BeforeEach
    void setUp() {
        mediaProcessor = mock(FfmpegMediaProcessor.class);
        assetDownloader = mock(ImageKitAssetDownloader.class);
        imageKit = mock(ImageKit.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        when(imageKit.getConfig()).thenReturn(
                new Configuration("public", "private", "https://ik.example"));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(Object.class), any(CorrelationData.class));
        service = new ThumbnailProcessingService(
                mediaProcessor,
                assetDownloader,
                imageKit,
                rabbitTemplate,
                "thumbnail-exchange",
                "thumbnail.result",
                1,
                100 * 1024 * 1024L,
                10000);
    }

    @Test
    void preservesCustomThumbnailAndPublishesDuration() {
        UUID eventId = UUID.randomUUID();
        VideoThumbnailRequestedEvent request = new VideoThumbnailRequestedEvent(
                eventId,
                "video-1",
                "https://ik.example/video.mp4",
                "https://ik.example/custom.png");
        when(mediaProcessor.probeVideo(any(Path.class)))
                .thenReturn(new FfmpegMediaProcessor.VideoMetadata(72));
        doNothing().when(assetDownloader).downloadTo(any(URI.class), any(Path.class), anyLong(), anyInt());

        service.processVideoThumbnail(request);

        ArgumentCaptor<VideoThumbnailResultEvent> result =
                ArgumentCaptor.forClass(VideoThumbnailResultEvent.class);
        verify(rabbitTemplate).convertAndSend(
                anyString(), anyString(), result.capture(), any(CorrelationData.class));
        assertEquals("CUSTOM", result.getValue().thumbnailSource());
        assertEquals("READY", result.getValue().processingStatus());
        assertEquals("https://ik.example/custom.png", result.getValue().thumbnailUrl());
        assertEquals(72, result.getValue().durationSeconds());
        verify(mediaProcessor, never()).generateThumbnail(
                any(Path.class), anyInt(), any(Path.class));
    }

    @Test
    void generatesAndUploadsThumbnailWhenNoCustomImageWasSelected() throws Exception {
        UUID eventId = UUID.randomUUID();
        VideoThumbnailRequestedEvent request = new VideoThumbnailRequestedEvent(
                eventId, "video-2", "https://ik.example/video.mp4", null);
        when(mediaProcessor.probeVideo(any(Path.class)))
                .thenReturn(new FfmpegMediaProcessor.VideoMetadata(42));
        when(mediaProcessor.generateThumbnail(any(Path.class), anyInt(), any(Path.class)))
                .thenReturn(new FfmpegMediaProcessor.GeneratedThumbnail(new byte[] {1, 2, 3}));
        Result uploaded = new Result();
        uploaded.setUrl("https://ik.example/generated.jpg");
        when(imageKit.upload(any(FileCreateRequest.class))).thenReturn(uploaded);
        doNothing().when(assetDownloader).downloadTo(any(URI.class), any(Path.class), anyLong(), anyInt());

        service.processVideoThumbnail(request);

        ArgumentCaptor<FileCreateRequest> upload = ArgumentCaptor.forClass(FileCreateRequest.class);
        verify(imageKit).upload(upload.capture());
        assertEquals("video-2.jpg", upload.getValue().getFileName());
        assertEquals("/streamsphere/thumbnails/generated", upload.getValue().getFolder());
        ArgumentCaptor<VideoThumbnailResultEvent> result =
                ArgumentCaptor.forClass(VideoThumbnailResultEvent.class);
        verify(rabbitTemplate).convertAndSend(
                anyString(), anyString(), result.capture(), any(CorrelationData.class));
        assertEquals("GENERATED", result.getValue().thumbnailSource());
        assertEquals("https://ik.example/generated.jpg", result.getValue().thumbnailUrl());
        assertEquals(42, result.getValue().durationSeconds());
    }

    @Test
    void publishesExplicitFallbackAfterAutomaticGenerationFails() throws Exception {
        UUID eventId = UUID.randomUUID();
        VideoThumbnailRequestedEvent request = new VideoThumbnailRequestedEvent(
                eventId, "video-3", "https://ik.example/video.mp4", null);
        when(mediaProcessor.probeVideo(any(Path.class)))
                .thenReturn(new FfmpegMediaProcessor.VideoMetadata(33));
        when(mediaProcessor.generateThumbnail(any(Path.class), anyInt(), any(Path.class)))
                .thenThrow(new ThumbnailProcessingException("No suitable frame."));
        doNothing().when(assetDownloader).downloadTo(any(URI.class), any(Path.class), anyLong(), anyInt());

        service.processVideoThumbnail(request);

        ArgumentCaptor<VideoThumbnailResultEvent> result =
                ArgumentCaptor.forClass(VideoThumbnailResultEvent.class);
        verify(rabbitTemplate).convertAndSend(
                anyString(), anyString(), result.capture(), any(CorrelationData.class));
        assertEquals("DEFAULT", result.getValue().thumbnailSource());
        assertEquals("FALLBACK", result.getValue().processingStatus());
        assertNull(result.getValue().thumbnailUrl());
        verify(imageKit, never()).upload(any(FileCreateRequest.class));
    }
}
