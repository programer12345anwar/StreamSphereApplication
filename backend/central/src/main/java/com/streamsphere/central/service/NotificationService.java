package com.streamsphere.central.service;

import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.entity.InAppNotification;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.repository.NotificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createAndSendNotification(AppUser user, String type, String message, Video video) {
        if (user == null) return;
        
        InAppNotification notification = InAppNotification.builder()
                .user(user)
                .type(type)
                .message(message)
                .video(video)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
                
        notification = notificationRepo.save(notification);
        
        // Push to user's personal WebSocket topic
        messagingTemplate.convertAndSend("/topic/notifications/" + user.getId(), notification);
    }

    public Page<InAppNotification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public int getUnreadCount(UUID userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }
    
    @Transactional
    public void markAllAsRead(UUID userId) {
        // Optimized update
        notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
            .forEach(n -> {
                n.setRead(true);
                notificationRepo.save(n);
            });
    }
}
