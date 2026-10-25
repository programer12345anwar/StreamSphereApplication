package com.streamsphere.central.controller;

import java.util.List;
import java.util.UUID;

import com.streamsphere.central.entity.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.streamsphere.central.dto.response.ChannelSummaryDTO;
import com.streamsphere.central.dto.request.CreateChannelRequestBody;
import com.streamsphere.central.dto.request.VideoDetailsDTO;
import com.streamsphere.central.service.ChannelService;
import com.streamsphere.central.service.VideoService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/central/channel")
@Slf4j
public class ChannelController {

    @Autowired
    ChannelService channelService;

    @Autowired
    VideoService videoService;

    @PostMapping("/create")
    public void createChannel(@Valid @RequestBody CreateChannelRequestBody channelDetails){
        log.info("Channel details {}", channelDetails);
        channelService.createChannel(channelDetails);
    }


    @PutMapping("/{channelId}/subscribe")
    public void addSubscriber(@PathVariable(name = "channelId") UUID channelId,
                              @RequestParam(name = "userId") UUID userId){
        channelService.addSubscriber(userId, channelId);
    }

    @PutMapping("/{channelId}/unsubscribe")
    public void removeSubscriber(@PathVariable(name = "channelId") UUID channelId,
                                 @RequestParam(name = "userId") UUID userId){
        channelService.removeSubscriber(userId, channelId);
    }

    @GetMapping("/{channelId}/check-subscription")
    public boolean checkSubscription(@PathVariable(name = "channelId") UUID channelId,
                                     @RequestParam(name = "userId") UUID userId) {
        return channelService.isUserSubscribed(userId, channelId);
    }

    @PostMapping("/{channelId}/video/upload")
    public void saveVideoDetails(@Valid @RequestBody VideoDetailsDTO videoDetailsDTO,
                                 @PathVariable(name = "channelId") UUID channelId){
        videoService.saveVideoDetails(channelId, videoDetailsDTO);
    }

    @GetMapping("/{channelId}/upload-permission")
    public boolean canUploadToChannel(@PathVariable(name = "channelId") UUID channelId,
                                      @RequestParam(name = "videoId", required = false) String videoId,
                                      org.springframework.security.core.Authentication authentication) {
        Channel channel = channelService.getChannelById(channelId);
        boolean ownsChannel = channel != null
                && authentication != null
                && channel.getUser() != null
                && authentication.getName().equalsIgnoreCase(channel.getUser().getEmail());
        return ownsChannel && (videoId == null || videoService.videoBelongsToChannel(videoId, channelId));
    }

    @PutMapping("/{channelId}/video/{videoId}/media")
    public void replaceVideoMedia(@PathVariable UUID channelId,
                                  @PathVariable String videoId,
                                  @RequestBody VideoDetailsDTO payload,
                                  org.springframework.security.core.Authentication authentication) {
        Channel channel = channelService.getChannelById(channelId);
        if (channel == null
                || authentication == null
                || channel.getUser() == null
                || !authentication.getName().equalsIgnoreCase(channel.getUser().getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to replace media for this channel.");
        }
        String videoLink = payload.getVideoLink();
        if (videoLink == null || videoLink.isBlank()) {
            throw new IllegalArgumentException("Video URL is required.");
        }
        if (!"https".equalsIgnoreCase(java.net.URI.create(videoLink).getScheme())) {
            throw new IllegalArgumentException("Video URL must use HTTPS.");
        }
        videoService.replaceVideoMedia(channelId, videoId, videoLink);
    }

    @PutMapping("/video/{videoId}/status")
    public void updateVideoStatus(@PathVariable(name = "videoId") String videoId,
                                  @RequestParam(name = "status") String status) {
        videoService.updateVideoStatus(videoId, status);
    }

    @PutMapping("/video/{videoId}/details")
    public void updateVideoDetails(@PathVariable(name = "videoId") String videoId,
                                   @RequestBody VideoDetailsDTO videoDetailsDTO) {
        videoService.updateVideoDetails(videoId, videoDetailsDTO);
    }

    @GetMapping("/popular")
    public List<ChannelSummaryDTO> getPopularChannels() {
        return channelService.getPopularChannels().stream()
                .map(this::toChannelSummaryDTO)
                .toList();
    }

    @GetMapping("/channelWithTag")
    public List<ChannelSummaryDTO> getChannelByTag(@RequestParam(name = "tag") String tag){
        return channelService.getChannels(tag).stream()
                .map(this::toChannelSummaryDTO)
                .toList();
    }

    @GetMapping("/search")
    public List<ChannelSummaryDTO> searchChannels(@RequestParam(name = "q") String query) {
        return channelService.searchChannels(query).stream()
                .map(this::toChannelSummaryDTO)
                .toList();
    }

    @GetMapping("/subscribed")
    public List<ChannelSummaryDTO> getSubscribedChannels(@RequestParam(name = "userId") UUID userId) {
        return channelService.getSubscribedChannels(userId).stream()
                .map(this::toChannelSummaryDTO)
                .toList();
    }

    @GetMapping("/my-channel")
    public ChannelSummaryDTO getMyChannel(@RequestParam(name = "email") String email) {
        Channel channel = channelService.getChannelByUserEmail(email);
        if (channel == null) {
            return null;
        }

        return toChannelSummaryDTO(channel);
    }

    private ChannelSummaryDTO toChannelSummaryDTO(Channel channel) {
        return new ChannelSummaryDTO(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getTotalSubs(),
                channel.getTotalViews()
        );
    }

    
    
}
