package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.time.LocalDateTime;
import com.streamsphere.central.enums.VideoStatus;
import com.streamsphere.central.enums.VideoVisibility;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "videos")
public class Video {
    @Id
    private String id; // This id will get generated inside imagekit
    private String name;
    private String description;
    private LocalDateTime uploadDateTime;
    private LocalDateTime updatedAt;
    private String videoLink;
    private String thumbnailLink;
    private int views;
    private boolean isShort;

    @Enumerated(EnumType.STRING)
    private VideoStatus status = VideoStatus.PUBLISHED;

    @Enumerated(EnumType.STRING)
    private VideoVisibility visibility = VideoVisibility.PUBLIC;

    @ManyToOne
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @ManyToMany
    @JoinTable(
            name = "videos_tags",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
}
