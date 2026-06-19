package com.youtube.video_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class VideoDetailsDTO {
    String id;
    String name;
    String description;
    LocalDateTime uploadDateTime;
    LocalDateTime updatedAt;
    String videoLink;
    List<String> tags;
}
