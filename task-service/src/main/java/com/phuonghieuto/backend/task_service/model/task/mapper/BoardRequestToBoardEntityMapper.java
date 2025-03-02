// BoardRequestToBoardEntityMapper.java
package com.phuonghieuto.backend.task_service.model.task.mapper;

import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BoardRequestToBoardEntityMapper extends BaseMapper<BoardRequestDTO, BoardEntity> {
    
    BoardRequestToBoardEntityMapper INSTANCE = Mappers.getMapper(BoardRequestToBoardEntityMapper.class);
    
    @Override
    BoardEntity map(BoardRequestDTO source);
       
    @Named("mapForCreation")
    default BoardEntity mapForCreation(BoardRequestDTO source, String ownerId) {
        if (source == null) {
            return null;
        }
        
        return BoardEntity.builder()
                .name(source.getName())
                .ownerId(ownerId)
                .collaboratorIds(source.getCollaboratorIds())
                .build();
    }
    
    static BoardRequestToBoardEntityMapper initialize() {
        return INSTANCE;
    }
}