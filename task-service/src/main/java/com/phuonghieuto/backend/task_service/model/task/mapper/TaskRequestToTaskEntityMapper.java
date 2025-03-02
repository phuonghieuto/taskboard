package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskRequestToTaskEntityMapper extends BaseMapper<TaskRequestDTO, TaskEntity> {
    
    TaskRequestToTaskEntityMapper INSTANCE = Mappers.getMapper(TaskRequestToTaskEntityMapper.class);
    
    @Override
    TaskEntity map(TaskRequestDTO source);
    
    @Named("mapForCreation")
    default TaskEntity mapForCreation(TaskRequestDTO source, TableEntity table) {
        if (source == null) {
            return null;
        }
        
        return TaskEntity.builder()
                .title(source.getTitle())
                .description(source.getDescription())
                .orderIndex(source.getOrderIndex())
                .table(table)
                .assignedUserId(source.getAssignedUserId())
                .build();
    }
    
    static TaskRequestToTaskEntityMapper initialize() {
        return INSTANCE;
    }
}