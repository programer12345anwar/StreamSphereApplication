package com.streamsphere.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.UserWatchHistory;

@Repository
public interface UserWatchHistoryRepo extends JpaRepository<UserWatchHistory, Integer> {

}

