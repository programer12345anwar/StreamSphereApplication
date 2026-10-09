package com.streamsphere.central.repository;

import com.streamsphere.central.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommentRepo extends JpaRepository<Comment, UUID> {
    Page<Comment> findByVideoIdAndParentCommentIsNullOrderByCreatedAtDesc(String videoId, Pageable pageable);
    Page<Comment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentId, Pageable pageable);
}
