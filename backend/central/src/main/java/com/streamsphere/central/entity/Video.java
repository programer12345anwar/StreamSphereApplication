package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.*;

 import java.util.List;
import java.time.LocalDateTime;

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
    private int views; //added
    // Add this field to link back to Channel
    @ManyToOne
    @JoinColumn(name = "channel_id") // foreign key in Video table
    private Channel channel;
    @ManyToMany
    @JoinTable(
            name = "videos_tags",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
}

