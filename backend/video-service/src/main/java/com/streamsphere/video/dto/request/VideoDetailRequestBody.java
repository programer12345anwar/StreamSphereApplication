package com.streamsphere.video.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class VideoDetailRequestBody {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 200)
    String name;

    @jakarta.validation.constraints.Size(max = 2000)
    String description;

    java.util.List<String> tags;
}


