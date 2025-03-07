package com.phuonghieuto.backend.task_service.scheduler;

import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
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

    @Scheduled(cron = "${task.reminder.schedule:*/30 * * * * *}") // Every hour by default
    @Transactional
    public void checkForDueSoonTasks() {
        log.info("Running scheduled check for due soon tasks");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(24); // Look for tasks due in next 24 hours

        List<TaskEntity> dueSoonTasks = taskRepository.findByDueDateBetweenAndReminderSent(now, threshold, false);
        log.info("Found {} tasks due soon", dueSoonTasks.size());

        for (TaskEntity task : dueSoonTasks) {
            notificationProducerService.sendTaskDueSoonNotification(task);
            
            // Mark reminder as sent
            task.setReminderSent(true);
            taskRepository.save(task);
        }
    }
}