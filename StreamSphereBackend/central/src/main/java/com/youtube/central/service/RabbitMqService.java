package com.youtube.central.service;

import com.youtube.central.dto.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class RabbitMqService {

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
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }
}
