package com.phuonghieuto.backend.task_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@Configuration
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
        Queue mockQueue = Mockito.mock(Queue.class);
        Mockito.when(mockQueue.getName()).thenReturn("mock.task.notifications.queue");
        return mockQueue;
    }
    
    @Bean
    @Primary
    public Queue emailConfirmationQueue() {
        Queue mockQueue = Mockito.mock(Queue.class);
        Mockito.when(mockQueue.getName()).thenReturn("mock.email.confirmation.queue");
        return mockQueue;
    }
    
    // Mock all exchange beans
    @Bean
    @Primary
    public DirectExchange tasksExchange() {
        DirectExchange mockExchange = Mockito.mock(DirectExchange.class);
        Mockito.when(mockExchange.getName()).thenReturn("mock.tasks.exchange");
        return mockExchange;
    }
    
    @Bean
    @Primary
    public DirectExchange notificationExchange() {
        DirectExchange mockExchange = Mockito.mock(DirectExchange.class);
        Mockito.when(mockExchange.getName()).thenReturn("mock.notification.exchange");
        return mockExchange;
    }
    
    // Mock all binding beans with proper parameters to prevent the null destination error
    @Bean
    @Primary
    public Binding taskDueSoonBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with("task.due.soon");
    }
    
    @Bean
    @Primary
    public Binding taskOverdueBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with("task.overdue");
    }
    
    @Bean
    @Primary
    public Binding boardInvitationBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with("board.invitation");
    }
    
    @Bean
    @Primary
    public Binding emailConfirmationBinding(Queue emailConfirmationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(emailConfirmationQueue).to(notificationExchange).with("email.confirmation");
    }
    
    @Bean
    @Primary
    public MessageConverter jsonMessageConverter() {
        return Mockito.mock(MessageConverter.class);
    }
}