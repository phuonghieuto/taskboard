package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface TableEntityToTableResponseMapper extends BaseMapper<TableEntity, TableResponseDTO> {
    
    TableEntityToTableResponseMapper INSTANCE = Mappers.getMapper(TableEntityToTableResponseMapper.class);
    
    default TableResponseDTO map(TableEntity source) {
        if (source == null) {
            return null;
        }

        Set<TaskResponseDTO> tasks = null;
        if (source.getTasks() != null) {
            tasks = source.getTasks().stream()
                .map(this::mapTaskEntityToTaskResponseDTO)
                .collect(Collectors.toSet());
        }
        
        return TableResponseDTO.builder()
                .id(source.getId())
                .name(source.getName())
                .orderIndex(source.getOrderIndex())
                .boardId(source.getBoard() != null ? source.getBoard().getId() : null)
                .tasks(tasks)
                .build();
    }
    
    default TaskResponseDTO mapTaskEntityToTaskResponseDTO(TaskEntity taskEntity) {
        if (taskEntity == null) {
            return null;
        }
        
        return TaskResponseDTO.builder()
                .id(taskEntity.getId())
                .title(taskEntity.getTitle())
                .description(taskEntity.getDescription())
                .orderIndex(taskEntity.getOrderIndex())
                .build();
    }
    
    static TableEntityToTableResponseMapper initialize() {
        return INSTANCE;
    }
}