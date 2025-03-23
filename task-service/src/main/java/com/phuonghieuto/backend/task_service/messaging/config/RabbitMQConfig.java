package com.phuonghieuto.backend.task_service.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.phuonghieuto.backend.task_service.model.common.rabbitmq.RabbitMQConstants;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_TASK_NOTIFICATIONS = RabbitMQConstants.QUEUE_TASK_NOTIFICATIONS;
    public static final String EXCHANGE_TASKS = RabbitMQConstants.EXCHANGE_TASKS;
    public static final String ROUTING_KEY_TASK_DUE_SOON = RabbitMQConstants.ROUTING_KEY_TASK_DUE_SOON;
    public static final String ROUTING_KEY_TASK_OVERDUE = RabbitMQConstants.ROUTING_KEY_TASK_OVERDUE;
    public static final String ROUTING_KEY_BOARD_INVITATION = RabbitMQConstants.ROUTING_KEY_BOARD_INVITATION;

    @Bean
    public Queue taskNotificationsQueue() {
        return new Queue(QUEUE_TASK_NOTIFICATIONS, true);
    }

    @Bean
    public DirectExchange tasksExchange() {
        return new DirectExchange(EXCHANGE_TASKS);
    }

    @Bean
    public Binding taskDueSoonBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with(ROUTING_KEY_TASK_DUE_SOON);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Binding taskOverdueBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with(ROUTING_KEY_TASK_OVERDUE);
    }

    @Bean
    public Binding boardInvitationBinding(Queue taskNotificationsQueue, DirectExchange tasksExchange) {
        return BindingBuilder.bind(taskNotificationsQueue).to(tasksExchange).with(ROUTING_KEY_BOARD_INVITATION);
    }
}