package com.streamsphere.central.controller;

import com.streamsphere.central.entity.InAppNotification;
import com.streamsphere.central.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/central/notifications")
@CrossOrigin(origins = "*") // Allows API Gateway or React frontend
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public Page<InAppNotification> getNotifications(@RequestParam("userId") UUID userId,
                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return notificationService.getUserNotifications(userId, PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public int getUnreadCount(@RequestParam("userId") UUID userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable("id") UUID id) {
        notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    public void markAllAsRead(@RequestParam("userId") UUID userId) {
        notificationService.markAllAsRead(userId);
    }
}
