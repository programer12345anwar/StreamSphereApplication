package com.streamsphere.video.events;

import java.util.UUID;

public record VideoThumbnailResultEvent(
        UUID eventId,
        String videoId,
        String thumbnailUrl,
        String thumbnailSource,
        String processingStatus,
        Integer durationSeconds
) {}
