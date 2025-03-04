package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskEntityToTaskResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskRequestToTaskEntityMapper;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final TableRepository tableRepository;
    private final BoardRepository boardRepository;
    private final TaskRequestToTaskEntityMapper taskRequestToTaskEntityMapper = TaskRequestToTaskEntityMapper
            .initialize();
    private final TaskEntityToTaskResponseMapper taskEntityToTaskResponseMapper = TaskEntityToTaskResponseMapper
            .initialize();

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO taskRequest) {
        String currentUserId = getCurrentUserId();

        // Check if table exists and user has access to it
        TableEntity table = findTableAndCheckAccess(taskRequest.getTableId(), currentUserId);

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
        String currentUserId = getCurrentUserId();
        TaskEntity taskEntity = findTaskAndCheckAccess(id, currentUserId);

        return taskEntityToTaskResponseMapper.map(taskEntity);
    }

    @Override
    public List<TaskResponseDTO> getAllTasksByTableId(String tableId) {
        String currentUserId = getCurrentUserId();

        // Check if table exists and user has access to it
        findTableAndCheckAccess(tableId, currentUserId);

        // Get all tasks for the table, ordered by orderIndex
        List<TaskEntity> tasks = taskRepository.findByTableIdOrderByOrderIndexAsc(tableId);

        return tasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    public List<TaskResponseDTO> getAllTasksByAssignedUserId(String userId) {
        String currentUserId = getCurrentUserId();

        // If requesting tasks for another user, verify current user has admin rights
        if (!currentUserId.equals(userId)) {
            // You could add admin check here if needed
            throw new UnauthorizedAccessException("You can only view your own tasks");
        }

        // Get all tasks assigned to the user
        List<TaskEntity> tasks = taskRepository.findByAssignedUserId(userId);

        // For each task, check if current user has access to its board
        List<TaskEntity> accessibleTasks = tasks.stream().filter(task -> hasAccessToTaskBoard(task, currentUserId))
                .collect(Collectors.toList());

        return accessibleTasks.stream().map(taskEntityToTaskResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    public TaskResponseDTO updateTask(String id, TaskRequestDTO taskRequest) {
        String currentUserId = getCurrentUserId();
        TaskEntity existingTask = findTaskAndCheckAccess(id, currentUserId);

        // Update task properties
        existingTask.setTitle(taskRequest.getTitle());
        existingTask.setDescription(taskRequest.getDescription());

        // If assignee has changed
        if (taskRequest.getAssignedUserId() != null) {
            existingTask.setAssignedUserId(taskRequest.getAssignedUserId());
        }

        // If table ID has changed (task moved to another table)
        if (!existingTask.getTable().getId().equals(taskRequest.getTableId())) {
            TableEntity newTable = findTableAndCheckAccess(taskRequest.getTableId(), currentUserId);
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
        String currentUserId = getCurrentUserId();
        TaskEntity taskEntity = findTaskAndCheckAccess(id, currentUserId);

        taskRepository.delete(taskEntity);
        log.info("Deleted task with ID: {}", id);
    }

    @Override
    @Transactional
    public void reorderTasks(String tableId, List<String> taskIds) {
        String currentUserId = getCurrentUserId();

        // Check if table exists and user has access to it
        findTableAndCheckAccess(tableId, currentUserId);

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

    // Helper methods
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim(TokenClaims.USER_ID.getValue());
        }
        throw new UnauthorizedAccessException("User not authenticated");
    }

    @Override
    public List<TaskResponseDTO> findByDueDateBetween(LocalDateTime start, LocalDateTime end) {
        return taskRepository.findByDueDateBetween(start, end).stream().map(taskEntityToTaskResponseMapper::map)
                .collect(Collectors.toList());
    }

    // private BoardEntity findBoardAndCheckAccess(String boardId, String userId) {
    //     BoardEntity boardEntity = boardRepository.findById(boardId)
    //             .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " + boardId));

    //     // Check if user is owner or collaborator
    //     boolean hasAccess = boardEntity.getOwnerId().equals(userId)
    //             || (boardEntity.getCollaboratorIds() != null && boardEntity.getCollaboratorIds().contains(userId));

    //     if (!hasAccess) {
    //         throw new UnauthorizedAccessException("User does not have access to this board");
    //     }

    //     return boardEntity;
    // }

    private TableEntity findTableAndCheckAccess(String tableId, String userId) {
        TableEntity tableEntity = tableRepository.findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found with ID: " + tableId));

        // Check if user has access to the board this table belongs to
        BoardEntity board = tableEntity.getBoard();
        if (board == null) {
            throw new TableNotFoundException("Table has no associated board");
        }

        boolean hasAccess = board.getOwnerId().equals(userId)
                || (board.getCollaboratorIds() != null && board.getCollaboratorIds().contains(userId));

        if (!hasAccess) {
            throw new UnauthorizedAccessException("User does not have access to this table");
        }

        return tableEntity;
    }

    private TaskEntity findTaskAndCheckAccess(String taskId, String userId) {
        TaskEntity taskEntity = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        // Check if user has access to the board this task belongs to
        TableEntity table = taskEntity.getTable();
        if (table == null) {
            throw new TaskNotFoundException("Task has no associated table");
        }

        BoardEntity board = table.getBoard();
        if (board == null) {
            throw new TableNotFoundException("Table has no associated board");
        }

        boolean hasAccess = board.getOwnerId().equals(userId)
                || (board.getCollaboratorIds() != null && board.getCollaboratorIds().contains(userId));

        if (!hasAccess) {
            throw new UnauthorizedAccessException("User does not have access to this task");
        }

        return taskEntity;
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
}