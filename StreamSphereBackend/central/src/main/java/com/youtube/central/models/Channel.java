package com.youtube.central.models;

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
    @OneToMany // ChannelId vs UserId
    private List<AppUser> subscribers;
    //mappedBy for bidirectional link
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos;
    @OneToMany
    private List<PlayList> playLists;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}