package com.phuonghieuto.backend.notification_service.service;

import com.phuonghieuto.backend.notification_service.config.RabbitMQConfig;
import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumerService {

    private final NotificationService notificationService;

        @RabbitListener(queues = RabbitMQConfig.QUEUE_TASK_NOTIFICATIONS)
    public void receiveTaskNotification(TaskNotificationDTO taskNotification) {
        log.info("Received task notification: {}", taskNotification);
        
        try {
            if (NotificationType.TASK_DUE_SOON.name().equals(taskNotification.getType())) {
                notificationService.createTaskDueSoonNotification(taskNotification);
                log.info("Successfully processed task due soon notification for task: {}", taskNotification.getTaskId());
            } else if (NotificationType.TASK_OVERDUE.name().equals(taskNotification.getType())) {
                notificationService.createTaskOverdueNotification(taskNotification);
                log.info("Successfully processed task overdue notification for task: {}", taskNotification.getTaskId());
            } else {
                log.warn("Unknown task notification type: {}", taskNotification.getType());
            }
        } catch (Exception e) {
            log.error("Error processing task notification: {}", e.getMessage(), e);
        }
    }
    
}