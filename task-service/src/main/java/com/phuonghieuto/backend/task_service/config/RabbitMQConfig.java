package com.phuonghieuto.backend.task_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_TASK_NOTIFICATIONS = "task.notifications.queue";
    public static final String EXCHANGE_TASKS = "task.events.exchange";
    public static final String ROUTING_KEY_TASK_DUE_SOON = "task.due.soon";

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
        return BindingBuilder.bind(taskNotificationsQueue)
                .to(tasksExchange)
                .with(ROUTING_KEY_TASK_DUE_SOON);
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
}