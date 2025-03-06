package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;

public interface EntityAccessControlService {
    
    public BoardEntity findBoardAndCheckAccess(String boardId, String userId);

    public TableEntity findTableAndCheckAccess(String tableId, String userId);

    public TaskEntity findTaskAndCheckAccess(String taskId, String userId);

}
