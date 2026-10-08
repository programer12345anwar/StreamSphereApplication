package com.streamsphere.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.streamsphere.central.entity.Video;

import java.util.List;

@Repository
public interface VideoRepo extends JpaRepository<Video, String>{
    List<Video> findAllByOrderByUploadDateTimeDesc();
}

