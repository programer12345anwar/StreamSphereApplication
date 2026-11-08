package com.streamsphere.central.repository;

import com.streamsphere.central.entity.VideoProcessingOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoProcessingOutboxRepo extends JpaRepository<VideoProcessingOutbox, UUID> {
    List<VideoProcessingOutbox> findTop25ByPublishedAtIsNullOrderByCreatedAtAsc();
}
