package com.streamsphere.central.service;

import com.streamsphere.central.entity.*;
import com.streamsphere.central.repository.*;
import com.streamsphere.central.event.model.NotificationMessage;
import com.streamsphere.central.event.producer.RabbitMqService;
import com.streamsphere.central.dto.response.CommentResponseDTO;
import com.streamsphere.central.dto.response.ReportSubmissionResponseDTO;
import com.streamsphere.central.dto.response.CommentLikeResponseDTO;
import com.streamsphere.central.dto.response.VideoEngagementResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
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
    private final CommentLikeRepo commentLikeRepo;

    public EngagementService(VideoLikeRepo videoLikeRepo, CommentRepo commentRepo, 
                             VideoRepo videoRepo, AppUserRepo userRepo, 
                             RabbitMqService rabbitMqService, StringRedisTemplate redisTemplate,
                             VideoReportRepo videoReportRepo, NotificationService notificationService,
                             CommentLikeRepo commentLikeRepo) {
        this.videoLikeRepo = videoLikeRepo;
        this.commentRepo = commentRepo;
        this.videoRepo = videoRepo;
        this.userRepo = userRepo;
        this.rabbitMqService = rabbitMqService;
        this.redisTemplate = redisTemplate;
        this.videoReportRepo = videoReportRepo;
        this.notificationService = notificationService;
        this.commentLikeRepo = commentLikeRepo;
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
    public CommentResponseDTO addComment(UUID userId, String videoId, String text, UUID parentId) {
        if (text == null || text.isBlank() || text.length() > 2000) {
            throw new IllegalArgumentException("Comment text must contain between 1 and 2000 characters.");
        }
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
        
        return toCommentResponse(saved, 0, false);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getVideoComments(String videoId, int page, int size, String currentUserEmail) {
        UUID currentUserId = findCurrentUserId(currentUserEmail);
        return mapComments(
                commentRepo.findByVideoIdAndParentCommentIsNullOrderByCreatedAtDesc(videoId, PageRequest.of(page, size)),
                currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getCommentReplies(UUID parentId, int page, int size, String currentUserEmail) {
        UUID currentUserId = findCurrentUserId(currentUserEmail);
        return mapComments(
                commentRepo.findByParentCommentIdOrderByCreatedAtAsc(parentId, PageRequest.of(page, size)),
                currentUserId);
    }

    private Page<CommentResponseDTO> mapComments(Page<Comment> commentPage, UUID currentUserId) {
        if (commentPage.isEmpty()) {
            return new PageImpl<>(java.util.List.of(), commentPage.getPageable(), commentPage.getTotalElements());
        }
        java.util.List<UUID> commentIds = commentPage.getContent().stream().map(Comment::getId).toList();
        java.util.Map<UUID, Long> likeCounts = commentLikeRepo.findLikeCounts(commentIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CommentLikeRepo.LikeCount::getCommentId,
                        CommentLikeRepo.LikeCount::getLikeCount));
        java.util.Set<UUID> likedCommentIds = currentUserId == null
                ? java.util.Set.of()
                : new java.util.HashSet<>(commentLikeRepo.findLikedCommentIds(currentUserId, commentIds));
        return commentPage.map(comment -> toCommentResponse(
                comment,
                likeCounts.getOrDefault(comment.getId(), 0L),
                likedCommentIds.contains(comment.getId())));
    }

    private CommentResponseDTO toCommentResponse(Comment comment, long likeCount, boolean likedByCurrentUser) {
        return new CommentResponseDTO(comment.getId(), comment.getText(), comment.getUser().getName(),
                comment.getCreatedAt(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                likeCount, likedByCurrentUser);
    }

    @Transactional(readOnly = true)
    public VideoEngagementResponseDTO getVideoEngagement(String videoId, String currentUserEmail) {
        if (!videoRepo.existsById(videoId)) {
            throw new IllegalArgumentException("Video not found.");
        }
        UUID currentUserId = findCurrentUserId(currentUserEmail);
        boolean liked = currentUserId != null
                && videoLikeRepo.findByUserIdAndVideoId(currentUserId, videoId).map(VideoLike::isLike).orElse(false);
        return new VideoEngagementResponseDTO(
                videoLikeRepo.countByVideoIdAndIsLikeTrue(videoId),
                commentRepo.countByVideoIdAndParentCommentIsNull(videoId),
                liked);
    }

    @Transactional
    public CommentLikeResponseDTO toggleCommentLike(UUID userId, UUID commentId) {
        AppUser user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));
        Optional<CommentLike> existing = commentLikeRepo.findByUserIdAndCommentId(userId, commentId);
        if (existing.isPresent()) {
            commentLikeRepo.delete(existing.get());
            commentLikeRepo.flush();
            return new CommentLikeResponseDTO(commentLikeRepo.countByCommentId(commentId), false);
        }
        commentLikeRepo.saveAndFlush(new CommentLike(user, comment));
        return new CommentLikeResponseDTO(commentLikeRepo.countByCommentId(commentId), true);
    }

    public UUID getAuthenticatedUserId(String email, UUID requestedUserId) {
        AppUser user = userRepo.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user was not found.");
        }
        if (requestedUserId != null && !requestedUserId.equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User identity does not match the authenticated account.");
        }
        return user.getId();
    }

    private UUID findCurrentUserId(String email) {
        if (email == null || email.isBlank()) return null;
        AppUser user = userRepo.findByEmail(email);
        return user == null ? null : user.getId();
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
    public ReportSubmissionResponseDTO reportVideo(String reporterEmail, String videoId, String reason, String description) {
        if (reason == null || reason.isBlank() || reason.length() > 200) {
            throw new IllegalArgumentException("Choose a report reason no longer than 200 characters.");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Report details must be 2000 characters or fewer.");
        }
        AppUser user = userRepo.findByEmail(reporterEmail);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user was not found.");
        }
        Video video = videoRepo.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found."));

        VideoReport report = new VideoReport();
        report.setUser(user);
        report.setVideo(video);
        report.setReason(reason.trim());
        report.setDescription(description == null || description.isBlank() ? null : description.trim());
        report.setStatus("PENDING");
        report.setCreatedAt(java.time.LocalDateTime.now());
        VideoReport persistedReport = videoReportRepo.saveAndFlush(report);
        return new ReportSubmissionResponseDTO(
                persistedReport.getId(),
                persistedReport.getStatus(),
                persistedReport.getCreatedAt());
    }
}
