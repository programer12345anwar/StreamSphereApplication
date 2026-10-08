package com.streamsphere.central.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Table(name = "channels")
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String name; //channel name
    @ManyToOne
    private AppUser user; // channel owner
    private String description;
    private Double watchHours;
    private boolean isMonetized;
    private int totalViews;
    private int totalLikeCount;
    private int totalSubs;
    @ManyToMany
    @JoinTable(
            name = "channels_subscribers",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "subscriber_id")
    )
    private List<AppUser> subscribers;
    //mappedBy for bidirectional link
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos;
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayList> playLists;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
