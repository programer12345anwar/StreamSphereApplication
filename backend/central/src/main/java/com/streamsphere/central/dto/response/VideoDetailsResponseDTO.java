package com.streamsphere.central.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.streamsphere.central.enums.ThumbnailProcessingStatus;
import com.streamsphere.central.enums.ThumbnailSource;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailsResponseDTO {
    private String id;
    private String title;
    private String description;
    private String videoLink;
    private String thumbnailLink;
    private ThumbnailSource thumbnailSource;
    private ThumbnailProcessingStatus thumbnailProcessingStatus;
    private Integer durationSeconds;
    private int views;
    private LocalDateTime uploadedAt;
    private UUID channelId;
    private String channelName;
    private List<String> tags;
    private boolean isShort;

    @com.fasterxml.jackson.annotation.JsonProperty("thumbnailUrl")
    public String getThumbnailUrl() {
        return thumbnailLink;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("thumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailLink = thumbnailUrl;
    }
}

