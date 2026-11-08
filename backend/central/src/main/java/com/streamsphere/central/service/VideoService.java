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
import com.streamsphere.video.events.VideoThumbnailResultEvent;
import com.streamsphere.central.event.producer.RabbitMqService;
import com.streamsphere.central.dto.response.VideoDetailsResponseDTO;
import com.streamsphere.central.dto.response.VideoFeedItemDTO;
import com.streamsphere.central.dto.request.VideoDetailsDTO;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.entity.Channel;
import com.streamsphere.central.entity.Tag;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.entity.VideoProcessingOutbox;
import com.streamsphere.central.enums.ThumbnailProcessingStatus;
import com.streamsphere.central.enums.ThumbnailSource;
import com.streamsphere.central.repository.VideoRepo;
import com.streamsphere.central.repository.VideoProcessingOutboxRepo;

@Service
public class VideoService {

    @Autowired
    ChannelService channelService;
    @Autowired
    TagService tagService;
    @Autowired
    VideoRepo videoRepo;
    @Autowired
    VideoProcessingOutboxRepo videoProcessingOutboxRepo;
    @Autowired
    RabbitMqService rabbitMqService;
    public void saveVideo(Video video){
        // Video repository
         videoRepo.save(video);
    }

    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
    public void completeThumbnailProcessing(VideoThumbnailResultEvent event) {
         Video video = videoRepo.findById(event.videoId())
                 .orElseThrow(() -> new IllegalArgumentException("Video not found for thumbnail result."));

         if (video.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.READY
                 || video.getThumbnailProcessingStatus() == ThumbnailProcessingStatus.FALLBACK) {
             return;
         }
         if (event.eventId() == null || !event.eventId().equals(video.getThumbnailProcessingEventId())) {
             return;
         }

         ThumbnailProcessingStatus processingStatus =
                 ThumbnailProcessingStatus.valueOf(event.processingStatus());
         ThumbnailSource source = ThumbnailSource.valueOf(event.thumbnailSource());
         if (processingStatus != ThumbnailProcessingStatus.READY
                 && processingStatus != ThumbnailProcessingStatus.FALLBACK) {
             throw new IllegalArgumentException("Invalid completed thumbnail processing status.");
         }
         if (processingStatus == ThumbnailProcessingStatus.FALLBACK && source != ThumbnailSource.DEFAULT) {
             throw new IllegalArgumentException("Fallback thumbnails must use the default source.");
         }
         if (source == ThumbnailSource.GENERATED
                 && (event.thumbnailUrl() == null || event.thumbnailUrl().isBlank())) {
             throw new IllegalArgumentException("Generated thumbnail URL is required.");
         }
         if (source == ThumbnailSource.CUSTOM
                 && (video.getThumbnailLink() == null || !video.getThumbnailLink().equals(event.thumbnailUrl()))) {
             throw new IllegalArgumentException("Custom thumbnail result does not match the stored asset.");
         }

         video.setThumbnailLink(event.thumbnailUrl());
         video.setThumbnailSource(source);
         video.setThumbnailProcessingStatus(processingStatus);
         if (event.durationSeconds() != null && event.durationSeconds() > 0) {
             video.setDurationSeconds(event.durationSeconds());
         }
         if (video.getStatus() == com.streamsphere.central.enums.VideoStatus.PROCESSING) {
             updateVideoStatus(video.getId(), com.streamsphere.central.enums.VideoStatus.PUBLISHED.name());
         }
    }

    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
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
        video.setThumbnailLink(videoDetailsDTO.getThumbnailLink());
        video.setThumbnailSource(videoDetailsDTO.getThumbnailLink() == null
                ? null
                : ThumbnailSource.CUSTOM);
        video.setThumbnailProcessingStatus(ThumbnailProcessingStatus.PENDING);
        video.setDurationSeconds(null);
        video.setViews(0);
        video.setUpdatedAt(videoDetailsDTO.getUpdatedAt());
        video.setUploadDateTime(videoDetailsDTO.getUploadDateTime());
        video.setChannel(channel);
        
        video.setStatus(com.streamsphere.central.enums.VideoStatus.PROCESSING);
        if (videoDetailsDTO.getVisibility() != null) {
            video.setVisibility(com.streamsphere.central.enums.VideoVisibility.valueOf(videoDetailsDTO.getVisibility()));
        }

