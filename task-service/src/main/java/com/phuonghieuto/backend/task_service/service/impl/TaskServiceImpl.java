package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskEntityToTaskResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskRequestToTaskEntityMapper;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.TaskService;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    public TaskResponseDTO getTaskById(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity taskEntity = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        return taskEntityToTaskResponseMapper.map(taskEntity);
    }

    @Override
    public List<TaskResponseDTO> getAllTasksByTableId(String tableId) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if table exists and user has access to it
        accessControlService.findTableAndCheckAccess(tableId, currentUserId);

        // Get all tasks for the table, ordered by orderIndex
        List<TaskEntity> tasks = taskRepository.findByTableIdOrderByOrderIndexAsc(tableId);

        return tasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
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

        TaskEntity updatedTask = taskRepository.save(existingTask);
        log.info("Updated task with ID: {}", updatedTask.getId());

        return taskEntityToTaskResponseMapper.map(updatedTask);
    }

    @Override
    public void deleteTask(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TaskEntity taskEntity = accessControlService.findTaskAndCheckAccess(id, currentUserId);

        taskRepository.delete(taskEntity);
        log.info("Deleted task with ID: {}", id);
    }

    @Override
    @Transactional
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

    // @Scheduled(cron = "0 */5 * * * *") // Check every 5 minutes
    // public void checkAndPublishUpcomingTasks() {
    // LocalDateTime now = LocalDateTime.now();
    // LocalDateTime cutoff = now.plusHours(1); // Tasks due within next hour

    // List<TaskEntity> upcomingTasks =
    // taskRepository.findByDueDateBetweenAndReminderSent(now, cutoff, false);

    // for (TaskEntity task : upcomingTasks) {
    // Map<String, Object> eventData = new HashMap<>();
    // eventData.put("taskId", task.getId());
    // eventData.put("userId", task.getAssignedUserId());
    // eventData.put("title", task.getTitle());
    // eventData.put("dueDate", task.getDueDate());

    // // Publish to RabbitMQ
    // rabbitTemplate.convertAndSend("task.events", "task.upcoming", eventData);

    // // Mark that we've sent a reminder
    // task.setReminderSent(true);
    // taskRepository.save(task);
    // }
    // }
}