package com.streamsphere.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.UserWatchHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserWatchHistoryRepo extends JpaRepository<UserWatchHistory, Integer> {
    Optional<UserWatchHistory> findByUserIdAndVideoId(UUID userId, String videoId);
    Page<UserWatchHistory> findByUserIdOrderByLastWatchedDesc(UUID userId, Pageable pageable);
    void deleteByUserId(UUID userId);
}

