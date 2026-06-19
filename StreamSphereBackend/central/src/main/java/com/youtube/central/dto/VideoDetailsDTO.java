package com.youtube.central.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class VideoDetailsDTO {
    private String id;
    private String name;
    private String description;
    private LocalDateTime uploadDateTime;
    private LocalDateTime updatedAt;
    private String videoLink;
    private List<String> tags;
}
