package com.streamsphere.central.repository;

import com.streamsphere.central.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VideoLikeRepo extends JpaRepository<VideoLike, UUID> {
    Optional<VideoLike> findByUserIdAndVideoId(UUID userId, String videoId);
    long countByVideoIdAndIsLikeTrue(String videoId);
    long countByVideoIdAndIsLikeFalse(String videoId);
}