        List<String> tags = videoDetailsDTO.getTags();
        List<Tag> dbTagList = tagService.getAllTagsFromSystem(tags == null ? List.of() : tags);
        video.setTags(dbTagList);
        video.setShort(videoDetailsDTO.isShort());
        VideoProcessingOutbox outboxEvent = new VideoProcessingOutbox();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setVideoId(video.getId());
        outboxEvent.setVideoUrl(video.getVideoLink());
        outboxEvent.setCustomThumbnailUrl(video.getThumbnailLink());
        video.setThumbnailProcessingEventId(outboxEvent.getId());
        // Save video and its processing event atomically.
        this.saveVideo(video);
        videoProcessingOutboxRepo.save(outboxEvent);
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
        boolean alreadyPublished = video.getStatus() == com.streamsphere.central.enums.VideoStatus.PUBLISHED;
        com.streamsphere.central.enums.VideoStatus status = com.streamsphere.central.enums.VideoStatus.valueOf(statusStr);
        video.setStatus(status);
        videoRepo.save(video);
        
        if (status == com.streamsphere.central.enums.VideoStatus.PUBLISHED && !alreadyPublished) {
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
                    this.notifySubscibers(channel.getSubscribers(), video.getVideoLink());
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
            video.setThumbnailSource(ThumbnailSource.CUSTOM);
            video.setThumbnailProcessingStatus(ThumbnailProcessingStatus.READY);
        }
        if (videoDetailsDTO.getName() != null) {
            video.setName(videoDetailsDTO.getName());
        }
        if (videoDetailsDTO.getDescription() != null) {
            video.setDescription(videoDetailsDTO.getDescription());
        }
        
        videoRepo.save(video);
    }

    @Transactional(readOnly = true)
    public boolean videoBelongsToChannel(String videoId, UUID channelId) {
        return videoRepo.findById(videoId)
                .map(video -> video.getChannel() != null
                        && channelId.equals(video.getChannel().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean videoBelongsToUser(String videoId, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return videoRepo.findById(videoId)
                .map(video -> video.getChannel() != null
                        && video.getChannel().getUser() != null
                        && email.equalsIgnoreCase(video.getChannel().getUser().getEmail()))
                .orElse(false);
    }

    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
    public void replaceVideoMedia(UUID channelId, String videoId, String videoLink) {
        replaceVideoMedia(channelId, videoId, videoLink, null);
    }

    @Transactional
    @CacheEvict(value = {"video:feed", "video:trending", "video:byId", "video:shorts"}, allEntries = true)
    public void replaceVideoMedia(UUID channelId, String videoId, String videoLink, String customThumbnailUrl) {
        Video video = videoRepo.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found."));
        if (video.getChannel() == null || !channelId.equals(video.getChannel().getId())) {
            throw new IllegalArgumentException("Video does not belong to the specified channel.");
        }
        video.setVideoLink(videoLink);
        video.setUpdatedAt(java.time.LocalDateTime.now());
        if (customThumbnailUrl != null) {
            video.setThumbnailLink(customThumbnailUrl);
            video.setThumbnailSource(ThumbnailSource.CUSTOM);
        }
        video.setThumbnailProcessingStatus(ThumbnailProcessingStatus.PENDING);
        video.setDurationSeconds(null);
        VideoProcessingOutbox outboxEvent = new VideoProcessingOutbox();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setVideoId(video.getId());
        outboxEvent.setVideoUrl(video.getVideoLink());
        outboxEvent.setCustomThumbnailUrl(
                video.getThumbnailSource() == ThumbnailSource.CUSTOM ? video.getThumbnailLink() : null);
        video.setThumbnailProcessingEventId(outboxEvent.getId());
        videoRepo.save(video);
        videoProcessingOutboxRepo.save(outboxEvent);
    }

    public void notifySubscibers(List<AppUser> subscribers, String videoLink){
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
                video.getThumbnailSource(),
                video.getThumbnailProcessingStatus(),
                video.getDurationSeconds(),
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
                video.getThumbnailSource(),
                video.getThumbnailProcessingStatus(),
                video.getDurationSeconds(),
                video.getViews(),
                video.getUploadDateTime(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName(),
                tags,
                video.isShort()
        );
    }
}
