package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TaskService {
    TaskResponseDTO createTask(TaskRequestDTO taskRequest);

    TaskResponseDTO getTaskById(String id);

    List<TaskResponseDTO> getAllTasksByTableId(String tableId);

    TaskResponseDTO updateTask(String id, TaskRequestDTO taskRequest);

    TaskResponseDTO updateTaskStatus(String id, TaskStatus newStatus);

    void deleteTask(String id);

    void reorderTasks(String tableId, List<String> taskIds);

    List<TaskResponseDTO> getAllTasksByAssignedUserId(String userId);

    List<TaskResponseDTO> getAllTasksByStatus(TaskStatus status);

    List<TaskResponseDTO> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    Map<TaskStatus, Long> getTaskStatistics(String userId);
}