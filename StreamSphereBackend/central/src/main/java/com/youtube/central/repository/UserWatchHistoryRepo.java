package com.youtube.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.youtube.central.models.UserWatchHistory;

@Repository
public interface UserWatchHistoryRepo extends JpaRepository<UserWatchHistory, Integer> {

}
