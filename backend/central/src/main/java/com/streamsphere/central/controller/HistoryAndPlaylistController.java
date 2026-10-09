package com.streamsphere.central.controller;

import com.streamsphere.central.entity.PlayList;
import com.streamsphere.central.entity.UserWatchHistory;
import com.streamsphere.central.service.HistoryAndPlaylistService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/central/library")
public class HistoryAndPlaylistController {

    private final HistoryAndPlaylistService service;

    public HistoryAndPlaylistController(HistoryAndPlaylistService service) {
        this.service = service;
    }

    // --- Watch History APIs ---

    @PostMapping("/history")
    public ResponseEntity<Void> recordWatchHistory(@RequestParam UUID userId, @RequestParam String videoId) {
        service.recordWatchHistory(userId, videoId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<Page<UserWatchHistory>> getWatchHistory(@RequestParam UUID userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(service.getUserWatchHistory(userId, page, size));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearWatchHistory(@RequestParam UUID userId) {
        service.clearWatchHistory(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/history/resume")
    public ResponseEntity<Void> updateResumeTime(@RequestParam UUID userId, @RequestParam String videoId, @RequestParam int resumeTime) {
        service.updateResumeTime(userId, videoId, resumeTime);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/resume")
    public ResponseEntity<Integer> getResumeTime(@RequestParam UUID userId, @RequestParam String videoId) {
        return ResponseEntity.ok(service.getResumeTime(userId, videoId));
    }

    // --- PlayList APIs ---

    @PostMapping("/playlist")
    public ResponseEntity<PlayList> createPlaylist(@RequestParam UUID channelId, @RequestParam String name) {
        return ResponseEntity.ok(service.createPlaylist(channelId, name));
    }

    @GetMapping("/channel/{channelId}/playlists")
    public ResponseEntity<List<PlayList>> getChannelPlaylists(@PathVariable UUID channelId) {
        return ResponseEntity.ok(service.getChannelPlaylists(channelId));
    }

    @PostMapping("/playlist/{playlistId}/video/{videoId}")
    public ResponseEntity<Void> addVideoToPlaylist(@PathVariable UUID playlistId, @PathVariable String videoId) {
        service.addVideoToPlaylist(playlistId, videoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/playlist/{playlistId}/video/{videoId}")
    public ResponseEntity<Void> removeVideoFromPlaylist(@PathVariable UUID playlistId, @PathVariable String videoId) {
        service.removeVideoFromPlaylist(playlistId, videoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/playlist/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID playlistId) {
        service.deletePlaylist(playlistId);
        return ResponseEntity.ok().build();
    }
}
