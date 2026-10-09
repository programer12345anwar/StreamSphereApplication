package com.streamsphere.central.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streamsphere.central.event.model.NotificationMessage;
import com.streamsphere.central.event.producer.RabbitMqService;
import com.streamsphere.central.dto.response.VideoDetailsResponseDTO;
import com.streamsphere.central.dto.response.VideoFeedItemDTO;
import com.streamsphere.central.dto.request.VideoDetailsDTO;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.entity.Channel;
import com.streamsphere.central.entity.Tag;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.repository.VideoRepo;

@Service
public class VideoService {

    @Autowired
    ChannelService channelService;
    @Autowired
    TagService tagService;
    @Autowired
    VideoRepo videoRepo;
    @Autowired
    RabbitMqService rabbitMqService;
    String videoLink;

    public void saveVideo(Video video){
        // Video repository
         videoRepo.save(video);
    }

    @CacheEvict(value = {"video:feed", "video:trending", "video:byId"}, allEntries = true)
    public void saveVideoDetails(UUID channelId,
                                 VideoDetailsDTO videoDetailsDTO){
        // We need to get the channel object
        Channel channel  = channelService.getChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel does not exist.");
        }

        Video video = new Video();
        video.setId(videoDetailsDTO.getId());
        video.setName(videoDetailsDTO.getName());
        video.setDescription(videoDetailsDTO.getDescription());
        video.setVideoLink(videoDetailsDTO.getVideoLink());
        video.setThumbnailLink(null);
        video.setViews(0);
        video.setUpdatedAt(videoDetailsDTO.getUpdatedAt());
        video.setUploadDateTime(videoDetailsDTO.getUploadDateTime());
        video.setChannel(channel);
        
        if (videoDetailsDTO.getStatus() != null) {
            video.setStatus(com.streamsphere.central.enums.VideoStatus.valueOf(videoDetailsDTO.getStatus()));
        }
        if (videoDetailsDTO.getVisibility() != null) {
            video.setVisibility(com.streamsphere.central.enums.VideoVisibility.valueOf(videoDetailsDTO.getVisibility()));
        }

        List<String> tags = videoDetailsDTO.getTags();
        List<Tag> dbTagList = tagService.getAllTagsFromSystem(tags == null ? List.of() : tags);
        video.setTags(dbTagList);
        video.setShort(videoDetailsDTO.isShort());
        // save these video details inside video table.
        this.saveVideo(video);
        this.videoLink = videoDetailsDTO.getVideoLink();
        
        // Only notify if PUBLISHED (or we can just skip for now since it's PROCESSING initially)
        if (video.getStatus() == com.streamsphere.central.enums.VideoStatus.PUBLISHED) {
            if (channel.getSubscribers() != null && !channel.getSubscribers().isEmpty()) {
                this.notifySubscibers(channel.getSubscribers());
            }
        }
        
