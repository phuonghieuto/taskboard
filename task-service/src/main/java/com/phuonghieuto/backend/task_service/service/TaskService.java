package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {
    TaskResponseDTO createTask(TaskRequestDTO taskRequest);
    TaskResponseDTO getTaskById(String id);
    List<TaskResponseDTO> getAllTasksByTableId(String tableId);
    TaskResponseDTO updateTask(String id, TaskRequestDTO taskRequest);
    void deleteTask(String id);
    void reorderTasks(String tableId, List<String> taskIds);
    List<TaskResponseDTO> getAllTasksByAssignedUserId(String userId);
    public List<TaskResponseDTO> findByDueDateBetween(LocalDateTime start, LocalDateTime end);
}