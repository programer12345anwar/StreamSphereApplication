package com.streamsphere.central.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

class RabbitMQConfigTest {

    @Test
    void connectionFactoryUsesConfiguredPortVirtualHostAndTls() throws Exception {
        CachingConnectionFactory connectionFactory = new RabbitMQConfig()
                .getConnectionFactory("rabbit.example.com", 5671, "app-user", "secret", "app-vhost", true);

        assertEquals(5671, connectionFactory.getRabbitConnectionFactory().getPort());
        assertEquals("app-vhost", connectionFactory.getRabbitConnectionFactory().getVirtualHost());
        assertTrue(connectionFactory.getRabbitConnectionFactory().isSSL());
        connectionFactory.destroy();
    }
}
