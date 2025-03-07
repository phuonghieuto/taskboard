package com.phuonghieuto.backend.task_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findByTableIdOrderByOrderIndexAsc(String tableId);
    List<TaskEntity> findByAssignedUserId(String userId);
    List<TaskEntity> findByDueDateBetweenAndReminderSent(LocalDateTime start, LocalDateTime end, boolean reminderSent);
    List<TaskEntity> findByDueDateBetween(LocalDateTime start, LocalDateTime end);
    List<TaskEntity> findByDueDateBeforeAndStatusNot(LocalDateTime dateTime, TaskStatus status);
    List<TaskEntity> findByStatus(TaskStatus status);
    long countByAssignedUserIdAndStatus(String userId, TaskStatus status);
}