package com.phuonghieuto.backend.auth_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "false", matchIfMissing = false)
public class TestRabbitMQConfig {
    
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }
    
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }
    
    // Mock all RabbitMQ queue beans
    @Bean
    @Primary
    public Queue taskNotificationsQueue() {
        return Mockito.mock(Queue.class);
    }
    
    @Bean
    @Primary
    public Queue emailConfirmationQueue() {
        return Mockito.mock(Queue.class);
    }
    
    // Mock all exchange beans
    @Bean
    @Primary
    public DirectExchange tasksExchange() {
        return Mockito.mock(DirectExchange.class);
    }
    
    @Bean
    @Primary
    public DirectExchange notificationExchange() {
        return Mockito.mock(DirectExchange.class);
    }
    
    // Mock all binding beans
    @Bean
    @Primary
    public Binding taskDueSoonBinding() {
        return Mockito.mock(Binding.class);
    }
    
    @Bean
    @Primary
    public Binding taskOverdueBinding() {
        return Mockito.mock(Binding.class);
    }
    
    @Bean
    @Primary
    public Binding boardInvitationBinding() {
        return Mockito.mock(Binding.class);
    }
    
    @Bean
    @Primary
    public Binding emailConfirmationBinding() {
        return Mockito.mock(Binding.class);
    }
}