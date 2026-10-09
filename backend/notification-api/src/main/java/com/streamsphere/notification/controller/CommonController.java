package com.streamsphere.notification.controller;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import com.streamsphere.notification.event.model.NotificationMessage;
import com.streamsphere.notification.event.model.NotificationType;
import com.streamsphere.notification.service.CommonUserService;
import com.streamsphere.notification.service.DuplicateMessageGuard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification API", description = "APIs for consuming and testing notifications")
@Slf4j
public class CommonController {

    @Autowired
    CommonUserService commonUserService;

    @Autowired
    DuplicateMessageGuard duplicateMessageGuard;


        @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeMessage(@Payload NotificationMessage message) throws Exception {
        log.info("Notification Message received successfully payload = {}", message.toString());
        if (message.getType() == null || message.getEmail() == null) {
            log.warn("Dropping malformed notification message: {}", message);
            return;
        }
        if (duplicateMessageGuard.isDuplicate(message)) {
            log.warn("Skipping duplicate notification message type={} email={}", message.getType(), message.getEmail());
            return;
        }
        try {
            if (message.getType().equals(NotificationType.user_registration.toString())) {
                log.info("Calling common user service to registration mail");
                commonUserService.sendUserRegistrationEmail(message);
            } else if (message.getType().equals(NotificationType.subscriber_added.toString())) {
                log.info("Message type is subscriber_added");
                commonUserService.sendSubscriberAddedMail(message);
            } else if (message.getType().equals(NotificationType.create_channel.toString())) {
                log.info("CommonController: Type of notification is create_channel");
                commonUserService.sendCreateChannelNotification(message);
            } else if (message.getType().equals(NotificationType.new_video.toString())) {
                log.info("Got type of message as new_video");
                commonUserService.notifyNewVideoUploadedToSubscriber(message);
            } else if (message.getType().equals(NotificationType.video_liked.toString())) {
                log.info("Got type of message as video_liked");
                commonUserService.sendVideoLikedMail(message);
            } else if (message.getType().equals(NotificationType.new_comment.toString())) {
                log.info("Got type of message as new_comment");
                commonUserService.sendNewCommentMail(message);
            } else if (message.getType().equals(NotificationType.video_processed.toString())) {
                log.info("Got type of message as video_processed");
                commonUserService.sendVideoProcessedMail(message);
            } else {
                log.warn("Unknown notification type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Failed to send notification email (likely due to missing SMTP config). Gracefully logging instead of crashing: {}", e.getMessage());
            // Intentionally not re-throwing to avoid infinite RabbitMQ requeue loops without DLQ
        }
    }

    // Added REST endpoint so that Swagger shows this API
    @PostMapping("/test")
    @Operation(
            summary = "Test notification message",
            description = "Send a sample notification message manually (for debugging via Swagger UI)"
    )
    public String testNotification(@RequestBody NotificationMessage message) {
        log.info("Received test notification via REST: {}", message.toString());
        return "Notification received for testing: " + message.getType();
    }
}

