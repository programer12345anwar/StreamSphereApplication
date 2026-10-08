package com.streamsphere.central.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.PlayList;

@Repository
public interface PlayListRepo extends JpaRepository<PlayList, UUID> {

}

