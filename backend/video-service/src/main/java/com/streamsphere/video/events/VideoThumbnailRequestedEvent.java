package com.streamsphere.video.events;

import java.util.UUID;

public record VideoThumbnailRequestedEvent(
        UUID eventId,
        String videoId,
        String videoUrl,
        String customThumbnailUrl
) {}
