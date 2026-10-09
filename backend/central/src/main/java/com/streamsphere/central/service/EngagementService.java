package com.streamsphere.central.service;

import com.streamsphere.central.entity.*;
import com.streamsphere.central.repository.*;
import com.streamsphere.central.event.model.NotificationMessage;
import com.streamsphere.central.event.producer.RabbitMqService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class EngagementService {

    private final VideoLikeRepo videoLikeRepo;
    private final CommentRepo commentRepo;
    private final VideoRepo videoRepo;
    private final AppUserRepo userRepo;
    private final RabbitMqService rabbitMqService;
    private final StringRedisTemplate redisTemplate;
    private final VideoReportRepo videoReportRepo;
    private final NotificationService notificationService;

    public EngagementService(VideoLikeRepo videoLikeRepo, CommentRepo commentRepo, 
                             VideoRepo videoRepo, AppUserRepo userRepo, 
                             RabbitMqService rabbitMqService, StringRedisTemplate redisTemplate,
                             VideoReportRepo videoReportRepo, NotificationService notificationService) {
        this.videoLikeRepo = videoLikeRepo;
        this.commentRepo = commentRepo;
        this.videoRepo = videoRepo;
        this.userRepo = userRepo;
        this.rabbitMqService = rabbitMqService;
        this.redisTemplate = redisTemplate;
        this.videoReportRepo = videoReportRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public void toggleLike(UUID userId, String videoId, boolean isLike) {
        AppUser user = userRepo.findById(userId).orElseThrow();
        Video video = videoRepo.findById(videoId).orElseThrow();

        Optional<VideoLike> existing = videoLikeRepo.findByUserIdAndVideoId(userId, videoId);
        if (existing.isPresent()) {
            VideoLike like = existing.get();
            if (like.isLike() == isLike) {
                // Toggle off
                videoLikeRepo.delete(like);
                return;
            } else {
                like.setLike(isLike);
                videoLikeRepo.save(like);
            }
        } else {
            VideoLike newLike = new VideoLike(null, user, video, isLike, java.time.LocalDateTime.now());
            videoLikeRepo.save(newLike);
        }

        if (isLike) {
            // Notify video owner
            NotificationMessage msg = new NotificationMessage();
            msg.setEmail(video.getChannel().getUser().getEmail());
            msg.setType("video_liked");
            msg.setName(video.getName());
            rabbitMqService.insertMessageToQueue(msg);
            
            // Send In-App Notification
            if (!user.getId().equals(video.getChannel().getUser().getId())) {
                String message = user.getName() + " liked your video: " + video.getName();
                notificationService.createAndSendNotification(video.getChannel().getUser(), "LIKE", message, video);
            }
        }
    }

    @Transactional
    public Comment addComment(UUID userId, String videoId, String text, UUID parentId) {
        AppUser user = userRepo.findById(userId).orElseThrow();
        Video video = videoRepo.findById(videoId).orElseThrow();
        
        Comment parent = null;
        if (parentId != null) {
            parent = commentRepo.findById(parentId).orElse(null);
        }

        Comment comment = new Comment(null, text, user, video, parent, null, java.time.LocalDateTime.now(), null);
        Comment saved = commentRepo.save(comment);

        // Notify video owner or parent comment owner
        NotificationMessage msg = new NotificationMessage();
        msg.setType("new_comment");
        msg.setName(video.getName());
        AppUser targetUser = null;
        if (parent != null) {
            targetUser = parent.getUser();
            msg.setEmail(targetUser.getEmail());
        } else {
            targetUser = video.getChannel().getUser();
            msg.setEmail(targetUser.getEmail());
        }
        if (!user.getEmail().equals(msg.getEmail())) {
            rabbitMqService.insertMessageToQueue(msg);
            
            // In-App Notification
            String message = user.getName() + (parent != null ? " replied to your comment." : " commented on your video: " + video.getName());
            notificationService.createAndSendNotification(targetUser, "COMMENT", message, video);
        }
        
        return saved;
    }

    public Page<Comment> getVideoComments(String videoId, int page, int size) {
        return commentRepo.findByVideoIdAndParentCommentIsNullOrderByCreatedAtDesc(videoId, PageRequest.of(page, size));
    }

    public Page<Comment> getCommentReplies(UUID parentId, int page, int size) {
        return commentRepo.findByParentCommentIdOrderByCreatedAtAsc(parentId, PageRequest.of(page, size));
    }

    @Transactional
    public void incrementViewCount(String videoId, String clientIpOrUserId) {
        // Smart View Counting: Avoid blindly incrementing views on every reload
        String redisKey = "streamsphere:view:" + videoId + ":" + clientIpOrUserId;
        Boolean isNewView = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 1, TimeUnit.HOURS);
        
        if (Boolean.TRUE.equals(isNewView)) {
            Video video = videoRepo.findById(videoId).orElseThrow();
            video.setViews(video.getViews() + 1);
            videoRepo.save(video);
            
            // Channel total views update
            Channel channel = video.getChannel();
            if (channel != null) {
                channel.setTotalViews(channel.getTotalViews() + 1);
            }
        }
    }

    @Transactional
    public void reportVideo(UUID userId, String videoId, String reason) {
        AppUser user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepo.findById(videoId).orElseThrow(() -> new IllegalArgumentException("Video not found"));

        VideoReport report = new VideoReport(0, user, video, reason, "PENDING", java.time.LocalDateTime.now());
        videoReportRepo.save(report);
    }
}
