package com.phuonghieuto.backend.task_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findByTableIdOrderByOrderIndexAsc(String tableId);
    List<TaskEntity> findByAssignedUserId(String assignedUserId);
    List<TaskEntity> findByDueDateBetween(LocalDateTime start, LocalDateTime end);
}