package com.phuonghieuto.backend.task_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phuonghieuto.backend.task_service.model.collaboration.entity.BoardInvitationEntity;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;

@Repository
public interface BoardInvitationRepository extends JpaRepository<BoardInvitationEntity, String> {
    
    List<BoardInvitationEntity> findByBoardIdAndStatusIn(String boardId, List<InvitationStatus> statuses);
    
    List<BoardInvitationEntity> findByInviteeEmailAndStatusIn(String email, List<InvitationStatus> statuses);
    
    Optional<BoardInvitationEntity> findByBoardIdAndInviteeEmailAndStatusIn(
            String boardId, String email, List<InvitationStatus> statuses);
    
    List<BoardInvitationEntity> findByExpiresAtBeforeAndStatus(LocalDateTime expiryTime, InvitationStatus status);
    
    List<BoardInvitationEntity> findByInviteeUserIdAndStatusIn(String userId, List<InvitationStatus> statuses);
    
    Optional<BoardInvitationEntity> findByToken(String token);
}