        // update channel list
        if (channel.getVideos() == null) {
            channel.setVideos(new java.util.ArrayList<>());
        }
        boolean exists = channel.getVideos().stream().anyMatch(v -> v.getId().equals(video.getId()));
        if (!exists) {
            channel.getVideos().add(video);
            channel.setUpdatedAt(java.time.LocalDateTime.now());
            channelService.updateChannel(channel);
        }
    }
    
    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
    public void updateVideoStatus(String videoId, String statusStr) {
        Video video = videoRepo.findById(videoId).orElseThrow(() -> new IllegalArgumentException("Video not found"));
        com.streamsphere.central.enums.VideoStatus status = com.streamsphere.central.enums.VideoStatus.valueOf(statusStr);
        video.setStatus(status);
        videoRepo.save(video);
        
        if (status == com.streamsphere.central.enums.VideoStatus.PUBLISHED) {
            Channel channel = video.getChannel();
            if (channel != null) {
                // Notify the creator that the video is processed
                NotificationMessage msg = new NotificationMessage();
                msg.setEmail(channel.getUser().getEmail());
                msg.setType("video_processed");
                msg.setName(video.getId()); // Using ID to construct URL in notification-api
                rabbitMqService.insertMessageToQueue(msg);

                // Notify all subscribers
                if (channel.getSubscribers() != null && !channel.getSubscribers().isEmpty()) {
                    this.notifySubscibers(channel.getSubscribers());
                }
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
    public void updateVideoDetails(String videoId, VideoDetailsDTO videoDetailsDTO) {
        Video video = videoRepo.findById(videoId).orElseThrow(() -> new IllegalArgumentException("Video not found"));
        
        if (videoDetailsDTO.getThumbnailLink() != null) {
            video.setThumbnailLink(videoDetailsDTO.getThumbnailLink());
        }
        if (videoDetailsDTO.getName() != null) {
            video.setName(videoDetailsDTO.getName());
        }
        if (videoDetailsDTO.getDescription() != null) {
            video.setDescription(videoDetailsDTO.getDescription());
        }
        
        videoRepo.save(video);
    }

    public void notifySubscibers(List<AppUser> subscribers){
        for(int i = 0; i < subscribers.size(); i++){
            AppUser subscriber = subscribers.get(i);
            NotificationMessage notificationMessage = new NotificationMessage();
            notificationMessage.setName(videoLink);
            notificationMessage.setType("new_video");
            notificationMessage.setEmail(subscriber.getEmail());
            rabbitMqService.insertMessageToQueue(notificationMessage);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "video:feed", key = "#limit")
    public List<VideoFeedItemDTO> getLatestVideos(int limit) {
        org.springframework.data.domain.Page<Video> page = videoRepo.findFeedVideos(org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
        return page.stream()
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "video:trending", key = "#limit")
    public List<VideoFeedItemDTO> getTrendingVideos(int limit) {
        java.time.LocalDateTime lastWeek = java.time.LocalDateTime.now().minusDays(7);
        org.springframework.data.domain.Page<Video> page = videoRepo.findTrendingVideos(lastWeek, org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
        return page.stream()
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoFeedItemDTO> searchVideos(String query, int limit) {
        org.springframework.data.domain.Page<Video> page = videoRepo.searchVideos(query, org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
        return page.stream()
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoFeedItemDTO> getSubscribedVideos(UUID userId, int limit) {
        org.springframework.data.domain.Page<Video> page = videoRepo.findSubscribedVideos(userId, org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
        return page.stream()
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "video:shorts", key = "#limit")
    public List<VideoFeedItemDTO> getShorts(int limit) {
        org.springframework.data.domain.Page<Video> page = videoRepo.findShorts(org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
        return page.stream()
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "video:byId", key = "#root.args[0]")
    public VideoDetailsResponseDTO getVideoById(String videoId) {
        return videoRepo.findById(videoId)
                .map(this::toVideoDetailsResponseDTO)
                .orElse(null);
    }

    private VideoFeedItemDTO toVideoFeedItemDTO(Video video) {
        Channel channel = video.getChannel();
        List<String> tags = video.getTags() == null
                ? List.of()
                : video.getTags().stream().map(Tag::getName).toList();
        return new VideoFeedItemDTO(
                video.getId(),
                video.getName(),
                video.getDescription(),
                video.getVideoLink(),
                video.getThumbnailLink(),
                video.getViews(),
                video.getUploadDateTime(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName(),
                tags,
                video.isShort()
        );
    }

    private VideoDetailsResponseDTO toVideoDetailsResponseDTO(Video video) {
        Channel channel = video.getChannel();
        List<String> tags = video.getTags() == null
                ? List.of()
                : video.getTags().stream().map(Tag::getName).toList();

        return new VideoDetailsResponseDTO(
                video.getId(),
                video.getName(),
                video.getDescription(),
                video.getVideoLink(),
                video.getThumbnailLink(),
                video.getViews(),
                video.getUploadDateTime(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName(),
                tags,
                video.isShort()
        );
    }
}
