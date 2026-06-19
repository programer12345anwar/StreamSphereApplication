package com.youtube.notification_api.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


}
/*
Jackson2JsonMessageConverter → is a predefined class that converts Java objects ↔ JSON automatically.
It’s like a translator 🗣️ between your Java code and RabbitMQ:

When sending → Java object ➝ JSON ➝ RabbitMQ.

When receiving → JSON ➝ Java object.

Jackson ( is a popular JSON library).
"We configure a Jackson2JsonMessageConverter so that RabbitMQ can automatically serialize/deserialize Java objects to JSON, instead of us manually handling message conversion."
 */