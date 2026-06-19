package com.youtube.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.youtube.central.models.Video;

import java.util.List;

@Repository
public interface VideoRepo extends JpaRepository<Video, String>{
    List<Video> findAllByOrderByUploadDateTimeDesc();
}
