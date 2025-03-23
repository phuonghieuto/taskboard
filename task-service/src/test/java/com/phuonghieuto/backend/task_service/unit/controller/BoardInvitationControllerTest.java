package com.phuonghieuto.backend.task_service.unit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phuonghieuto.backend.task_service.controller.BoardInvitationController;
import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.DuplicateInvitationException;
import com.phuonghieuto.backend.task_service.exception.InvitationNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.request.BoardInvitationRequestDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.response.BoardInvitationResponseDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;
import com.phuonghieuto.backend.task_service.service.BoardInvitationService;

@ExtendWith(MockitoExtension.class)
class BoardInvitationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BoardInvitationService boardInvitationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardInvitationController boardInvitationController;

    private static final String TEST_BOARD_ID = "test-board-id";
    private static final String TEST_INVITATION_ID = "test-invitation-id";
    private static final String TEST_TOKEN = "test-invitation-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_INVITER_ID = "test-inviter-id";
    private static final String TEST_BOARD_NAME = "Test Board";

    private BoardInvitationRequestDTO invitationRequest;
    private BoardInvitationResponseDTO invitationResponse;
    private List<BoardInvitationResponseDTO> invitationResponseList;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(boardInvitationController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test data
        // Create request DTO
        invitationRequest = new BoardInvitationRequestDTO();
        invitationRequest.setEmail(TEST_EMAIL);

        // Create response DTO
        LocalDateTime now = LocalDateTime.now();
        invitationResponse = BoardInvitationResponseDTO.builder().id(TEST_INVITATION_ID).boardId(TEST_BOARD_ID)
                .boardName(TEST_BOARD_NAME).inviterUserId(TEST_INVITER_ID).inviteeEmail(TEST_EMAIL)
                .inviteeUserId(TEST_USER_ID).token(TEST_TOKEN).status(InvitationStatus.PENDING).createdAt(now)
                .expiresAt(now.plusHours(48)).build();

        // Create a second invitation for list tests
        BoardInvitationResponseDTO invitation2 = BoardInvitationResponseDTO.builder().id("invitation-id-2")
                .boardId(TEST_BOARD_ID).boardName(TEST_BOARD_NAME).inviterUserId(TEST_INVITER_ID)
                .inviteeEmail("another@example.com").inviteeUserId("another-user-id").token("another-token")
                .status(InvitationStatus.PENDING).createdAt(now).expiresAt(now.plusHours(48)).build();

        // Create list of invitations
        invitationResponseList = Arrays.asList(invitationResponse, invitation2);

        // Setup authentication mock
    }

    @Test
    void createInvitation_Success() throws Exception {
        when(boardInvitationService.createInvitation(eq(TEST_BOARD_ID), any(BoardInvitationRequestDTO.class)))
                .thenReturn(invitationResponse);

        mockMvc.perform(post("/board-invitations/board/{boardId}", TEST_BOARD_ID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response.boardId").value(TEST_BOARD_ID))
                .andExpect(jsonPath("$.response.boardName").value(TEST_BOARD_NAME))
                .andExpect(jsonPath("$.response.inviteeEmail").value(TEST_EMAIL))
                .andExpect(jsonPath("$.response.status").value("PENDING"))
                .andExpect(jsonPath("$.response.token").value(TEST_TOKEN));

        verify(boardInvitationService, times(1)).createInvitation(eq(TEST_BOARD_ID),
                any(BoardInvitationRequestDTO.class));
    }

    @Test
    void createInvitation_InvalidRequest() throws Exception {
        // Create invalid request (empty email)
        BoardInvitationRequestDTO invalidRequest = new BoardInvitationRequestDTO();

        mockMvc.perform(post("/board-invitations/board/{boardId}", TEST_BOARD_ID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));

        verify(boardInvitationService, times(0)).createInvitation(anyString(), any(BoardInvitationRequestDTO.class));
    }

    @Test
    void createInvitation_BoardNotFound() throws Exception {
        when(boardInvitationService.createInvitation(eq(TEST_BOARD_ID), any(BoardInvitationRequestDTO.class)))
                .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

        mockMvc.perform(post("/board-invitations/board/{boardId}", TEST_BOARD_ID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

        verify(boardInvitationService, times(1)).createInvitation(eq(TEST_BOARD_ID),
                any(BoardInvitationRequestDTO.class));
    }

    @Test
    void createInvitation_DuplicateInvitation() throws Exception {
        when(boardInvitationService.createInvitation(eq(TEST_BOARD_ID), any(BoardInvitationRequestDTO.class)))
                .thenThrow(new DuplicateInvitationException("A pending invitation already exists for this email"));

        mockMvc.perform(post("/board-invitations/board/{boardId}", TEST_BOARD_ID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("A pending invitation already exists for this email"));

        verify(boardInvitationService, times(1)).createInvitation(eq(TEST_BOARD_ID),
                any(BoardInvitationRequestDTO.class));
    }

    @Test
    void createInvitation_Unauthorized() throws Exception {
        when(boardInvitationService.createInvitation(eq(TEST_BOARD_ID), any(BoardInvitationRequestDTO.class)))
                .thenThrow(new UnauthorizedAccessException("You don't have permission to invite users to this board"));

        mockMvc.perform(post("/board-invitations/board/{boardId}", TEST_BOARD_ID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invitationRequest)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("You don't have permission to invite users to this board"));

        verify(boardInvitationService, times(1)).createInvitation(eq(TEST_BOARD_ID),
                any(BoardInvitationRequestDTO.class));
    }

    @Test
    void getInvitation_Success() throws Exception {
        when(boardInvitationService.getInvitationById(TEST_INVITATION_ID)).thenReturn(invitationResponse);

        mockMvc.perform(get("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response.boardId").value(TEST_BOARD_ID))
                .andExpect(jsonPath("$.response.inviteeEmail").value(TEST_EMAIL));

        verify(boardInvitationService, times(1)).getInvitationById(TEST_INVITATION_ID);
    }

    @Test
    void getInvitation_NotFound() throws Exception {
        when(boardInvitationService.getInvitationById(TEST_INVITATION_ID))
                .thenThrow(new InvitationNotFoundException("Invitation not found with ID: " + TEST_INVITATION_ID));

        mockMvc.perform(get("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Invitation not found with ID: " + TEST_INVITATION_ID));

        verify(boardInvitationService, times(1)).getInvitationById(TEST_INVITATION_ID);
    }

    @Test
    void getInvitation_Unauthorized() throws Exception {
        when(boardInvitationService.getInvitationById(TEST_INVITATION_ID))
                .thenThrow(new UnauthorizedAccessException("You don't have permission to view this invitation"));

        mockMvc.perform(get("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("You don't have permission to view this invitation"));

        verify(boardInvitationService, times(1)).getInvitationById(TEST_INVITATION_ID);
    }

    @Test
    void getPendingInvitationsForBoard_Success() throws Exception {
        when(boardInvitationService.getPendingInvitationsForBoard(TEST_BOARD_ID)).thenReturn(invitationResponseList);

        mockMvc.perform(get("/board-invitations/board/{boardId}", TEST_BOARD_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true)).andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response", hasSize(2)))
                .andExpect(jsonPath("$.response[0].id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response[1].id").value("invitation-id-2"));

        verify(boardInvitationService, times(1)).getPendingInvitationsForBoard(TEST_BOARD_ID);
    }

    @Test
    void getPendingInvitationsForBoard_EmptyList() throws Exception {
        when(boardInvitationService.getPendingInvitationsForBoard(TEST_BOARD_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/board-invitations/board/{boardId}", TEST_BOARD_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true)).andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response").isEmpty());

        verify(boardInvitationService, times(1)).getPendingInvitationsForBoard(TEST_BOARD_ID);
    }

    @Test
    void getPendingInvitationsForBoard_BoardNotFound() throws Exception {
        when(boardInvitationService.getPendingInvitationsForBoard(TEST_BOARD_ID))
                .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

        mockMvc.perform(get("/board-invitations/board/{boardId}", TEST_BOARD_ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

        verify(boardInvitationService, times(1)).getPendingInvitationsForBoard(TEST_BOARD_ID);
    }

    @Test
    void getPendingInvitationsForBoard_Unauthorized() throws Exception {
        when(boardInvitationService.getPendingInvitationsForBoard(TEST_BOARD_ID))
                .thenThrow(new UnauthorizedAccessException("You don't have access to this board"));

        mockMvc.perform(get("/board-invitations/board/{boardId}", TEST_BOARD_ID)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("You don't have access to this board"));

        verify(boardInvitationService, times(1)).getPendingInvitationsForBoard(TEST_BOARD_ID);
    }

    @Test
    void getMyPendingInvitations_Success() throws Exception {
        when(authentication.getName()).thenReturn(TEST_EMAIL);

        when(boardInvitationService.getPendingInvitationsForUser(TEST_EMAIL)).thenReturn(invitationResponseList);

        mockMvc.perform(get("/board-invitations/my-invitations").principal(authentication)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true)).andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response", hasSize(2)));

        verify(authentication, times(2)).getName();
        verify(boardInvitationService, times(1)).getPendingInvitationsForUser(TEST_EMAIL);
    }

    @Test
    void getMyPendingInvitations_EmptyList() throws Exception {
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(boardInvitationService.getPendingInvitationsForUser(TEST_EMAIL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/board-invitations/my-invitations").principal(authentication)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true)).andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response").isEmpty());

        verify(authentication, times(2)).getName();
        verify(boardInvitationService, times(1)).getPendingInvitationsForUser(TEST_EMAIL);
    }

    @Test
    void acceptInvitation_Success() throws Exception {
        BoardInvitationResponseDTO acceptedInvitation = BoardInvitationResponseDTO.builder().id(TEST_INVITATION_ID)
                .boardId(TEST_BOARD_ID).boardName(TEST_BOARD_NAME).inviterUserId(TEST_INVITER_ID)
                .inviteeEmail(TEST_EMAIL).inviteeUserId(TEST_USER_ID).token(TEST_TOKEN)
                .status(InvitationStatus.ACCEPTED).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48)).build();

        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED))
                .thenReturn(acceptedInvitation);

        mockMvc.perform(put("/board-invitations/{id}/accept", TEST_INVITATION_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response.status").value("ACCEPTED"));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED);
    }

    @Test
    void acceptInvitation_NotFound() throws Exception {
        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED))
                .thenThrow(new InvitationNotFoundException("Invitation not found with ID: " + TEST_INVITATION_ID));

        mockMvc.perform(put("/board-invitations/{id}/accept", TEST_INVITATION_ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Invitation not found with ID: " + TEST_INVITATION_ID));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED);
    }

    @Test
    void acceptInvitation_Unauthorized() throws Exception {
        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED))
                .thenThrow(new UnauthorizedAccessException("You don't have permission to update this invitation"));

        mockMvc.perform(put("/board-invitations/{id}/accept", TEST_INVITATION_ID)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("You don't have permission to update this invitation"));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.ACCEPTED);
    }

    @Test
    void declineInvitation_Success() throws Exception {
        BoardInvitationResponseDTO declinedInvitation = BoardInvitationResponseDTO.builder().id(TEST_INVITATION_ID)
                .boardId(TEST_BOARD_ID).boardName(TEST_BOARD_NAME).inviterUserId(TEST_INVITER_ID)
                .inviteeEmail(TEST_EMAIL).inviteeUserId(TEST_USER_ID).token(TEST_TOKEN)
                .status(InvitationStatus.DECLINED).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48)).build();

        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED))
                .thenReturn(declinedInvitation);

        mockMvc.perform(put("/board-invitations/{id}/decline", TEST_INVITATION_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response.status").value("DECLINED"));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED);
    }

    @Test
    void declineInvitation_NotFound() throws Exception {
        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED))
                .thenThrow(new InvitationNotFoundException("Invitation not found with ID: " + TEST_INVITATION_ID));

        mockMvc.perform(put("/board-invitations/{id}/decline", TEST_INVITATION_ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Invitation not found with ID: " + TEST_INVITATION_ID));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED);
    }

    @Test
    void declineInvitation_Unauthorized() throws Exception {
        when(boardInvitationService.updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED))
                .thenThrow(new UnauthorizedAccessException("You don't have permission to update this invitation"));

        mockMvc.perform(put("/board-invitations/{id}/decline", TEST_INVITATION_ID)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("You don't have permission to update this invitation"));

        verify(boardInvitationService, times(1)).updateInvitationStatus(TEST_INVITATION_ID, InvitationStatus.DECLINED);
    }

    @Test
    void cancelInvitation_Success() throws Exception {
        doNothing().when(boardInvitationService).cancelInvitation(TEST_INVITATION_ID);

        mockMvc.perform(delete("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(boardInvitationService, times(1)).cancelInvitation(TEST_INVITATION_ID);
    }

    @Test
    void cancelInvitation_NotFound() throws Exception {
        doThrow(new InvitationNotFoundException("Invitation not found with ID: " + TEST_INVITATION_ID))
                .when(boardInvitationService).cancelInvitation(TEST_INVITATION_ID);

        mockMvc.perform(delete("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Invitation not found with ID: " + TEST_INVITATION_ID));

        verify(boardInvitationService, times(1)).cancelInvitation(TEST_INVITATION_ID);
    }

    @Test
    void cancelInvitation_Unauthorized() throws Exception {
        doThrow(new UnauthorizedAccessException("Only the inviter or board owner can cancel this invitation"))
                .when(boardInvitationService).cancelInvitation(TEST_INVITATION_ID);

        mockMvc.perform(delete("/board-invitations/{id}", TEST_INVITATION_ID)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Only the inviter or board owner can cancel this invitation"));

        verify(boardInvitationService, times(1)).cancelInvitation(TEST_INVITATION_ID);
    }

    @Test
    void getInvitationByToken_Success() throws Exception {
        when(boardInvitationService.getInvitationByToken(TEST_TOKEN)).thenReturn(invitationResponse);

        mockMvc.perform(get("/board-invitations/token/{token}", TEST_TOKEN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(TEST_INVITATION_ID))
                .andExpect(jsonPath("$.response.token").value(TEST_TOKEN));

        verify(boardInvitationService, times(1)).getInvitationByToken(TEST_TOKEN);
    }

    @Test
    void getInvitationByToken_NotFound() throws Exception {
        when(boardInvitationService.getInvitationByToken(TEST_TOKEN))
                .thenThrow(new InvitationNotFoundException("Invalid invitation token"));

        mockMvc.perform(get("/board-invitations/token/{token}", TEST_TOKEN)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Invalid invitation token"));

        verify(boardInvitationService, times(1)).getInvitationByToken(TEST_TOKEN);
    }
}