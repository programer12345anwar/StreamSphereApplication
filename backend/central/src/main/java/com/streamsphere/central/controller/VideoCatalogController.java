










































package com.streamsphere.central.controller;

import com.streamsphere.central.dto.response.VideoDetailsResponseDTO;
import com.streamsphere.central.dto.response.VideoFeedItemDTO;
import com.streamsphere.central.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/central/videos")
public class VideoCatalogController {

    @Autowired
    VideoService videoService;

    @GetMapping
    public List<VideoFeedItemDTO> getFeed(@RequestParam(name = "limit", defaultValue = "30") int limit) {
        return videoService.getLatestVideos(limit);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoDetailsResponseDTO> getVideoById(@PathVariable(name = "videoId") String videoId) {
        VideoDetailsResponseDTO video = videoService.getVideoById(videoId);
        if (video == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(video);
    }
}

