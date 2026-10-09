package com.streamsphere.central.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminRealtimeService {

    private final AdminService adminService;
    private final SimpMessagingTemplate messagingTemplate;

    public AdminRealtimeService(AdminService adminService, SimpMessagingTemplate messagingTemplate) {
        this.adminService = adminService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void broadcastAnalytics() {
        try {
            Map<String, Long> analytics = adminService.getAnalytics();
            messagingTemplate.convertAndSend("/topic/admin/analytics", analytics);
        } catch (Exception e) {
            // Ignore if DB isn't ready
        }
    }
}
