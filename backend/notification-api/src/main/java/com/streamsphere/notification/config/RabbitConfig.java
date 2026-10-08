package com.streamsphere.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange.name:notification-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name:notification-queue}")
    private String queueName;

    @Value("${rabbitmq.routing.key:notification-123}")
    private String routingKey;

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(exchangeName + ".dlx", true, false);
    }

    @Bean
    public Queue notificationQueue() {
        // Failed messages are dead-lettered instead of being lost or hot-looping the broker.
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", exchangeName + ".dlx");
        args.put("x-dead-letter-routing-key", routingKey + ".dlq");
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return new Queue(queueName + ".dlq", true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(routingKey);
    }

    @Bean
    public Binding notificationDeadLetterBinding(Queue notificationDeadLetterQueue, DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue).to(notificationDeadLetterExchange).with(routingKey + ".dlq");
    }

}

