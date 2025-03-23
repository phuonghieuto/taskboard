package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.DuplicateInvitationException;
import com.phuonghieuto.backend.task_service.exception.InvitationNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.request.BoardInvitationRequestDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.response.BoardInvitationResponseDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.entity.BoardInvitationEntity;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;
import com.phuonghieuto.backend.task_service.model.collaboration.mapper.BoardInvitationEntityToResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.repository.BoardInvitationRepository;
import com.phuonghieuto.backend.task_service.service.BoardInvitationService;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardInvitationServiceImpl implements BoardInvitationService {

    private final BoardInvitationRepository boardInvitationRepository;
    private final EntityAccessControlService accessControlService;
    private final AuthUtils authUtils;
    private final NotificationProducer notificationProducer;
    private final BoardInvitationEntityToResponseMapper invitationMapper = BoardInvitationEntityToResponseMapper
            .initialize();

    @Value("${app.invitation.expiration-hours:48}")
    private int invitationExpirationHours;

    @Override
    public BoardInvitationResponseDTO createInvitation(String boardId, BoardInvitationRequestDTO invitationRequest) {
        String currentUserId = authUtils.getCurrentUserId();
        BoardEntity board = accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        // Check if invitee is already collaborator or owner
        if (board.getOwnerId().equals(invitationRequest.getEmail()) || (board.getCollaboratorIds() != null
                && board.getCollaboratorIds().contains(invitationRequest.getEmail()))) {
            throw new DuplicateInvitationException("User is already a collaborator or owner of this board");
        }

        // Check for existing pending invitation
        Optional<BoardInvitationEntity> existingInvitation = boardInvitationRepository
                .findByBoardIdAndInviteeEmailAndStatusIn(boardId, invitationRequest.getEmail(),
                        Arrays.asList(InvitationStatus.PENDING));

        if (existingInvitation.isPresent()) {
            throw new DuplicateInvitationException("A pending invitation already exists for this email");
        }

        // Create new invitation
        BoardInvitationEntity invitation = BoardInvitationEntity.builder().board(board).inviterUserId(currentUserId)
                .inviteeEmail(invitationRequest.getEmail())
                .inviteeUserId(authUtils.getUserIdFromEmail(invitationRequest.getEmail()))
                .status(InvitationStatus.PENDING).expiresAt(LocalDateTime.now().plusHours(invitationExpirationHours))
                .build();

        BoardInvitationEntity savedInvitation = boardInvitationRepository.save(invitation);
        log.info("Created board invitation: {}", savedInvitation.getId());
        
        String inviterName = authUtils.getCurrentUserFullName();
        notificationProducer.sendBoardInvitationNotification(savedInvitation, inviterName);
        return invitationMapper.map(savedInvitation);
    }

    @Override
    public BoardInvitationResponseDTO getInvitationById(String id) {
        BoardInvitationEntity invitation = findInvitationById(id);
        String currentUserId = authUtils.getCurrentUserId();

        // Only inviter, invitee or board owner/collaborator can view the invitation
        if (!hasAccessToInvitation(invitation, currentUserId)) {
            throw new UnauthorizedAccessException("You don't have permission to view this invitation");
        }

        return invitationMapper.map(invitation);
    }

    @Override
    public List<BoardInvitationResponseDTO> getPendingInvitationsForBoard(String boardId) {
        String currentUserId = authUtils.getCurrentUserId();
        accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        List<BoardInvitationEntity> pendingInvitations = boardInvitationRepository.findByBoardIdAndStatusIn(boardId,
                List.of(InvitationStatus.PENDING));

        return pendingInvitations.stream().map(invitationMapper::map).collect(Collectors.toList());
    }

    @Override
    public List<BoardInvitationResponseDTO> getPendingInvitationsForUser(String email) {
        List<BoardInvitationEntity> pendingInvitations = boardInvitationRepository.findByInviteeEmailAndStatusIn(email,
                List.of(InvitationStatus.PENDING));

        return pendingInvitations.stream().map(invitationMapper::map).collect(Collectors.toList());
    }

    @Override
    public BoardInvitationResponseDTO updateInvitationStatus(String id, InvitationStatus status) {
        if (status == InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot update invitation status to PENDING");
        }

        BoardInvitationEntity invitation = findInvitationById(id);
        String currentUserId = authUtils.getCurrentUserId();
        String currentUserEmail = authUtils.getCurrentUserEmail(); // Get current user's email

        // Check if current user's email matches the invitation email
        boolean isInvitee = invitation.getInviteeEmail().equalsIgnoreCase(currentUserEmail);
        boolean isInviteeById = currentUserId.equals(invitation.getInviteeUserId())
                || currentUserId.equals(authUtils.getUserIdFromEmail(invitation.getInviteeEmail()));

        // Only invitee can accept or decline (by email match or ID match)
        if (!isInvitee && !isInviteeById) {
            throw new UnauthorizedAccessException("You don't have permission to update this invitation");
        }

        // Check if invitation is expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            boardInvitationRepository.save(invitation);
            throw new IllegalStateException("This invitation has expired");
        }

        // Update status
        invitation.setStatus(status);
        invitation.setInviteeUserId(currentUserId); // Always set the current user ID when accepting

        // If accepted, add user to board collaborators
        if (status == InvitationStatus.ACCEPTED) {
            BoardEntity board = invitation.getBoard();
            Set<String> collaborators = board.getCollaboratorIds();
            if (collaborators == null) {
                collaborators = new HashSet<>();
                collaborators.add(currentUserId);
            } else {
                collaborators.add(currentUserId);
            }
            board.setCollaboratorIds(collaborators);
        }

        BoardInvitationEntity updatedInvitation = boardInvitationRepository.save(invitation);
        log.info("Updated invitation status to {}: {}", status, id);

        return invitationMapper.map(updatedInvitation);
    }

    @Override
    public void cancelInvitation(String id) {
        BoardInvitationEntity invitation = findInvitationById(id);
        String currentUserId = authUtils.getCurrentUserId();

        // Only inviter or board owner can cancel
        if (!currentUserId.equals(invitation.getInviterUserId())
                && !currentUserId.equals(invitation.getBoard().getOwnerId())) {
            throw new UnauthorizedAccessException("You don't have permission to cancel this invitation");
        }

        boardInvitationRepository.delete(invitation);
        log.info("Cancelled invitation: {}", id);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional // Run every hour
    public void processExpiredInvitations() {
        List<BoardInvitationEntity> expiredInvitations = boardInvitationRepository
                .findByExpiresAtBeforeAndStatus(LocalDateTime.now(), InvitationStatus.PENDING);

        expiredInvitations.forEach(invitation -> {
            invitation.setStatus(InvitationStatus.EXPIRED);
        });

        if (!expiredInvitations.isEmpty()) {
            boardInvitationRepository.saveAll(expiredInvitations);
            log.info("Processed {} expired invitations", expiredInvitations.size());
        }
    }

    private BoardInvitationEntity findInvitationById(String id) {
        return boardInvitationRepository.findById(id)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found with ID: " + id));
    }

    private boolean hasAccessToInvitation(BoardInvitationEntity invitation, String userId) {
        String inviterUserId = invitation.getInviterUserId();
        String inviteeUserId = invitation.getInviteeUserId();
        BoardEntity board = invitation.getBoard();
        String currentUserEmail = authUtils.getCurrentUserEmail();

        return userId.equals(inviterUserId) || userId.equals(inviteeUserId)
                || userId.equals(authUtils.getUserIdFromEmail(invitation.getInviteeEmail()))
                || invitation.getInviteeEmail().equalsIgnoreCase(currentUserEmail) // Check by email
                || userId.equals(board.getOwnerId())
                || (board.getCollaboratorIds() != null && board.getCollaboratorIds().contains(userId));
    }

    @Override
    public BoardInvitationResponseDTO getInvitationByToken(String token) {
        BoardInvitationEntity invitation = boardInvitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation token"));

        // Check if invitation has expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())
                && invitation.getStatus() == InvitationStatus.PENDING) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            boardInvitationRepository.save(invitation);
            throw new InvitationNotFoundException("This invitation has expired");
        }

        return invitationMapper.map(invitation);
    }
}