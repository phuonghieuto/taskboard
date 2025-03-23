package com.phuonghieuto.backend.task_service.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.impl.BoardInvitationServiceImpl;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

@ExtendWith(MockitoExtension.class)
class BoardInvitationServiceImplTest {

    @Mock
    private BoardInvitationRepository boardInvitationRepository;

    @Mock
    private EntityAccessControlService accessControlService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private BoardInvitationEntityToResponseMapper boardInvitationEntityToResponseMapper;

    @InjectMocks
    private BoardInvitationServiceImpl boardInvitationService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_BOARD_ID = "test-board-id";
    private static final String TEST_INVITATION_ID = "test-invitation-id";
    private static final String TEST_INVITATION_TOKEN = "test-invitation-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_INVITER_USER_ID = "inviter-user-id";
    private static final String TEST_INVITER_FULL_NAME = "John Doe";

    private BoardEntity boardEntity;
    private BoardInvitationEntity invitationEntity;
    private BoardInvitationResponseDTO invitationResponseDTO;
    private BoardInvitationRequestDTO invitationRequestDTO;

    @BeforeEach
    void setUp() {
        // Mock static mapper initialization
        try (MockedStatic<BoardInvitationEntityToResponseMapper> mockedMapper = Mockito
                .mockStatic(BoardInvitationEntityToResponseMapper.class)) {
            mockedMapper.when(BoardInvitationEntityToResponseMapper::initialize)
                    .thenReturn(boardInvitationEntityToResponseMapper);

            // Recreate service to pick up mocked static mapper
            boardInvitationService = new BoardInvitationServiceImpl(boardInvitationRepository, accessControlService,
                    authUtils, notificationProducer);
        }

        // Set expiration hours
        ReflectionTestUtils.setField(boardInvitationService, "invitationExpirationHours", 48);

        // Set up test data
        boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        Set<String> collaboratorIds = new HashSet<>();
        collaboratorIds.add("collaborator-1");
        boardEntity.setCollaboratorIds(collaboratorIds);

        invitationEntity = BoardInvitationEntity.builder().id(TEST_INVITATION_ID).board(boardEntity)
                .inviterUserId(TEST_INVITER_USER_ID).inviteeEmail(TEST_EMAIL).token(TEST_INVITATION_TOKEN)
                .status(InvitationStatus.PENDING).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48)).build();

        invitationResponseDTO = BoardInvitationResponseDTO.builder().id(TEST_INVITATION_ID).boardId(TEST_BOARD_ID)
                .boardName("Test Board").inviterUserId(TEST_INVITER_USER_ID).inviteeEmail(TEST_EMAIL)
                .token(TEST_INVITATION_TOKEN).status(InvitationStatus.PENDING).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48)).build();

        invitationRequestDTO = new BoardInvitationRequestDTO();
        invitationRequestDTO.setEmail(TEST_EMAIL);
    }

    @Test
    void createInvitation_Success() {
        // Arrange
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(boardInvitationRepository.findByBoardIdAndInviteeEmailAndStatusIn(eq(TEST_BOARD_ID), eq(TEST_EMAIL),
                anyList())).thenReturn(Optional.empty());
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn("invitee-user-id");
        when(boardInvitationRepository.save(any(BoardInvitationEntity.class))).thenReturn(invitationEntity);
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);
        when(authUtils.getCurrentUserFullName()).thenReturn(TEST_INVITER_FULL_NAME);
        doNothing().when(notificationProducer).sendBoardInvitationNotification(any(), anyString());

        // Act
        BoardInvitationResponseDTO result = boardInvitationService.createInvitation(TEST_BOARD_ID,
                invitationRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_INVITATION_ID, result.getId());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(TEST_EMAIL, result.getInviteeEmail());
        assertEquals(InvitationStatus.PENDING, result.getStatus());

        // Verify interactions
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardInvitationRepository).findByBoardIdAndInviteeEmailAndStatusIn(eq(TEST_BOARD_ID), eq(TEST_EMAIL),
                anyList());
        verify(authUtils).getUserIdFromEmail(TEST_EMAIL);
        verify(boardInvitationRepository).save(any(BoardInvitationEntity.class));
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);
        verify(authUtils).getCurrentUserFullName();
        verify(notificationProducer).sendBoardInvitationNotification(any(), eq(TEST_INVITER_FULL_NAME));
    }

    @Test
    void createInvitation_InviteeAlreadyCollaborator_ThrowsException() {
        // Arrange
        boardEntity.getCollaboratorIds().add(TEST_EMAIL);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);

        // Act & Assert
        DuplicateInvitationException exception = assertThrows(DuplicateInvitationException.class,
                () -> boardInvitationService.createInvitation(TEST_BOARD_ID, invitationRequestDTO));

        assertEquals("User is already a collaborator or owner of this board", exception.getMessage());

        // Verify interactions
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardInvitationRepository, never()).save(any(BoardInvitationEntity.class));
    }

    @Test
    void createInvitation_DuplicatePendingInvitation_ThrowsException() {
        // Arrange
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(boardInvitationRepository.findByBoardIdAndInviteeEmailAndStatusIn(eq(TEST_BOARD_ID), eq(TEST_EMAIL),
                anyList())).thenReturn(Optional.of(invitationEntity));

        // Act & Assert
        DuplicateInvitationException exception = assertThrows(DuplicateInvitationException.class,
                () -> boardInvitationService.createInvitation(TEST_BOARD_ID, invitationRequestDTO));

        assertEquals("A pending invitation already exists for this email", exception.getMessage());

        // Verify interactions
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardInvitationRepository).findByBoardIdAndInviteeEmailAndStatusIn(eq(TEST_BOARD_ID), eq(TEST_EMAIL),
                anyList());
        verify(boardInvitationRepository, never()).save(any(BoardInvitationEntity.class));
    }

    @Test
    void getInvitationById_Success() {
        // Arrange
        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(authUtils.getCurrentUserEmail()).thenReturn("current@example.com");
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        BoardInvitationResponseDTO result = boardInvitationService.getInvitationById(TEST_INVITATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_INVITATION_ID, result.getId());

        // Verify interactions
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);
    }

    @Test
    void getInvitationById_NotFound_ThrowsException() {
        // Arrange
        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.empty());

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class,
                () -> boardInvitationService.getInvitationById(TEST_INVITATION_ID));

        assertEquals("Invitation not found with ID: " + TEST_INVITATION_ID, exception.getMessage());

        // Verify interactions
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
    }

    @Test
    void getInvitationById_UnauthorizedAccess_ThrowsException() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";
        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(unauthorizedUserId);
        when(authUtils.getCurrentUserEmail()).thenReturn("unauthorized@example.com");
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn("invitee-user-id");

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> boardInvitationService.getInvitationById(TEST_INVITATION_ID));

        assertEquals("You don't have permission to view this invitation", exception.getMessage());

        // Verify interactions
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(authUtils).getCurrentUserEmail();
    }

    @Test
    void getPendingInvitationsForBoard_Success() {
        // Arrange
        List<BoardInvitationEntity> pendingInvitations = Arrays.asList(invitationEntity);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(boardInvitationRepository.findByBoardIdAndStatusIn(eq(TEST_BOARD_ID), anyList()))
                .thenReturn(pendingInvitations);
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        List<BoardInvitationResponseDTO> results = boardInvitationService.getPendingInvitationsForBoard(TEST_BOARD_ID);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TEST_INVITATION_ID, results.get(0).getId());

        // Verify interactions
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardInvitationRepository).findByBoardIdAndStatusIn(eq(TEST_BOARD_ID), anyList());
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);
    }

    @Test
    void getPendingInvitationsForUser_Success() {
        // Arrange
        List<BoardInvitationEntity> pendingInvitations = Arrays.asList(invitationEntity);

        when(boardInvitationRepository.findByInviteeEmailAndStatusIn(eq(TEST_EMAIL), anyList()))
                .thenReturn(pendingInvitations);
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        List<BoardInvitationResponseDTO> results = boardInvitationService.getPendingInvitationsForUser(TEST_EMAIL);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TEST_INVITATION_ID, results.get(0).getId());

        // Verify interactions
        verify(boardInvitationRepository).findByInviteeEmailAndStatusIn(eq(TEST_EMAIL), anyList());
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);
    }

    @Test
    void updateInvitationStatus_Accept_Success() {
        // Arrange
        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(authUtils.getCurrentUserEmail()).thenReturn(TEST_EMAIL);
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn(TEST_USER_ID);
        when(boardInvitationRepository.save(invitationEntity)).thenReturn(invitationEntity);
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        BoardInvitationResponseDTO result = boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID,
                InvitationStatus.ACCEPTED);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_INVITATION_ID, result.getId());

        // Verify invitation was updated correctly
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(authUtils).getCurrentUserEmail();
        verify(boardInvitationRepository).save(invitationEntity);
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);

        // Verify board collaborators were updated
        assertTrue(boardEntity.getCollaboratorIds().contains(TEST_USER_ID));
    }

    @Test
    void updateInvitationStatus_Decline_Success() {
        // Arrange
        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(authUtils.getCurrentUserEmail()).thenReturn(TEST_EMAIL);
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn(TEST_USER_ID);
        when(boardInvitationRepository.save(invitationEntity)).thenReturn(invitationEntity);
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        BoardInvitationResponseDTO result = boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID,
                InvitationStatus.DECLINED);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_INVITATION_ID, result.getId());

        // Verify invitation was updated correctly
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(authUtils).getCurrentUserEmail();
        verify(boardInvitationRepository).save(invitationEntity);
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);

        // Verify board collaborators were NOT updated
        assertNotNull(boardEntity.getCollaboratorIds());
        assertEquals(1, boardEntity.getCollaboratorIds().size());
        assertTrue(boardEntity.getCollaboratorIds().contains("collaborator-1"));
        assertTrue(!boardEntity.getCollaboratorIds().contains(TEST_USER_ID));
    }

    @Test
    void updateInvitationStatus_UpdateToPending_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.PENDING));

        assertEquals("Cannot update invitation status to PENDING", exception.getMessage());

        // Verify no interactions
        verify(boardInvitationRepository, never()).findById(anyString());
    }

    @Test
    void updateInvitationStatus_Expired_ThrowsException() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(3);
        invitationEntity.setExpiresAt(pastTime);

        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(authUtils.getCurrentUserEmail()).thenReturn(TEST_EMAIL);
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn(TEST_USER_ID);
        when(boardInvitationRepository.save(invitationEntity)).thenReturn(invitationEntity);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED));

        assertEquals("This invitation has expired", exception.getMessage());

        // Verify invitation's status was updated to EXPIRED
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(authUtils).getCurrentUserEmail();
        verify(boardInvitationRepository).save(invitationEntity);
        assertEquals(InvitationStatus.EXPIRED, invitationEntity.getStatus());
    }

    @Test
    void updateInvitationStatus_UnauthorizedUser_ThrowsException() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";

        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(unauthorizedUserId);
        when(authUtils.getCurrentUserEmail()).thenReturn("other@example.com");
        when(authUtils.getUserIdFromEmail(TEST_EMAIL)).thenReturn("invitee-user-id");

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED));

        assertEquals("You don't have permission to update this invitation", exception.getMessage());

        // Verify
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(authUtils).getCurrentUserEmail();
        verify(boardInvitationRepository, never()).save(any());
    }

    @Test
    void cancelInvitation_ByInviter_Success() {
        // Arrange
        invitationEntity.setInviterUserId(TEST_USER_ID);

        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);

        // Act
        boardInvitationService.cancelInvitation(TEST_INVITATION_ID);

        // Assert & Verify
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(boardInvitationRepository).delete(invitationEntity);
    }

    @Test
    void cancelInvitation_ByBoardOwner_Success() {
        // Arrange
        boardEntity.setOwnerId(TEST_USER_ID);

        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);

        // Act
        boardInvitationService.cancelInvitation(TEST_INVITATION_ID);

        // Assert & Verify
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(boardInvitationRepository).delete(invitationEntity);
    }

    @Test
    void cancelInvitation_UnauthorizedUser_ThrowsException() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";

        when(boardInvitationRepository.findById(TEST_INVITATION_ID)).thenReturn(Optional.of(invitationEntity));
        when(authUtils.getCurrentUserId()).thenReturn(unauthorizedUserId);

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> boardInvitationService.cancelInvitation(TEST_INVITATION_ID));

        assertEquals("You don't have permission to cancel this invitation", exception.getMessage());

        // Verify
        verify(boardInvitationRepository).findById(TEST_INVITATION_ID);
        verify(authUtils).getCurrentUserId();
        verify(boardInvitationRepository, never()).delete(any());
    }

    @Test
    void processExpiredInvitations_Success() {
        // Arrange
        List<BoardInvitationEntity> expiredInvitations = Arrays.asList(invitationEntity);

        when(boardInvitationRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class),
                eq(InvitationStatus.PENDING))).thenReturn(expiredInvitations);
        when(boardInvitationRepository.saveAll(expiredInvitations)).thenReturn(expiredInvitations);

        // Act
        boardInvitationService.processExpiredInvitations();

        // Assert & Verify
        verify(boardInvitationRepository).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class),
                eq(InvitationStatus.PENDING));
        verify(boardInvitationRepository).saveAll(expiredInvitations);
        assertEquals(InvitationStatus.EXPIRED, invitationEntity.getStatus());
    }

    @Test
    void processExpiredInvitations_NoExpiredInvitations() {
        // Arrange
        when(boardInvitationRepository.findByExpiresAtBeforeAndStatus(any(LocalDateTime.class),
                eq(InvitationStatus.PENDING))).thenReturn(Collections.emptyList());

        // Act
        boardInvitationService.processExpiredInvitations();

        // Assert & Verify
        verify(boardInvitationRepository).findByExpiresAtBeforeAndStatus(any(LocalDateTime.class),
                eq(InvitationStatus.PENDING));
        verify(boardInvitationRepository, never()).saveAll(anyList());
    }

    @Test
    void getInvitationByToken_Success() {
        // Arrange
        when(boardInvitationRepository.findByToken(TEST_INVITATION_TOKEN)).thenReturn(Optional.of(invitationEntity));
        when(boardInvitationEntityToResponseMapper.map(invitationEntity)).thenReturn(invitationResponseDTO);

        // Act
        BoardInvitationResponseDTO result = boardInvitationService.getInvitationByToken(TEST_INVITATION_TOKEN);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_INVITATION_ID, result.getId());
        assertEquals(TEST_INVITATION_TOKEN, result.getToken());

        // Verify
        verify(boardInvitationRepository).findByToken(TEST_INVITATION_TOKEN);
        verify(boardInvitationEntityToResponseMapper).map(invitationEntity);
    }

    @Test
    void getInvitationByToken_NotFound_ThrowsException() {
        // Arrange
        when(boardInvitationRepository.findByToken(TEST_INVITATION_TOKEN)).thenReturn(Optional.empty());

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class,
                () -> boardInvitationService.getInvitationByToken(TEST_INVITATION_TOKEN));

        assertEquals("Invalid invitation token", exception.getMessage());

        // Verify
        verify(boardInvitationRepository).findByToken(TEST_INVITATION_TOKEN);
    }

    @Test
    void getInvitationByToken_Expired_ThrowsException() {
        // Arrange
        invitationEntity.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(boardInvitationRepository.findByToken(TEST_INVITATION_TOKEN)).thenReturn(Optional.of(invitationEntity));
        when(boardInvitationRepository.save(invitationEntity)).thenReturn(invitationEntity);

        // Act & Assert
        InvitationNotFoundException exception = assertThrows(InvitationNotFoundException.class,
                () -> boardInvitationService.getInvitationByToken(TEST_INVITATION_TOKEN));

        assertEquals("This invitation has expired", exception.getMessage());

        // Verify
        verify(boardInvitationRepository).findByToken(TEST_INVITATION_TOKEN);
        verify(boardInvitationRepository).save(invitationEntity);
        assertEquals(InvitationStatus.EXPIRED, invitationEntity.getStatus());
    }
}