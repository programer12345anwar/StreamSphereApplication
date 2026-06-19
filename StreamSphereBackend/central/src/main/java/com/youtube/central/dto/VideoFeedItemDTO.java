package com.youtube.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFeedItemDTO {
    private String id;
    private String title;
    private String description;
    private String videoLink;
    private String thumbnailLink;
    private int views;
    private LocalDateTime uploadedAt;
    private UUID channelId;
    private String channelName;
}
