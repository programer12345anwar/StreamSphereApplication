package com.streamsphere.central.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminChannelDTO(
        UUID id,
        String name,
        String description,
        int totalSubs,
        int totalViews,
        boolean monetized,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
