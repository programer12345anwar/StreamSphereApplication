package com.streamsphere.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ImageKitUploadCompletionRequest {
    @NotNull
    private UUID channelId;

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9a-fA-F-]{36}$")
    private String videoId;

    @NotBlank
    @Size(max = 200)
    private String fileId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    private List<String> tags;
    @com.fasterxml.jackson.annotation.JsonProperty("isShort")
    private boolean isShort;
}
