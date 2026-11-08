package com.streamsphere.central.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_processing_outbox")
@Getter
@Setter
@NoArgsConstructor
public class VideoProcessingOutbox {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String videoId;

    @Column(nullable = false, length = 2048)
    private String videoUrl;

    @Column(length = 2048)
    private String customThumbnailUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @PrePersist
    void initializeDefaults() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
