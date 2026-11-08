package com.streamsphere.central.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "comment_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    protected CommentLike() {}

    public CommentLike(AppUser user, Comment comment) {
        this.user = user;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public Comment getComment() { return comment; }
}
