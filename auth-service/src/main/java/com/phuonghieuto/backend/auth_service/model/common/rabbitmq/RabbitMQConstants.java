package com.phuonghieuto.backend.auth_service.model.common.rabbitmq;

public class RabbitMQConstants {
    // Queue names
    public static final String QUEUE_TASK_NOTIFICATIONS = "task.notifications.queue";
    
    // Exchange names
    public static final String EXCHANGE_TASKS = "task.events.exchange";
    
    // Routing keys
    public static final String ROUTING_KEY_TASK_DUE_SOON = "task.due.soon";
    public static final String ROUTING_KEY_TASK_OVERDUE = "task.overdue";

    public static final String ROUTING_KEY_BOARD_INVITATION = "board.invitation";

    public static final String EXCHANGE_NOTIFICATION = "notification.exchange";
    public static final String QUEUE_EMAIL_CONFIRMATION = "email.confirmation.queue";
    public static final String ROUTING_KEY_EMAIL_CONFIRMATION = "email.confirmation";
    
}
