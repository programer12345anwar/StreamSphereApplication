package com.streamsphere.central.repository;

import com.streamsphere.central.entity.CommentLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepo extends JpaRepository<CommentLike, UUID> {
    interface LikeCount {
        UUID getCommentId();
        long getLikeCount();
    }

    long countByCommentId(UUID commentId);
    Optional<CommentLike> findByUserIdAndCommentId(UUID userId, UUID commentId);

    @Query("""
            select item.comment.id as commentId, count(item.id) as likeCount
            from CommentLike item
            where item.comment.id in :commentIds
            group by item.comment.id
            """)
    List<LikeCount> findLikeCounts(@Param("commentIds") Collection<UUID> commentIds);

    @Query("""
            select item.comment.id
            from CommentLike item
            where item.user.id = :userId and item.comment.id in :commentIds
            """)
    List<UUID> findLikedCommentIds(@Param("userId") UUID userId,
                                   @Param("commentIds") Collection<UUID> commentIds);
}
