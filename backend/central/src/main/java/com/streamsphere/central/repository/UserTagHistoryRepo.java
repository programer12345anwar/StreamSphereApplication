package com.streamsphere.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.UserTagHistory;

@Repository
public interface UserTagHistoryRepo extends JpaRepository<UserTagHistory, Integer> {

}

