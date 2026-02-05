package com.youtube.central.configuration;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    @Bean
    public DirectExchange getDirectExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue getMessagingQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public CachingConnectionFactory getConnectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate getRabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Binding bindQueueWithExchange(DirectExchange exchange, Queue queue) {
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
