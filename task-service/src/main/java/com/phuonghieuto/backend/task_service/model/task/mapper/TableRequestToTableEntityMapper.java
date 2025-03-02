package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TableRequestToTableEntityMapper extends BaseMapper<TableRequestDTO, TableEntity> {
    
    TableRequestToTableEntityMapper INSTANCE = Mappers.getMapper(TableRequestToTableEntityMapper.class);
    
    @Override
    TableEntity map(TableRequestDTO source);
    
    @Named("mapForCreation")
    default TableEntity mapForCreation(TableRequestDTO source, BoardEntity board) {
        if (source == null) {
            return null;
        }
        
        return TableEntity.builder()
                .name(source.getName())
                .orderIndex(source.getOrderIndex())
                .board(board)
                .build();
    }
    
    static TableRequestToTableEntityMapper initialize() {
        return INSTANCE;
    }
}