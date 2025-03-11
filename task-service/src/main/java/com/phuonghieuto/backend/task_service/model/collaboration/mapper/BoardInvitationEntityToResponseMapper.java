package com.phuonghieuto.backend.task_service.model.collaboration.mapper;

import com.phuonghieuto.backend.task_service.model.collaboration.dto.response.BoardInvitationResponseDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.entity.BoardInvitationEntity;
import com.phuonghieuto.backend.task_service.model.common.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BoardInvitationEntityToResponseMapper extends BaseMapper<BoardInvitationEntity, BoardInvitationResponseDTO> {
    
    BoardInvitationEntityToResponseMapper INSTANCE = Mappers.getMapper(BoardInvitationEntityToResponseMapper.class);
    
    default BoardInvitationResponseDTO map(BoardInvitationEntity source) {
        if (source == null) {
            return null;
        }
        
        return BoardInvitationResponseDTO.builder()
                .id(source.getId())
                .boardId(source.getBoard().getId())
                .boardName(source.getBoard().getName())
                .inviterUserId(source.getInviterUserId())
                .inviteeEmail(source.getInviteeEmail())
                .inviteeUserId(source.getInviteeUserId())
                .status(source.getStatus())
                .token(source.getToken())
                .createdAt(source.getCreatedAt())
                .expiresAt(source.getExpiresAt())
                .build();
    }
    
    static BoardInvitationEntityToResponseMapper initialize() {
        return INSTANCE;
    }
}