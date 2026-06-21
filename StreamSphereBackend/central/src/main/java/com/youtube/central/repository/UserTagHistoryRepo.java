package com.youtube.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.youtube.central.models.UserTagHistory;

@Repository
public interface UserTagHistoryRepo extends JpaRepository<UserTagHistory, Integer> {

}
