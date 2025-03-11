package com.phuonghieuto.backend.task_service.model.common.rabbitmq;

public class RabbitMQConstants {
    // Queue names
    public static final String QUEUE_TASK_NOTIFICATIONS = "task.notifications.queue";
    
    // Exchange names
    public static final String EXCHANGE_TASKS = "task.events.exchange";
    
    // Routing keys
    public static final String ROUTING_KEY_TASK_DUE_SOON = "task.due.soon";
    public static final String ROUTING_KEY_TASK_OVERDUE = "task.overdue";
}
