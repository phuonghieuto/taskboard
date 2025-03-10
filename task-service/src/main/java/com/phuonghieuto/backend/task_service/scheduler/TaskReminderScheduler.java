package com.phuonghieuto.backend.task_service.scheduler;

import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.NotificationProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final NotificationProducerService notificationProducerService;

    @Scheduled(cron = "${task.reminder.schedule:0 0 * * * *}")
    @Transactional
    public void checkForDueSoonTasks() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[{}] Running scheduled check for due soon tasks", timestamp);

        LocalDateTime threshold = now.plusHours(24); // Look for tasks due in next 24 hours

        List<TaskEntity> dueSoonTasks = taskRepository.findByDueDateBetweenAndReminderSent(now, threshold, false);
        log.info("[{}] Found {} tasks due soon", timestamp, dueSoonTasks.size());

        for (TaskEntity task : dueSoonTasks) {
            try {
                notificationProducerService.sendTaskDueSoonNotification(task);

                task.setReminderSent(true);
                taskRepository.save(task);
                log.info("[{}] Sent due soon notification for task ID: {}", timestamp, task.getId());
            } catch (Exception e) {
                log.error("[{}] Failed to send notification for task {}: {}", timestamp, task.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${task.overdue.schedule:0 0 * * * *}")
    @Transactional
    public void checkForOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("[{}] Running scheduled check for overdue tasks", timestamp);

        // Find tasks that are overdue but no overdue notification sent
        List<TaskEntity> overdueTasks = taskRepository.findByDueDateBeforeAndStatusAndOverdueNotificationSentFalse(
                now, TaskStatus.TODO);

        log.info("[{}] Found {} overdue tasks that need notifications", timestamp, overdueTasks.size());

        for (TaskEntity task : overdueTasks) {
            try {
                // IMPORTANT: Set these flags BEFORE sending notification and saving
                task.setOverdueNotificationSent(true);
                task.setStatus(TaskStatus.OVERDUE);
                
                // Save the task with updated flags
                taskRepository.save(task);
                
                // Send notification after saving to ensure flags are persisted
                // even if notification sending fails
                notificationProducerService.sendTaskOverdueNotification(task);
                
                log.info("[{}] Sent overdue notification for task ID: {}", timestamp, task.getId());
            } catch (Exception e) {
                log.error("[{}] Failed to send overdue notification for task {}: {}", timestamp, task.getId(), e.getMessage());
            }
        }
    }
}