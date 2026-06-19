package com.youtube.central.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.youtube.central.dto.NotificationMessage;
import com.youtube.central.dto.VideoDetailsResponseDTO;
import com.youtube.central.dto.VideoFeedItemDTO;
import com.youtube.central.dto.VideoDetailsDTO;
import com.youtube.central.models.AppUser;
import com.youtube.central.models.Channel;
import com.youtube.central.models.Tag;
import com.youtube.central.models.Video;
import com.youtube.central.repository.VideoRepo;

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

        List<String> tags = videoDetailsDTO.getTags();
        List<Tag> dbTagList = tagService.getAllTagsFromSystem(tags == null ? List.of() : tags);
        video.setTags(dbTagList);
        // save these video details inside video table.
        this.saveVideo(video);
        this.videoLink = videoDetailsDTO.getVideoLink();
        // we need to update list videos of channel
        if (channel.getVideos() == null) {
            channel.setVideos(new java.util.ArrayList<>());
        }
        channel.getVideos().add(video);
        channel.setUpdatedAt(java.time.LocalDateTime.now());
        channelService.updateChannel(channel);
        // We need to notify all the subscribers that hey a new uploaded over the channel
        if (channel.getSubscribers() != null && !channel.getSubscribers().isEmpty()) {
            this.notifySubscibers(channel.getSubscribers());
        }
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

    public List<VideoFeedItemDTO> getLatestVideos(int limit) {
        List<Video> videos = videoRepo.findAllByOrderByUploadDateTimeDesc();
        return videos.stream()
                .limit(Math.max(limit, 0))
                .map(this::toVideoFeedItemDTO)
                .collect(Collectors.toList());
    }

    public VideoDetailsResponseDTO getVideoById(String videoId) {
        return videoRepo.findById(videoId)
                .map(this::toVideoDetailsResponseDTO)
                .orElse(null);
    }

    private VideoFeedItemDTO toVideoFeedItemDTO(Video video) {
        Channel channel = video.getChannel();
        return new VideoFeedItemDTO(
                video.getId(),
                video.getName(),
                video.getDescription(),
                video.getVideoLink(),
                video.getThumbnailLink(),
                video.getViews(),
                video.getUploadDateTime(),
                channel == null ? null : channel.getId(),
                channel == null ? null : channel.getName()
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
                tags
        );
    }
}
