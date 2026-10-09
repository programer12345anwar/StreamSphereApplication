package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "video_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "video_id"})
})
public class VideoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // True for Like, False for Dislike
    @Column(nullable = false)
    private boolean isLike;

    private LocalDateTime createdAt = LocalDateTime.now();
}
