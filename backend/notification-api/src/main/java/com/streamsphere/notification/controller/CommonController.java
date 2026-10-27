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
        if (message == null || message.getType() == null || message.getEmail() == null) {
            log.warn("Dropping malformed notification message");
            return;
        }
        if (duplicateMessageGuard.isDuplicate(message)) {
            log.warn("Skipping duplicate notification message type={}", message.getType());
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
            duplicateMessageGuard.forget(message);
            log.error("Failed to process notification type {}; it will be retried and dead-lettered after configured attempts",
                    message.getType(), e);
            throw e;
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
