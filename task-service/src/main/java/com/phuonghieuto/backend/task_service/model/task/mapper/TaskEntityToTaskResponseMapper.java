package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskEntityToTaskResponseMapper extends BaseMapper<TaskEntity, TaskResponseDTO> {
    
    TaskEntityToTaskResponseMapper INSTANCE = Mappers.getMapper(TaskEntityToTaskResponseMapper.class);
    
    default TaskResponseDTO map(TaskEntity source) {
        if (source == null) {
            return null;
        }
        
        return TaskResponseDTO.builder()
                .id(source.getId())
                .title(source.getTitle())
                .description(source.getDescription())
                .orderIndex(source.getOrderIndex())
                .tableId(source.getTable() != null ? source.getTable().getId() : null)
                .assignedUserId(source.getAssignedUserId())
                .build();
    }
    
    static TaskEntityToTaskResponseMapper initialize() {
        return INSTANCE;
    }
}