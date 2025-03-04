package com.phuonghieuto.backend.notification_service.scheduler;

import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;
import com.phuonghieuto.backend.notification_service.service.RabbitMQProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final RabbitMQProducerService rabbitMQProducerService;
    private final RestTemplate restTemplate;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *") 
    public void checkUpcomingTasks() {
        log.info("Running scheduled task reminder check at {}", LocalDateTime.now());
        
        try {
            // Call task-service to get tasks due soon (within 24 hours)
            // This is just a conceptual example - you'd need to implement the actual endpoint in task-service
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> upcomingTasks = restTemplate.getForObject(
                "http://task-service/api/tasks/upcoming", List.class);
            
            if (upcomingTasks != null) {
                for (Map<String, Object> task : upcomingTasks) {
                    String userId = (String) task.get("assigneeId");
                    String taskId = (String) task.get("id");
                    String taskTitle = (String) task.get("title");
                    
                    NotificationRequestDTO reminderNotification = NotificationRequestDTO.builder()
                        .recipientId(userId)
                        .title("Task Due Soon")
                        .message("Your task \"" + taskTitle + "\" is due soon.")
                        .type(NotificationType.TASK_DUE_SOON)
                        .referenceId(taskId)
                        .build();
                    
                    rabbitMQProducerService.sendNotification(reminderNotification);
                }
            }
        } catch (Exception e) {
            log.error("Error checking for upcoming tasks", e);
        }
    }
}