package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskEntityToTaskResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskRequestToTaskEntityMapper;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.TaskService;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskRequestToTaskEntityMapper taskRequestToTaskEntityMapper = TaskRequestToTaskEntityMapper
            .initialize();
    private final TaskEntityToTaskResponseMapper taskEntityToTaskResponseMapper = TaskEntityToTaskResponseMapper
            .initialize();
    private final EntityAccessControlService accessControlService;
    private final AuthUtils authUtils;

    @Override
    @CacheEvict(value = { "tasks", "tasksByTable", "tasksByUser", "tasksByStatus", "upcomingTasks",
            "taskStatistics" }, allEntries = true)
    public TaskResponseDTO createTask(TaskRequestDTO taskRequest) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if table exists and user has access to it
        TableEntity table = accessControlService.findTableAndCheckAccess(taskRequest.getTableId(), currentUserId);

        // Determine the order index if not specified
        if (taskRequest.getOrderIndex() <= 0) {
            List<TaskEntity> existingTasks = taskRepository.findByTableIdOrderByOrderIndexAsc(table.getId());
            taskRequest.setOrderIndex(existingTasks.isEmpty() ? 1 : existingTasks.size() + 1);
        }

        // Create and save the task
        TaskEntity taskEntity = taskRequestToTaskEntityMapper.mapForCreation(taskRequest, table);
        TaskEntity savedTask = taskRepository.save(taskEntity);

        log.info("Created new task with ID: {} for table: {}", savedTask.getId(), table.getId());
        return taskEntityToTaskResponseMapper.map(savedTask);
    }

    @Override
    @Cacheable(value = "tasks", key = "#id")
    public TaskResponseDTO getTaskById(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity taskEntity = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        return taskEntityToTaskResponseMapper.map(taskEntity);
    }

    @Override
    @Cacheable(value = "tasksByTable", key = "#tableId")
    public List<TaskResponseDTO> getAllTasksByTableId(String tableId) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if table exists and user has access to it
        accessControlService.findTableAndCheckAccess(tableId, currentUserId);

        // Get all tasks for the table, ordered by orderIndex
        List<TaskEntity> tasks = taskRepository.findByTableIdOrderByOrderIndexAsc(tableId);

        return tasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "tasksByUser", key = "#userId")
    public List<TaskResponseDTO> getAllTasksByAssignedUserId(String userId) {
        String currentUserId = authUtils.getCurrentUserId();

        // If requesting tasks for another user, verify current user has admin rights
        if (!currentUserId.equals(userId)) {
            // You could add admin check here if needed
            throw new UnauthorizedAccessException("You can only view your own tasks");
        }

        // Get all tasks assigned to the user
        List<TaskEntity> tasks = taskRepository.findByAssignedUserId(userId);

        // For each task, filter to only include those the user has access to
        List<TaskEntity> accessibleTasks = tasks.stream().filter(task -> hasAccessToTaskBoard(task, currentUserId))
                .collect(Collectors.toList());

        return accessibleTasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "tasks", key = "#id"), @CacheEvict(value = { "tasksByTable", "tasksByUser",
            "tasksByStatus", "upcomingTasks", "taskStatistics" }, allEntries = true) })
    public TaskResponseDTO updateTask(String id, TaskRequestDTO taskRequest) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity existingTask = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        // Update task properties
        existingTask.setTitle(taskRequest.getTitle());
        existingTask.setDescription(taskRequest.getDescription());

        // If assignee has changed
        if (taskRequest.getAssignedUserId() != null) {
            existingTask.setAssignedUserId(taskRequest.getAssignedUserId());
        }

        // If table ID has changed (task moved to another table)
        if (!existingTask.getTable().getId().equals(taskRequest.getTableId())) {
            TableEntity newTable = accessControlService.findTableAndCheckAccess(taskRequest.getTableId(),
                    currentUserId);
            existingTask.setTable(newTable);
        }

        // Update order index if it has changed
        if (taskRequest.getOrderIndex() > 0 && taskRequest.getOrderIndex() != existingTask.getOrderIndex()) {
            existingTask.setOrderIndex(taskRequest.getOrderIndex());
        }

        // Update due date if provided
        if (taskRequest.getDueDate() != null) {
            existingTask.setDueDate(taskRequest.getDueDate());

            // Check if task is now overdue
            if (LocalDateTime.now().isAfter(taskRequest.getDueDate())
                    && existingTask.getStatus() != TaskStatus.COMPLETED) {
                existingTask.setStatus(TaskStatus.OVERDUE);
            }
        }

        // Update status if provided
        if (taskRequest.getStatus() != null) {
            existingTask.setStatus(taskRequest.getStatus());

            // If marking as completed, reset the overdue status
            if (taskRequest.getStatus() == TaskStatus.COMPLETED) {
                existingTask.setReminderSent(false); // Reset reminder when completed
            }
        }
        TaskEntity updatedTask = taskRepository.save(existingTask);
        log.info("Updated task with ID: {}", updatedTask.getId());

        return taskEntityToTaskResponseMapper.map(updatedTask);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "#id"),
        @CacheEvict(value = {"tasksByTable", "tasksByUser", "tasksByStatus", "upcomingTasks", "taskStatistics"}, 
                    allEntries = true)
    })
    public void deleteTask(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity taskEntity = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        taskRepository.delete(taskEntity);
        log.info("Deleted task with ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"tasksByTable", "tasks", "tasksByUser", "tasksByStatus", "upcomingTasks"}, allEntries = true)
    public void reorderTasks(String tableId, List<String> taskIds) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if table exists and user has access to it
        accessControlService.findTableAndCheckAccess(tableId, currentUserId);

        // Update order indexes based on the provided order
        IntStream.range(0, taskIds.size()).forEach(index -> {
            String taskId = taskIds.get(index);
            TaskEntity task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

            // Check if the task belongs to the specified table
            if (!task.getTable().getId().equals(tableId)) {
                throw new UnauthorizedAccessException("Task does not belong to the specified table");
            }

            // Update the order index
            task.setOrderIndex(index + 1);
            taskRepository.save(task);
        });

        log.info("Reordered tasks for table ID: {}", tableId);
    }

    @Override
    @Cacheable(value = "upcomingTasks", key = "T(java.time.LocalDateTime).now().toLocalDate().toString()")
    public List<TaskResponseDTO> findByDueDateBetween(LocalDateTime start, LocalDateTime end) {
        String currentUserId = authUtils.getCurrentUserId();

        List<TaskEntity> tasks = taskRepository.findByDueDateBetween(start, end);

        // Filter tasks by access
        List<TaskEntity> accessibleTasks = tasks.stream().filter(task -> hasAccessToTaskBoard(task, currentUserId))
                .collect(Collectors.toList());

        return accessibleTasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    private boolean hasAccessToTaskBoard(TaskEntity task, String userId) {
        try {
            TableEntity table = task.getTable();
            if (table != null) {
                BoardEntity board = table.getBoard();
                if (board != null) {
                    return board.getOwnerId().equals(userId)
                            || (board.getCollaboratorIds() != null && board.getCollaboratorIds().contains(userId));
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Error checking board access for task: {}", task.getId(), e);
            return false;
        }
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "tasks", key = "#id"),
        @CacheEvict(value = {"tasksByStatus", "taskStatistics"}, allEntries = true)
    })
    public TaskResponseDTO updateTaskStatus(String id, TaskStatus newStatus) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity existingTask = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        // Record previous status for logging
        TaskStatus oldStatus = existingTask.getStatus();

        // Update the status
        existingTask.setStatus(newStatus);

        // If marking as completed, reset the reminder
        if (newStatus == TaskStatus.COMPLETED) {
            existingTask.setReminderSent(false);
        }

        TaskEntity updatedTask = taskRepository.save(existingTask);
        log.info("Task {} status changed from {} to {}", id, oldStatus, newStatus);

        return taskEntityToTaskResponseMapper.map(updatedTask);
    }

    @Override
    @Cacheable(value = "tasksByStatus", key = "#status")
    public List<TaskResponseDTO> getAllTasksByStatus(TaskStatus status) {
        String currentUserId = authUtils.getCurrentUserId();

        // Get all tasks with the specified status
        List<TaskEntity> tasks = taskRepository.findByStatus(status);

        // Filter to only include those the user has access to
        List<TaskEntity> accessibleTasks = tasks.stream().filter(task -> hasAccessToTaskBoard(task, currentUserId))
                .collect(Collectors.toList());

        return accessibleTasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "taskStatistics", key = "#userId")
    public Map<TaskStatus, Long> getTaskStatistics(String userId) {
        Map<TaskStatus, Long> statistics = new HashMap<>();

        // Populate statistics for each status
        for (TaskStatus status : TaskStatus.values()) {
            long count = taskRepository.countByAssignedUserIdAndStatus(userId, status);
            statistics.put(status, count);
        }

        return statistics;
    }
}