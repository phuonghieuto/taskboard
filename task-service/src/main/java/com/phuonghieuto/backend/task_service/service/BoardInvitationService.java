package com.phuonghieuto.backend.task_service.service;

import java.util.List;

import com.phuonghieuto.backend.task_service.model.collaboration.dto.request.BoardInvitationRequestDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.response.BoardInvitationResponseDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;

public interface BoardInvitationService {
    
    BoardInvitationResponseDTO createInvitation(String boardId, BoardInvitationRequestDTO invitationRequest);
    
    BoardInvitationResponseDTO getInvitationById(String id);
    
    List<BoardInvitationResponseDTO> getPendingInvitationsForBoard(String boardId);
    
    List<BoardInvitationResponseDTO> getPendingInvitationsForUser(String email);
    
    BoardInvitationResponseDTO updateInvitationStatus(String id, InvitationStatus status);
    
    void cancelInvitation(String id);
    
    BoardInvitationResponseDTO getInvitationByToken(String token);

    void processExpiredInvitations();
}
