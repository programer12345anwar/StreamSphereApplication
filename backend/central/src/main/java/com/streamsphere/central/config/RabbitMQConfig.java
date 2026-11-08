package com.streamsphere.central.config;


import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;
    @Value("${thumbnail.events.exchange}")
    private String thumbnailExchangeName;
    @Value("${thumbnail.events.request-queue}")
    private String thumbnailRequestQueueName;
    @Value("${thumbnail.events.request-routing-key}")
    private String thumbnailRequestRoutingKey;
    @Value("${thumbnail.events.result-queue}")
    private String thumbnailResultQueueName;
    @Value("${thumbnail.events.result-routing-key}")
    private String thumbnailResultRoutingKey;
    @Value("${thumbnail.events.dlx}")
    private String thumbnailDeadLetterExchangeName;

    @Bean
    public DirectExchange getDirectExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue getMessagingQueue() {
        // Must match the consumer-side declaration (notification-api RabbitConfig),
        // including the dead-letter arguments, or the broker will reject redeclaration.
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", exchangeName + ".dlx");
        args.put("x-dead-letter-routing-key", routingKey + ".dlq");
        return QueueBuilder.durable(queueName).withArguments(args).build();
    }

    @Bean("thumbnailExchange")
    public DirectExchange thumbnailExchange() {
        return new DirectExchange(thumbnailExchangeName);
    }

    @Bean("thumbnailDeadLetterExchange")
    public DirectExchange thumbnailDeadLetterExchange() {
        return new DirectExchange(thumbnailDeadLetterExchangeName);
    }

    @Bean("thumbnailRequestQueue")
    public Queue thumbnailRequestQueue() {
        return thumbnailQueue(thumbnailRequestQueueName, thumbnailRequestRoutingKey + ".dlq");
    }

    @Bean("thumbnailResultQueue")
    public Queue thumbnailResultQueue() {
        return thumbnailQueue(thumbnailResultQueueName, thumbnailResultRoutingKey + ".dlq");
    }

    @Bean("thumbnailRequestDeadLetterQueue")
    public Queue thumbnailRequestDeadLetterQueue() {
        return QueueBuilder.durable(thumbnailRequestQueueName + ".dlq").build();
    }

    @Bean("thumbnailResultDeadLetterQueue")
    public Queue thumbnailResultDeadLetterQueue() {
        return QueueBuilder.durable(thumbnailResultQueueName + ".dlq").build();
    }

    @Bean
    public Binding bindThumbnailRequests(
            @Qualifier("thumbnailRequestQueue") Queue queue,
            @Qualifier("thumbnailExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(thumbnailRequestRoutingKey);
    }

    @Bean
    public Binding bindThumbnailResults(
            @Qualifier("thumbnailResultQueue") Queue queue,
            @Qualifier("thumbnailExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(thumbnailResultRoutingKey);
    }

    @Bean
    public Binding bindThumbnailRequestDeadLetters(
            @Qualifier("thumbnailRequestDeadLetterQueue") Queue queue,
            @Qualifier("thumbnailDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(thumbnailRequestRoutingKey + ".dlq");
    }

    @Bean
    public Binding bindThumbnailResultDeadLetters(
            @Qualifier("thumbnailResultDeadLetterQueue") Queue queue,
            @Qualifier("thumbnailDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(thumbnailResultRoutingKey + ".dlq");
    }

    private Queue thumbnailQueue(String queueName, String deadLetterRoutingKey) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", thumbnailDeadLetterExchangeName);
        arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);
        return QueueBuilder.durable(queueName).withArguments(arguments).build();
    }

    @Bean
    public CachingConnectionFactory getConnectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") int port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password,
            @Value("${spring.rabbitmq.virtual-host:/}") String virtualHost,
            @Value("${spring.rabbitmq.ssl.enabled:false}") boolean sslEnabled)
            throws NoSuchAlgorithmException {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        if (sslEnabled) {
            connectionFactory.getRabbitConnectionFactory().useSslProtocol(SSLContext.getDefault());
            connectionFactory.getRabbitConnectionFactory().enableHostnameVerification();
        }
        return connectionFactory;
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate getRabbitTemplate(
            CachingConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean("notificationBinding")
    public Binding bindQueueWithExchange(
            @Qualifier("getDirectExchange") DirectExchange exchange,
            @Qualifier("getMessagingQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}



   
    

 /*

     * for explaining @Bean
     * @Autowired
     * RestTemplate restTemplate;//spring boot will not able to provide the object for the restTemplate b'z RestTemplate doesn't have @Component,@Service, @Repository, @Controller,so you need to configure it.
     * similarly
     * @Autowired
     * ArrayList<> arrayList;//similarly also ArrayList doesn't have @Component,@Service, @Repository, @Controller,so you need to configure it.
     * solution: if you want bean in ioc cntainer then use @Bean
     * example:
     * @Bean
     * public RestTemplate restTemplate() {
     *     return new RestTemplate();
     * }
     * @Bean
     * public ArrayList<String> arrayList() {
     *     return new ArrayList<>();
     * }
     */
