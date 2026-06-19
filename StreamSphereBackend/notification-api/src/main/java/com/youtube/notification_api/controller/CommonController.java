package com.youtube.notification_api.controller;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import com.youtube.notification_api.dto.NotificationMessage;
import com.youtube.notification_api.enums.NotificationType;
import com.youtube.notification_api.service.CommonUserService;

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


        @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbitmq.queue.name}", durable = "true"),
            exchange = @Exchange(value = "${rabbitmq.exchange.name}", type = ExchangeTypes.DIRECT, durable = "true"),
            key = "${rabbitmq.routing.key}"
        ))
    public void consumeMessage(@Payload NotificationMessage message) throws Exception {
        log.info("Notification Message received successfully payload = {}", message.toString());
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
            }
        } catch (Exception e) {
            log.error("Error while processing notification: {}", e.getMessage());
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
