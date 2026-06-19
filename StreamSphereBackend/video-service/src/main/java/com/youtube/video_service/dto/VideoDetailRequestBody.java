package com.youtube.video_service.dto;

import java.util.List;

import lombok.Data;

@Data
public class VideoDetailRequestBody {
    String name;
    String description;
    List<String> tags;
}
