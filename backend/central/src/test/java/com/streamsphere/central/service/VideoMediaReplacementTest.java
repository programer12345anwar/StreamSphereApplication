package com.streamsphere.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.streamsphere.central.entity.Channel;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.enums.VideoStatus;
import com.streamsphere.central.enums.VideoVisibility;
import com.streamsphere.central.repository.VideoRepo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoMediaReplacementTest {

    @Test
    void replacesOnlyMediaUrlAndPreservesExistingVideoMetadata() {
        UUID channelId = UUID.randomUUID();
        Channel channel = new Channel();
        channel.setId(channelId);
        Video video = new Video();
        video.setId("existing-video");
        video.setChannel(channel);
        video.setViews(42);
        video.setStatus(VideoStatus.PUBLISHED);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setName("Original title");

        VideoRepo videoRepo = org.mockito.Mockito.mock(VideoRepo.class);
        when(videoRepo.findById("existing-video")).thenReturn(Optional.of(video));
        VideoService service = new VideoService();
        ReflectionTestUtils.setField(service, "videoRepo", videoRepo);

        service.replaceVideoMedia(channelId, "existing-video", "https://ik.example/video.mp4");

        assertEquals("https://ik.example/video.mp4", video.getVideoLink());
        assertEquals(42, video.getViews());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());
        assertEquals(VideoVisibility.PUBLIC, video.getVisibility());
        assertEquals("Original title", video.getName());
        verify(videoRepo).save(video);
    }
}
