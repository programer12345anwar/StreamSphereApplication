package com.streamsphere.central.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminVideoDTO(
        String id,
        String title,
        String description,
        String videoLink,
        String thumbnailLink,
        int views,
        LocalDateTime uploadedAt,
        UUID channelId,
        String channelName
) {}
