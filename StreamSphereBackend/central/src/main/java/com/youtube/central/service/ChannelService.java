package com.youtube.central.service;

import com.youtube.central.dto.CreateChannelRequestBody;
import com.youtube.central.dto.NotificationMessage;
import com.youtube.central.exception.ChannelNotFound;
import com.youtube.central.exception.UserNotFound;
import com.youtube.central.models.AppUser;
import com.youtube.central.models.Channel;
import com.youtube.central.repository.ChannelRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ChannelService {

    @Autowired
    UserService userService;

    @Autowired
    RabbitMqService rabbitMqService;

    @Autowired
    ChannelRepo channelRepo;

    public Channel getChannelById(UUID channelId){
        return channelRepo.findById(channelId).orElse(null);
    }

    public Channel getChannelByUserEmail(String email) {
        return channelRepo.findByUserEmail(email);
    }

    public void updateChannel(Channel channel){
        channelRepo.save(channel);
    }

    public void createChannel(CreateChannelRequestBody channelDetails) {
        log.info("Channel details {}", channelDetails);
        String email=channelDetails.getUserEmail();
        //we need to check with this email user is registered or not
        AppUser user=userService.getUserByEmail(email);
        if(user==null){
            //user is not registered
            throw new UserNotFound(String.format("user with email %s does not exist in our system",email));
        }
        //we need to create channel
        Channel channel=new Channel();
        channel.setCreatedAt(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        channel.setMonetized(false);
        channel.setUser(user);
        channel.setName(channelDetails.getChannelName());
        channel.setDescription(channelDetails.getDescription());
        channel.setWatchHours(0.0);
        channel.setTotalViews(0);
        channel.setTotalLikeCount(0);
        channel.setTotalSubs(0);
        channel.setSubscribers(new ArrayList<>());
        channel.setVideos(new ArrayList<>());
        channel.setPlayLists(new ArrayList<>());

        //call repository layer to save the channel
        channelRepo.save(channel);
        //Insert channel creation message payload inside rabbit mq queue.
         NotificationMessage notificationMessage = new NotificationMessage();
         notificationMessage.setName(user.getName());
         notificationMessage.setEmail(user.getEmail());
         notificationMessage.setType("create_channel");
         //to upload message to queue we need rabbitMqService
         rabbitMqService.insertMessageToQueue(notificationMessage);
    }
    public void addSubscriber(UUID userId, UUID channelId){

        // I need to validate both userId and channelId

        AppUser user = userService.getUserById(userId);
        // We are checking userId is present in our system or not
        if(user == null){
            throw new UserNotFound(String.format("" +
                    "User with id %s does not exist in the system.", userId.toString()));
        }
        // We need to check channelId is present in our system or not

        Channel channel = this.getChannelById(channelId);
        if(channel == null){
            // That means channel does not exist in system
            throw new ChannelNotFound(String.format("Channel with channelId %s does not exist in system", channelId));
        }
        channel.setTotalSubs(channel.getTotalSubs() + 1);
        List<AppUser> subscribers = channel.getSubscribers();
        if (subscribers == null) {
            subscribers = new ArrayList<>();
            channel.setSubscribers(subscribers);
        }
        subscribers.add(user);
        channelRepo.save(channel);

        // channel owner should get mail hey new subscriber added in your channel
        // Notification Message -> I will pass this notification message to the messaging queue

        NotificationMessage message = new NotificationMessage();
        message.setEmail(channel.getUser().getEmail());
        message.setType("subscriber_added");
        message.setName(channel.getName());

        rabbitMqService.insertMessageToQueue(message);
    }

    //placement mock
    public List<Channel> getPopularChannels() {
        return channelRepo.findPopularChannels();
    }

    public List<Channel> getChannels(String tag){
        List<Channel> channels=channelRepo.findAll();
        return channels.stream()
                .filter(c -> c.getVideos() != null && c.getVideos().stream()
                        .anyMatch(v -> v.getTags() != null && v.getTags().stream()
                                .anyMatch(t -> t.getName() != null && t.getName().equalsIgnoreCase(tag))))
                .toList();
    }
}
