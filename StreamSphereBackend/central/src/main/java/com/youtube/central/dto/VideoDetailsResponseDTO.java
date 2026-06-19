package com.youtube.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailsResponseDTO {
    private String id;
    private String title;
    private String description;
    private String videoLink;
    private String thumbnailLink;
    private int views;
    private LocalDateTime uploadedAt;
    private UUID channelId;
    private String channelName;
    private List<String> tags;
}
