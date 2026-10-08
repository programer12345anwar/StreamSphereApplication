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

    @PostMapping("/{channelId}/video/upload")
    public void saveVideoDetails(@Valid @RequestBody VideoDetailsDTO videoDetailsDTO,
                                 @PathVariable(name = "channelId") UUID channelId){
        videoService.saveVideoDetails(channelId, videoDetailsDTO);
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

