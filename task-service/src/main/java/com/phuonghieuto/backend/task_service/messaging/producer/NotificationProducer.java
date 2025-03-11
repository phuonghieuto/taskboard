package com.phuonghieuto.backend.task_service.messaging.producer;

import com.phuonghieuto.backend.task_service.messaging.config.RabbitMQConfig;
import com.phuonghieuto.backend.task_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.task_service.model.notification.enums.NotificationType;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTaskDueSoonNotification(TaskEntity task) {
        try {
            if (task.getAssignedUserId() == null) {
                log.info("Task {} has no assigned user, skipping notification", task.getId());
                return;
            }

            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("dueDate", task.getDueDate().toString());

            TaskNotificationDTO notification = TaskNotificationDTO.builder()
                    .type(NotificationType.TASK_DUE_SOON)
                    .taskId(task.getId())
                    .taskTitle(task.getTitle())
                    .boardId(task.getTable().getBoard().getId())
                    .boardName(task.getTable().getBoard().getName())
                    .tableId(task.getTable().getId())
                    .tableName(task.getTable().getName())
                    .recipientId(task.getAssignedUserId())
                    .dueDate(task.getDueDate())
                    .additionalData(additionalData)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_TASKS,
                    RabbitMQConfig.ROUTING_KEY_TASK_DUE_SOON,
                    notification
            );

            log.info("Sent due soon notification for task ID: {} to queue", task.getId());
        } catch (Exception e) {
            log.error("Failed to send task due soon notification: {}", e.getMessage(), e);
        }
    }

        public void sendTaskOverdueNotification(TaskEntity task) {
        try {
            if (task.getAssignedUserId() == null) {
                log.info("Task {} has no assigned user, skipping overdue notification", task.getId());
                return;
            }
    
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("dueDate", task.getDueDate().toString());
            additionalData.put("daysOverdue", 
                ChronoUnit.DAYS.between(task.getDueDate(), LocalDateTime.now()));
    
            TaskNotificationDTO notification = TaskNotificationDTO.builder()
                    .type(NotificationType.TASK_OVERDUE)
                    .taskId(task.getId())
                    .taskTitle(task.getTitle())
                    .boardId(task.getTable().getBoard().getId())
                    .boardName(task.getTable().getBoard().getName())
                    .tableId(task.getTable().getId())
                    .tableName(task.getTable().getName())
                    .recipientId(task.getAssignedUserId())
                    .dueDate(task.getDueDate())
                    .additionalData(additionalData)
                    .build();
    
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_TASKS,
                    RabbitMQConfig.ROUTING_KEY_TASK_OVERDUE,
                    notification
            );
    
            log.info("Sent overdue notification for task ID: {} to queue", task.getId());
        } catch (Exception e) {
            log.error("Failed to send task overdue notification: {}", e.getMessage(), e);
        }
    }
}