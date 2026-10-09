










































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
    @GetMapping("/trending")
    public List<VideoFeedItemDTO> getTrendingFeed(@RequestParam(name = "limit", defaultValue = "30") int limit) {
        return videoService.getTrendingVideos(limit);
    }

    @GetMapping("/search")
    public List<VideoFeedItemDTO> searchVideos(@RequestParam(name = "q") String query,
                                               @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return videoService.searchVideos(query, limit);
    }

    @GetMapping("/subscriptions")
    public List<VideoFeedItemDTO> getSubscriptionsFeed(@RequestParam(name = "userId") java.util.UUID userId,
                                                       @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return videoService.getSubscribedVideos(userId, limit);
    }

    @GetMapping("/shorts")
    public List<VideoFeedItemDTO> getShorts(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return videoService.getShorts(limit);
    }
}

