package com.streamsphere.central.service;

import com.streamsphere.central.entity.Video;
import com.streamsphere.central.enums.ThumbnailProcessingStatus;
import com.streamsphere.central.enums.ThumbnailSource;
import com.streamsphere.central.enums.VideoStatus;
import com.streamsphere.central.repository.VideoRepo;
import com.streamsphere.central.repository.VideoProcessingOutboxRepo;
import com.streamsphere.video.events.VideoThumbnailResultEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoThumbnailProcessingTest {
    @Test
    void appliesGeneratedThumbnailAndPublishesOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        Video video = pendingVideo(eventId);
        VideoRepo videoRepo = mock(VideoRepo.class);
        when(videoRepo.findById(video.getId())).thenReturn(Optional.of(video));
        VideoService service = service(videoRepo);

        service.completeThumbnailProcessing(new VideoThumbnailResultEvent(
                eventId, video.getId(), "https://ik.example/video.jpg", "GENERATED", "READY", 45));

        assertEquals("https://ik.example/video.jpg", video.getThumbnailLink());
        assertEquals(ThumbnailSource.GENERATED, video.getThumbnailSource());
        assertEquals(ThumbnailProcessingStatus.READY, video.getThumbnailProcessingStatus());
        assertEquals(45, video.getDurationSeconds());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());

        service.completeThumbnailProcessing(new VideoThumbnailResultEvent(
                eventId, video.getId(), "https://ik.example/duplicate.jpg", "GENERATED", "READY", 90));

        assertEquals("https://ik.example/video.jpg", video.getThumbnailLink());
        verify(videoRepo).save(video);
    }

    @Test
    void publishesVideoWithExplicitFallbackStateWhenGenerationFails() {
        UUID eventId = UUID.randomUUID();
        Video video = pendingVideo(eventId);
        VideoRepo videoRepo = mock(VideoRepo.class);
        when(videoRepo.findById(video.getId())).thenReturn(Optional.of(video));
        VideoService service = service(videoRepo);

        service.completeThumbnailProcessing(new VideoThumbnailResultEvent(
                eventId, video.getId(), null, "DEFAULT", "FALLBACK", null));

        assertNull(video.getThumbnailLink());
        assertEquals(ThumbnailSource.DEFAULT, video.getThumbnailSource());
        assertEquals(ThumbnailProcessingStatus.FALLBACK, video.getThumbnailProcessingStatus());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());
    }

    @Test
    void ignoresStaleProcessingResultAfterAReplacementUpload() {
        UUID currentEventId = UUID.randomUUID();
        Video video = pendingVideo(currentEventId);
        VideoRepo videoRepo = mock(VideoRepo.class);
        when(videoRepo.findById(video.getId())).thenReturn(Optional.of(video));
        VideoService service = service(videoRepo);

        service.completeThumbnailProcessing(new VideoThumbnailResultEvent(
                UUID.randomUUID(), video.getId(), "https://ik.example/stale.jpg", "GENERATED", "READY", 20));

        assertNull(video.getThumbnailLink());
        assertEquals(VideoStatus.PROCESSING, video.getStatus());
    }

    @Test
    void preservesAnExistingNonProcessingStatusForMediaReplacement() {
        UUID eventId = UUID.randomUUID();
        Video video = pendingVideo(eventId);
        video.setStatus(VideoStatus.DRAFT);
        VideoRepo videoRepo = mock(VideoRepo.class);
        when(videoRepo.findById(video.getId())).thenReturn(Optional.of(video));
        VideoService service = service(videoRepo);

        service.completeThumbnailProcessing(new VideoThumbnailResultEvent(
                eventId, video.getId(), "https://ik.example/video.jpg", "GENERATED", "READY", 45));

        assertEquals(VideoStatus.DRAFT, video.getStatus());
        assertEquals(ThumbnailProcessingStatus.READY, video.getThumbnailProcessingStatus());
    }

    private static Video pendingVideo(UUID eventId) {
        Video video = new Video();
        video.setId("thumbnail-video");
        video.setStatus(VideoStatus.PROCESSING);
        video.setThumbnailProcessingStatus(ThumbnailProcessingStatus.PENDING);
        video.setThumbnailProcessingEventId(eventId);
        return video;
    }

    private static VideoService service(VideoRepo videoRepo) {
        VideoService service = new VideoService();
        ReflectionTestUtils.setField(service, "videoRepo", videoRepo);
        ReflectionTestUtils.setField(service, "videoProcessingOutboxRepo", mock(VideoProcessingOutboxRepo.class));
        ReflectionTestUtils.setField(service, "rabbitMqService",
                mock(com.streamsphere.central.event.producer.RabbitMqService.class));
        return service;
    }
}
