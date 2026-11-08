package com.streamsphere.central.event.consumer;

import com.streamsphere.video.events.VideoThumbnailResultEvent;
import com.streamsphere.central.service.VideoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VideoThumbnailResultListener {
    private final VideoService videoService;

    public VideoThumbnailResultListener(VideoService videoService) {
        this.videoService = videoService;
    }

    @RabbitListener(queues = "${thumbnail.events.result-queue}")
    public void onThumbnailResult(VideoThumbnailResultEvent event) {
        videoService.completeThumbnailProcessing(event);
    }
}
