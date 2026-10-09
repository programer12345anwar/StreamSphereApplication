package com.streamsphere.central.service;

import com.streamsphere.central.entity.*;
import com.streamsphere.central.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HistoryAndPlaylistService {

    private final UserWatchHistoryRepo watchHistoryRepo;
    private final PlayListRepo playListRepo;
    private final ChannelRepo channelRepo;
    private final VideoRepo videoRepo;

    public HistoryAndPlaylistService(UserWatchHistoryRepo watchHistoryRepo, PlayListRepo playListRepo,
                                     ChannelRepo channelRepo, VideoRepo videoRepo) {
        this.watchHistoryRepo = watchHistoryRepo;
        this.playListRepo = playListRepo;
        this.channelRepo = channelRepo;
        this.videoRepo = videoRepo;
    }

    @Transactional
    public void recordWatchHistory(UUID userId, String videoId) {
        Optional<UserWatchHistory> existing = watchHistoryRepo.findByUserIdAndVideoId(userId, videoId);
        if (existing.isPresent()) {
            UserWatchHistory history = existing.get();
            history.setCount(history.getCount() + 1);
            history.setLastWatched(LocalDateTime.now());
            watchHistoryRepo.save(history);
        } else {
            UserWatchHistory history = new UserWatchHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setCount(1);
            history.setLastWatched(LocalDateTime.now());
            history.setLiked(false);
            history.setResumeTime(0);
            watchHistoryRepo.save(history);
        }
    }

    @Transactional
    public void updateResumeTime(UUID userId, String videoId, int resumeTime) {
        Optional<UserWatchHistory> existing = watchHistoryRepo.findByUserIdAndVideoId(userId, videoId);
        if (existing.isPresent()) {
            UserWatchHistory history = existing.get();
            history.setResumeTime(resumeTime);
            history.setLastWatched(LocalDateTime.now());
            watchHistoryRepo.save(history);
        } else {
            UserWatchHistory history = new UserWatchHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setCount(1);
            history.setLastWatched(LocalDateTime.now());
            history.setLiked(false);
            history.setResumeTime(resumeTime);
            watchHistoryRepo.save(history);
        }
    }

    public int getResumeTime(UUID userId, String videoId) {
        return watchHistoryRepo.findByUserIdAndVideoId(userId, videoId)
                .map(UserWatchHistory::getResumeTime)
                .orElse(0);
    }

    public Page<UserWatchHistory> getUserWatchHistory(UUID userId, int page, int size) {
        return watchHistoryRepo.findByUserIdOrderByLastWatchedDesc(userId, PageRequest.of(page, size));
    }

    @Transactional
    public void clearWatchHistory(UUID userId) {
        watchHistoryRepo.deleteByUserId(userId);
    }

    @Transactional
    public PlayList createPlaylist(UUID channelId, String name) {
        Channel channel = channelRepo.findById(channelId).orElseThrow();
        PlayList playList = new PlayList(null, name, channel, new ArrayList<>());
        return playListRepo.save(playList);
    }

    public List<PlayList> getChannelPlaylists(UUID channelId) {
        return playListRepo.findByChannelId(channelId);
    }

    @Transactional
    public void addVideoToPlaylist(UUID playlistId, String videoId) {
        PlayList playList = playListRepo.findById(playlistId).orElseThrow();
        Video video = videoRepo.findById(videoId).orElseThrow();
        if (playList.getVideos() == null) {
            playList.setVideos(new ArrayList<>());
        }
        if (!playList.getVideos().contains(video)) {
            playList.getVideos().add(video);
            playListRepo.save(playList);
        }
    }

    @Transactional
    public void removeVideoFromPlaylist(UUID playlistId, String videoId) {
        PlayList playList = playListRepo.findById(playlistId).orElseThrow();
        if (playList.getVideos() != null) {
            playList.getVideos().removeIf(v -> v.getId().equals(videoId));
            playListRepo.save(playList);
        }
    }

    @Transactional
    public void deletePlaylist(UUID playlistId) {
        playListRepo.deleteById(playlistId);
    }
}
