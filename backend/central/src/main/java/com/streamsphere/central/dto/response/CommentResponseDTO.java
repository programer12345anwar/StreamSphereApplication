package com.streamsphere.central.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponseDTO(
        UUID id,
        String text,
        String authorName,
        LocalDateTime createdAt,
        UUID parentId,
        long likeCount,
        boolean likedByCurrentUser) {}
