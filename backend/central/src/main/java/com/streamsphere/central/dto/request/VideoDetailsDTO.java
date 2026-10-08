package com.streamsphere.central.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class VideoDetailsDTO {
    private String id;
    @jakarta.validation.constraints.NotBlank
    private String name;
    @jakarta.validation.constraints.Size(max = 2000)
    private String description;
    private LocalDateTime uploadDateTime;
    private LocalDateTime updatedAt;
    @jakarta.validation.constraints.NotBlank
    private String videoLink;
    private List<String> tags;
}


