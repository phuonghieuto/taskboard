package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.config.RabbitMQConfig;
import com.phuonghieuto.backend.task_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.task_service.model.notification.enums.NotificationType;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducerService {

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
}