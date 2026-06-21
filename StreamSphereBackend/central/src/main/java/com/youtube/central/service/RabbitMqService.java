package com.youtube.central.service;

import com.youtube.central.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public RabbitMqService(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.name}") String exchangeName,
            @Value("${rabbitmq.routing.key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void insertMessageToQueue(NotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (RuntimeException ex) {
            // Swallowing exceptions for local/dev when RabbitMQ is not available.
            log.warn("Unable to send message to RabbitMQ (exchange={}, routingKey={}). Registration will continue without messaging. Cause: {}",
                    exchangeName, routingKey, ex.getMessage());
        }
    }
}
