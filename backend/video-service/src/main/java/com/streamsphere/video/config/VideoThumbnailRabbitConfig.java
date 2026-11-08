package com.streamsphere.video.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class VideoThumbnailRabbitConfig {
    @Value("${thumbnail.events.exchange}")
    private String exchangeName;
    @Value("${thumbnail.events.request-queue}")
    private String requestQueueName;
    @Value("${thumbnail.events.request-routing-key}")
    private String requestRoutingKey;
    @Value("${thumbnail.events.result-queue}")
    private String resultQueueName;
    @Value("${thumbnail.events.result-routing-key}")
    private String resultRoutingKey;
    @Value("${thumbnail.events.dlx}")
    private String deadLetterExchangeName;

    @Bean("videoThumbnailExchange")
    public DirectExchange thumbnailExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean("videoThumbnailDeadLetterExchange")
    public DirectExchange thumbnailDeadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName);
    }

    @Bean("videoThumbnailRequestQueue")
    public Queue thumbnailRequestQueue() {
        return thumbnailQueue(requestQueueName, requestRoutingKey + ".dlq");
    }

    @Bean("videoThumbnailResultQueue")
    public Queue thumbnailResultQueue() {
        return thumbnailQueue(resultQueueName, resultRoutingKey + ".dlq");
    }

    @Bean("videoThumbnailRequestDeadLetterQueue")
    public Queue thumbnailRequestDeadLetterQueue() {
        return QueueBuilder.durable(requestQueueName + ".dlq").build();
    }

    @Bean("videoThumbnailResultDeadLetterQueue")
    public Queue thumbnailResultDeadLetterQueue() {
        return QueueBuilder.durable(resultQueueName + ".dlq").build();
    }

    @Bean
    public Binding bindVideoThumbnailRequests(
            @Qualifier("videoThumbnailRequestQueue") Queue queue,
            @Qualifier("videoThumbnailExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(requestRoutingKey);
    }

    @Bean
    public Binding bindVideoThumbnailResults(
            @Qualifier("videoThumbnailResultQueue") Queue queue,
            @Qualifier("videoThumbnailExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(resultRoutingKey);
    }

    @Bean
    public Binding bindVideoThumbnailRequestDeadLetters(
            @Qualifier("videoThumbnailRequestDeadLetterQueue") Queue queue,
            @Qualifier("videoThumbnailDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(requestRoutingKey + ".dlq");
    }

    @Bean
    public Binding bindVideoThumbnailResultDeadLetters(
            @Qualifier("videoThumbnailResultDeadLetterQueue") Queue queue,
            @Qualifier("videoThumbnailDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(resultRoutingKey + ".dlq");
    }

    private Queue thumbnailQueue(String queueName, String deadLetterRoutingKey) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", deadLetterExchangeName);
        arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);
        return QueueBuilder.durable(queueName).withArguments(arguments).build();
    }
}
