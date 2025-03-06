package com.phuonghieuto.backend.notification_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.phuonghieuto.backend.notification_service.client.TaskServiceClient;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;
import com.phuonghieuto.backend.notification_service.service.RabbitMQProducerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final RabbitMQProducerService rabbitMQProducerService;
    private final TaskServiceClient taskServiceClient;
    
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void checkUpcomingTasks() {
        try {
            log.info("Checking for upcoming tasks...");
            List<Map<String, Object>> upcomingTasks = taskServiceClient.getUpcomingTasks();
            
            log.info("Found {} upcoming tasks to send reminders for", upcomingTasks.size());
            
            for (Map<String, Object> task : upcomingTasks) {
                String userId = (String) task.get("userId");
                String taskId = (String) task.get("id");
                String title = (String) task.get("title");
                
                NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .recipientId(userId)
                    .title("Task Reminder")
                    .message("Your task '" + title + "' is due soon")
                    .type(NotificationType.TASK_DUE_SOON)
                    .referenceId(taskId)
                    .build();
                
                rabbitMQProducerService.sendNotification(notification);
            }
        } catch (Exception e) {
            log.error("Error checking for upcoming tasks", e);
        }
    }
}