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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final NotificationProducerService notificationProducerService;

    @Scheduled(cron = "${task.reminder.schedule:*/30 * * * * *}")
    @Transactional
    public void checkForDueSoonTasks() {
        log.info("Running scheduled check for due soon tasks");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(24); // Look for tasks due in next 24 hours

        List<TaskEntity> dueSoonTasks = taskRepository.findByDueDateBetweenAndReminderSent(now, threshold, false);
        log.info("Found {} tasks due soon", dueSoonTasks.size());

        for (TaskEntity task : dueSoonTasks) {
            try {
                notificationProducerService.sendTaskDueSoonNotification(task);

                task.setReminderSent(true);
                taskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to send notification for task {}: {}", task.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${task.overdue.schedule:*/30 * * * * *}")
    @Transactional
    public void checkForOverdueTasks() {
        log.info("Running scheduled check for overdue tasks");

        LocalDateTime now = LocalDateTime.now();

        // Find tasks that are overdue but no overdue notification sent
        List<TaskEntity> overdueTasks = taskRepository.findByDueDateLessThanAndStatusNotAndOverdueNotificationSent(now,
                TaskStatus.COMPLETED, false);

        log.info("Found {} overdue tasks that need notifications", overdueTasks.size());

        for (TaskEntity task : overdueTasks) {
            try {
                notificationProducerService.sendTaskOverdueNotification(task);

                // Mark as notification sent and update status to OVERDUE
                task.setOverdueNotificationSent(true);
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to send overdue notification for task {}: {}", task.getId(), e.getMessage());
            }
        }
    }
}