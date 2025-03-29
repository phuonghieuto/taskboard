package com.phuonghieuto.backend.task_service.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntityAccessControlServiceImpl implements EntityAccessControlService{
    private final BoardRepository boardRepository;
    private final TableRepository tableRepository;
    private final TaskRepository taskRepository;

    @Override
    @Cacheable(value = "boardAccessControl", key = "#boardId + '-' + #userId")
    public BoardEntity findBoardAndCheckAccess(String boardId, String userId) {
        BoardEntity boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("Board not found"));

        checkBoardAccess(boardEntity, userId);
        return boardEntity;
    }

    @Override
    @Cacheable(value = "tableAccessControl", key = "#tableId + '-' + #userId")
    public TableEntity findTableAndCheckAccess(String tableId, String userId) {
        TableEntity tableEntity = tableRepository.findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found with ID: " + tableId));
        
        checkBoardAccess(tableEntity.getBoard(), userId);
        return tableEntity;
    }

    @Override
    @Cacheable(value = "taskAccessControl", key = "#taskId + '-' + #userId")
    public TaskEntity findTaskAndCheckAccess(String taskId, String userId) {
        TaskEntity taskEntity = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
        
        checkBoardAccess(taskEntity.getTable().getBoard(), userId);
        return taskEntity;
    }

    private void checkBoardAccess(BoardEntity board, String userId) {
        if (board == null) {
            throw new BoardNotFoundException("Board not found");
        }
        
        boolean hasAccess = board.getOwnerId().equals(userId) || 
                (board.getCollaboratorIds() != null && 
                board.getCollaboratorIds().contains(userId));
                
        if (!hasAccess) {
            throw new UnauthorizedAccessException("User does not have access to this resource");
        }
    }
    
}
