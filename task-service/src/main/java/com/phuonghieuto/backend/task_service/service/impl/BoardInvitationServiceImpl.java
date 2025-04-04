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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @CacheEvict(value = {"boardInvitations", "userInvitations"}, allEntries = true)
    public BoardInvitationResponseDTO createInvitation(String boardId, BoardInvitationRequestDTO invitationRequest) {
        log.info("Creating invitation for board: {}, invitee: {}", boardId, invitationRequest.getEmail());
        String currentUserId = authUtils.getCurrentUserId();
        BoardEntity board = accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        // Check if invitee is already collaborator or owner
        if (board.getOwnerId().equals(invitationRequest.getEmail()) || (board.getCollaboratorIds() != null
                && board.getCollaboratorIds().contains(invitationRequest.getEmail()))) {
            log.warn("Cannot create invitation: invitee is already a collaborator or owner. Board: {}, Invitee: {}", 
                    boardId, invitationRequest.getEmail());
            throw new DuplicateInvitationException("User is already a collaborator or owner of this board");
        }

        // Check for existing pending invitation
        Optional<BoardInvitationEntity> existingInvitation = boardInvitationRepository
                .findByBoardIdAndInviteeEmailAndStatusIn(boardId, invitationRequest.getEmail(),
                        Arrays.asList(InvitationStatus.PENDING));

        if (existingInvitation.isPresent()) {
            log.warn("Cannot create invitation: pending invitation already exists. Board: {}, Invitee: {}", 
                    boardId, invitationRequest.getEmail());
            throw new DuplicateInvitationException("A pending invitation already exists for this email");
        }

        // Create new invitation
        log.debug("Creating new invitation entity for board: {}", boardId);
        BoardInvitationEntity invitation = BoardInvitationEntity.builder().board(board).inviterUserId(currentUserId)
                .inviteeEmail(invitationRequest.getEmail())
                .inviteeUserId(authUtils.getUserIdFromEmail(invitationRequest.getEmail()))
                .status(InvitationStatus.PENDING).expiresAt(LocalDateTime.now().plusHours(invitationExpirationHours))
                .build();

        BoardInvitationEntity savedInvitation = boardInvitationRepository.save(invitation);
        log.info("Created board invitation: {}, board: {}, invitee: {}", 
                savedInvitation.getId(), boardId, invitationRequest.getEmail());
        
        String inviterName = authUtils.getCurrentUserFullName();
        log.debug("Sending invitation notification from: {}", inviterName);
        notificationProducer.sendBoardInvitationNotification(savedInvitation, inviterName);
        return invitationMapper.map(savedInvitation);
    }

    @Override
    @Cacheable(value = "invitations", key = "#id")
    public BoardInvitationResponseDTO getInvitationById(String id) {
        log.info("Retrieving invitation by ID: {}", id);
        BoardInvitationEntity invitation = findInvitationById(id);
        String currentUserId = authUtils.getCurrentUserId();

        // Only inviter, invitee or board owner/collaborator can view the invitation
        if (!hasAccessToInvitation(invitation, currentUserId)) {
            log.warn("Unauthorized access attempt to invitation: {}, user: {}", id, currentUserId);
            throw new UnauthorizedAccessException("You don't have permission to view this invitation");
        }

        log.debug("Returning invitation: {}", id);
        return invitationMapper.map(invitation);
    }

    @Override
    @Cacheable(value = "boardInvitations", key = "#boardId")
    public List<BoardInvitationResponseDTO> getPendingInvitationsForBoard(String boardId) {
        log.info("Retrieving pending invitations for board: {}", boardId);
        String currentUserId = authUtils.getCurrentUserId();
        accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        List<BoardInvitationEntity> pendingInvitations = boardInvitationRepository.findByBoardIdAndStatusIn(boardId,
                List.of(InvitationStatus.PENDING));

        log.debug("Found {} pending invitations for board: {}", pendingInvitations.size(), boardId);
        return pendingInvitations.stream().map(invitationMapper::map).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "userInvitations", key = "#email")
    public List<BoardInvitationResponseDTO> getPendingInvitationsForUser(String email) {
        log.info("Retrieving pending invitations for user: {}", email);
        List<BoardInvitationEntity> pendingInvitations = boardInvitationRepository.findByInviteeEmailAndStatusIn(email,
                List.of(InvitationStatus.PENDING));

        log.debug("Found {} pending invitations for user: {}", pendingInvitations.size(), email);
        return pendingInvitations.stream().map(invitationMapper::map).collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "invitations", key = "#id"),
        @CacheEvict(value = {"boardInvitations", "userInvitations"}, allEntries = true)
    })
    public BoardInvitationResponseDTO updateInvitationStatus(String id, InvitationStatus status) {
        log.info("Updating invitation status: {}, new status: {}", id, status);
        if (status == InvitationStatus.PENDING) {
            log.warn("Invalid status update attempt: cannot set status to PENDING for invitation: {}", id);
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
            log.warn("Unauthorized status update attempt for invitation: {}, user: {}", id, currentUserId);
            throw new UnauthorizedAccessException("You don't have permission to update this invitation");
        }

        // Check if invitation is expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempted to update expired invitation: {}", id);
            invitation.setStatus(InvitationStatus.EXPIRED);
            boardInvitationRepository.save(invitation);
            throw new IllegalStateException("This invitation has expired");
        }

        // Update status
        log.debug("Updating invitation status to {}: {}", status, id);
        invitation.setStatus(status);
        invitation.setInviteeUserId(currentUserId); // Always set the current user ID when accepting

        // If accepted, add user to board collaborators
        if (status == InvitationStatus.ACCEPTED) {
            log.debug("Invitation accepted, adding user {} to board collaborators", currentUserId);
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
    @Caching(evict = {
        @CacheEvict(value = "invitations", key = "#id"),
        @CacheEvict(value = {"boardInvitations", "userInvitations"}, allEntries = true)
    })
    public void cancelInvitation(String id) {
        log.info("Cancelling invitation: {}", id);
        BoardInvitationEntity invitation = findInvitationById(id);
        String currentUserId = authUtils.getCurrentUserId();

        // Only inviter or board owner can cancel
        if (!currentUserId.equals(invitation.getInviterUserId())
                && !currentUserId.equals(invitation.getBoard().getOwnerId())) {
            log.warn("Unauthorized cancellation attempt for invitation: {}, user: {}", id, currentUserId);
            throw new UnauthorizedAccessException("You don't have permission to cancel this invitation");
        }

        boardInvitationRepository.delete(invitation);
        log.info("Successfully cancelled invitation: {}", id);
    }

    @Override
    @CacheEvict(value = {"boardInvitations", "userInvitations", "invitations"}, allEntries = true)
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void processExpiredInvitations() {
        log.info("Starting scheduled job: processing expired invitations");
        LocalDateTime now = LocalDateTime.now();
        List<BoardInvitationEntity> expiredInvitations = boardInvitationRepository
                .findByExpiresAtBeforeAndStatus(now, InvitationStatus.PENDING);

        expiredInvitations.forEach(invitation -> {
            log.debug("Marking invitation as expired: {}, expired at: {}", 
                    invitation.getId(), invitation.getExpiresAt());
            invitation.setStatus(InvitationStatus.EXPIRED);
        });

        if (!expiredInvitations.isEmpty()) {
            boardInvitationRepository.saveAll(expiredInvitations);
            log.info("Processed {} expired invitations", expiredInvitations.size());
        } else {
            log.debug("No expired invitations found");
        }
    }

    private BoardInvitationEntity findInvitationById(String id) {
        log.debug("Finding invitation by ID: {}", id);
        return boardInvitationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Invitation not found with ID: {}", id);
                    return new InvitationNotFoundException("Invitation not found with ID: " + id);
                });
    }

    private boolean hasAccessToInvitation(BoardInvitationEntity invitation, String userId) {
        log.debug("Checking access for invitation: {}, user: {}", invitation.getId(), userId);
        String inviterUserId = invitation.getInviterUserId();
        String inviteeUserId = invitation.getInviteeUserId();
        BoardEntity board = invitation.getBoard();
        String currentUserEmail = authUtils.getCurrentUserEmail();

        boolean hasAccess = userId.equals(inviterUserId) || userId.equals(inviteeUserId)
                || userId.equals(authUtils.getUserIdFromEmail(invitation.getInviteeEmail()))
                || invitation.getInviteeEmail().equalsIgnoreCase(currentUserEmail) // Check by email
                || userId.equals(board.getOwnerId())
                || (board.getCollaboratorIds() != null && board.getCollaboratorIds().contains(userId));
        
        log.debug("Access check result for invitation {}: {}", invitation.getId(), hasAccess);
        return hasAccess;
    }

    @Override
    @Cacheable(value = "invitationTokens", key = "#token")
    public BoardInvitationResponseDTO getInvitationByToken(String token) {
        log.info("Retrieving invitation by token: {}", token);
        BoardInvitationEntity invitation = boardInvitationRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid invitation token: {}", token);
                    return new InvitationNotFoundException("Invalid invitation token");
                });

        // Check if invitation has expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())
                && invitation.getStatus() == InvitationStatus.PENDING) {
            log.warn("Expired invitation accessed with token: {}", token);
            invitation.setStatus(InvitationStatus.EXPIRED);
            boardInvitationRepository.save(invitation);
            throw new InvitationNotFoundException("This invitation has expired");
        }

        log.debug("Found invitation by token: {}, id: {}", token, invitation.getId());
        return invitationMapper.map(invitation);
    }
}