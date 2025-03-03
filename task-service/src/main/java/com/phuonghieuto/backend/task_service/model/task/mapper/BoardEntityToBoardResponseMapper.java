package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface BoardEntityToBoardResponseMapper extends BaseMapper<BoardEntity, BoardResponseDTO> {

    BoardEntityToBoardResponseMapper INSTANCE = Mappers.getMapper(BoardEntityToBoardResponseMapper.class);

    default BoardResponseDTO map(BoardEntity source) {
        if (source == null) {
            return null;
        }

        List<TableResponseDTO> tables = null;
        if (source.getTables() != null) {
            tables = source.getTables().stream().sorted(Comparator.comparing(TableEntity::getOrderIndex))
                    .map(this::mapTableEntityToTableResponseDTO).collect(Collectors.toList());
        }

        return BoardResponseDTO.builder().id(source.getId()).name(source.getName()).ownerId(source.getOwnerId())
                .collaboratorIds(source.getCollaboratorIds()).tables(tables != null ? new HashSet<>(tables) : null)
                .build();
    }

    default TableResponseDTO mapTableEntityToTableResponseDTO(TableEntity tableEntity) {
        if (tableEntity == null) {
            return null;
        }

        Set<TaskResponseDTO> tasks = null;
        if (tableEntity.getTasks() != null) {
            // Sort tasks by orderIndex before mapping
            List<TaskEntity> sortedTasks = new ArrayList<>(tableEntity.getTasks());
            sortedTasks.sort(Comparator.comparing(TaskEntity::getOrderIndex));

            tasks = sortedTasks.stream().map(this::mapTaskEntityToTaskResponseDTO).collect(Collectors.toSet());
        }

        return TableResponseDTO.builder().id(tableEntity.getId()).name(tableEntity.getName())
                .orderIndex(tableEntity.getOrderIndex())
                .boardId(tableEntity.getBoard() != null ? tableEntity.getBoard().getId() : null).tasks(tasks).build();
    }

    default TaskResponseDTO mapTaskEntityToTaskResponseDTO(TaskEntity taskEntity) {
        if (taskEntity == null) {
            return null;
        }

        return TaskResponseDTO.builder().id(taskEntity.getId()).title(taskEntity.getTitle())
                .description(taskEntity.getDescription()).orderIndex(taskEntity.getOrderIndex())
                .assignedUserId(taskEntity.getAssignedUserId())
                .tableId(taskEntity.getTable() != null ? taskEntity.getTable().getId() : null).build();
    }

    static BoardEntityToBoardResponseMapper initialize() {
        return INSTANCE;
    }
}