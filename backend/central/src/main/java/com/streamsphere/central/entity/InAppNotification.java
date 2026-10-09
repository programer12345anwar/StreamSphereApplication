package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "in_app_notifications")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user; // The recipient of the notification

    private String type; // e.g., "LIKE", "COMMENT", "SUBSCRIBE"
    
    private String message;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Optional: Reference to the related video
    @ManyToOne
    @JoinColumn(name = "video_id")
    private Video video;
}
