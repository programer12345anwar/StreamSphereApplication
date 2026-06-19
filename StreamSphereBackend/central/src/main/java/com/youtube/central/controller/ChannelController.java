package com.youtube.central.controller;

import java.util.List;
import java.util.UUID;

import com.youtube.central.models.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.youtube.central.dto.ChannelSummaryDTO;
import com.youtube.central.dto.CreateChannelRequestBody;
import com.youtube.central.dto.VideoDetailsDTO;
import com.youtube.central.service.ChannelService;
import com.youtube.central.service.VideoService;

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
    public void createChannel(@RequestBody CreateChannelRequestBody channelDetails){
        log.info("Channel details {}", channelDetails);
        channelService.createChannel(channelDetails);
    }


    @PutMapping("/{channelId}/subscribe")
    public void addSubscriber(@PathVariable UUID channelId,
                              @RequestParam UUID userId){
        channelService.addSubscriber(userId, channelId);
    }

    @PostMapping("/{channelId}/video/upload")
    public void saveVideoDetails(@RequestBody VideoDetailsDTO videoDetailsDTO,
                                 @PathVariable UUID channelId){
        videoService.saveVideoDetails(channelId, videoDetailsDTO);
    }

    //placement mock
   /* “I’d create a custom JPA query in my ChannelRepo to fetch all channels having more than 20 videos and more than 50 subscribers.
    I can use JPQL with the size() function or a native SQL subquery.
    Then expose a REST endpoint /api/v1/central/channel/popular to return that filtered list to the frontend.” */
    @GetMapping("/popular")
    public List<Channel> getPopularChannels() {
        return channelService.getPopularChannels();
    }

    @GetMapping("/channelWithTag")
    public List<Channel> getChannelByTag(@RequestParam String tag){
        return channelService.getChannels(tag);
    }

    @GetMapping("/my-channel")
    public ChannelSummaryDTO getMyChannel(@RequestParam String email) {
        Channel channel = channelService.getChannelByUserEmail(email);
        if (channel == null) {
            return null;
        }

        return new ChannelSummaryDTO(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getTotalSubs(),
                channel.getTotalViews()
        );
    }

    
    
}
