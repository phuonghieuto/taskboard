// BoardEntityToBoardResponseMapper.java
package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface BoardEntityToBoardResponseMapper extends BaseMapper<BoardEntity, BoardResponseDTO> {
    
    BoardEntityToBoardResponseMapper INSTANCE = Mappers.getMapper(BoardEntityToBoardResponseMapper.class);
    
    default BoardResponseDTO map(BoardEntity source) {
        if (source == null) {
            return null;
        }

        Set<TableResponseDTO> tables = null;
        if (source.getTables() != null) {
            tables = source.getTables().stream()
                .map(this::mapTableEntityToTableResponseDTO)
                .collect(Collectors.toSet());
        }
        
        return BoardResponseDTO.builder()
                .id(source.getId())
                .name(source.getName())
                .ownerId(source.getOwnerId())
                .collaboratorIds(source.getCollaboratorIds())
                .tables(tables)
                .build();
    }
    
    default TableResponseDTO mapTableEntityToTableResponseDTO(TableEntity tableEntity) {
        if (tableEntity == null) {
            return null;
        }
        
        return TableResponseDTO.builder()
                .id(tableEntity.getId())
                .name(tableEntity.getName())
                .orderIndex(tableEntity.getOrderIndex())
                .build();
    }
    
    static BoardEntityToBoardResponseMapper initialize() {
        return INSTANCE;
    }
}