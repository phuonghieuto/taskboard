package com.phuonghieuto.backend.task_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.task_service.config.TestTokenConfigurationParameter;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.request.BoardInvitationRequestDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.entity.BoardInvitationEntity;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.repository.BoardInvitationRepository;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import io.jsonwebtoken.Jwts;

@AutoConfigureMockMvc
public class BoardInvitationControllerIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private BoardInvitationRepository invitationRepository;
        
        @Autowired
        private BoardRepository boardRepository;
        
        @Autowired
        private ObjectMapper objectMapper;
        
        @Autowired
        private TestTokenConfigurationParameter tokenConfigurationParameter;
        
        private static final String TEST_USER_ID = "test-user-id";
        private static final String TEST_USER_EMAIL = "test@example.com";
        private static final String OTHER_USER_ID = "other-user-id";
        private static final String OTHER_USER_EMAIL = "other@example.com";
        private static final String INVITEE_EMAIL = "invitee@example.com";
        
        private String accessToken;
        private String otherUserAccessToken;
        private BoardEntity testBoard;
        
        @BeforeEach
        void setUp() {
                // Clean up the database before each test
                invitationRepository.deleteAll();
                boardRepository.deleteAll();
                
                // Create test tokens
                accessToken = generateToken(TEST_USER_ID, TEST_USER_EMAIL);
                otherUserAccessToken = generateToken(OTHER_USER_ID, OTHER_USER_EMAIL);
                
                // Create a test board owned by the test user
                testBoard = new BoardEntity();
                testBoard.setName("Test Board");
                testBoard.setOwnerId(TEST_USER_ID);
                testBoard.setCollaboratorIds(new HashSet<>());
                testBoard = boardRepository.save(testBoard);
        }
        
        public String generateToken(String userId, String userEmail) {
                final long currentTimeMillis = System.currentTimeMillis();
                final Date tokenIssuedAt = new Date(currentTimeMillis);
                final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis), 30);

                return Jwts.builder()
                                .setHeaderParam(TokenClaims.TYP.getValue(), "Bearer")
                                .setId(UUID.randomUUID().toString())
                                .setIssuedAt(tokenIssuedAt)
                                .setExpiration(accessTokenExpiresAt)
                                .signWith(tokenConfigurationParameter.getPrivateKey())
                                .addClaims(Map.of("userId", userId, "userEmail", userEmail))
                                .compact();
        }
        
        @Test
        void createInvitation_Success() throws Exception {
                // Create an invitation request
                BoardInvitationRequestDTO invitationRequest = new BoardInvitationRequestDTO();
                invitationRequest.setEmail(INVITEE_EMAIL);
                
                // Create the invitation
                mockMvc.perform(post("/board-invitations/board/{boardId}", testBoard.getId())
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.boardId").value(testBoard.getId()))
                                .andExpect(jsonPath("$.response.boardName").value("Test Board"))
                                .andExpect(jsonPath("$.response.inviterUserId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.response.inviteeEmail").value(INVITEE_EMAIL))
                                .andExpect(jsonPath("$.response.status").value("PENDING"));
                
                // Verify the invitation was created in the database
                List<BoardInvitationEntity> invitations = invitationRepository.findAll();
                assertEquals(1, invitations.size());
                BoardInvitationEntity savedInvitation = invitations.get(0);
                assertEquals(testBoard.getId(), savedInvitation.getBoard().getId());
                assertEquals(TEST_USER_ID, savedInvitation.getInviterUserId());
                assertEquals(INVITEE_EMAIL, savedInvitation.getInviteeEmail());
                assertEquals(InvitationStatus.PENDING, savedInvitation.getStatus());
        }
        
        @Test
        void createInvitation_InvalidRequest() throws Exception {
                // Create invalid request (empty email)
                BoardInvitationRequestDTO invalidRequest = new BoardInvitationRequestDTO();
                
                mockMvc.perform(post("/board-invitations/board/{boardId}", testBoard.getId())
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify no invitation was created
                assertEquals(0, invitationRepository.findAll().size());
        }
        
        @Test
        void createInvitation_Unauthorized() throws Exception {
                // Try to create an invitation without authentication
                BoardInvitationRequestDTO invitationRequest = new BoardInvitationRequestDTO();
                invitationRequest.setEmail(INVITEE_EMAIL);
                
                mockMvc.perform(post("/board-invitations/board/{boardId}", testBoard.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Verify no invitation was created
                assertEquals(0, invitationRepository.findAll().size());
        }
        
        @Test
        void createInvitation_BoardNotFound() throws Exception {
                // Create an invitation request for a non-existent board
                BoardInvitationRequestDTO invitationRequest = new BoardInvitationRequestDTO();
                invitationRequest.setEmail(INVITEE_EMAIL);
                
                mockMvc.perform(post("/board-invitations/board/{boardId}", "non-existent-board-id")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify no invitation was created
                assertEquals(0, invitationRepository.findAll().size());
        }
        
        @Test
        void createInvitation_DuplicateInvitation() throws Exception {
                // Create an initial invitation
                BoardInvitationEntity existingInvitation = new BoardInvitationEntity();
                existingInvitation.setBoard(testBoard);
                existingInvitation.setInviterUserId(TEST_USER_ID);
                existingInvitation.setInviteeEmail(INVITEE_EMAIL);
                existingInvitation.setStatus(InvitationStatus.PENDING);
                existingInvitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(existingInvitation);
                
                // Try to create a duplicate invitation
                BoardInvitationRequestDTO invitationRequest = new BoardInvitationRequestDTO();
                invitationRequest.setEmail(INVITEE_EMAIL);
                
                mockMvc.perform(post("/board-invitations/board/{boardId}", testBoard.getId())
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify only one invitation exists
                assertEquals(1, invitationRepository.findAll().size());
        }
        
        @Test
        void createInvitation_AccessDenied() throws Exception {
                // Create a board owned by OTHER_USER_ID
                BoardEntity otherBoard = new BoardEntity();
                otherBoard.setName("Other User's Board");
                otherBoard.setOwnerId(OTHER_USER_ID);
                otherBoard.setCollaboratorIds(new HashSet<>());
                otherBoard = boardRepository.save(otherBoard);
                
                // Try to create an invitation for a board the user doesn't own
                BoardInvitationRequestDTO invitationRequest = new BoardInvitationRequestDTO();
                invitationRequest.setEmail(INVITEE_EMAIL);
                
                mockMvc.perform(post("/board-invitations/board/{boardId}", otherBoard.getId())
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invitationRequest)))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify no invitation was created
                assertEquals(0, invitationRepository.findAll().size());
        }
        
        @Test
        void getInvitation_Success() throws Exception {
                // Create an invitation in the database
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Get the invitation by ID
                mockMvc.perform(get("/board-invitations/{id}", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.id").value(invitation.getId()))
                                .andExpect(jsonPath("$.response.boardId").value(testBoard.getId()))
                                .andExpect(jsonPath("$.response.inviterUserId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.response.inviteeEmail").value(INVITEE_EMAIL))
                                .andExpect(jsonPath("$.response.status").value("PENDING"));
        }
        
        @Test
        void getInvitation_NotFound() throws Exception {
                // Try to get a non-existent invitation
                String nonExistentId = "non-existent-id";
                
                mockMvc.perform(get("/board-invitations/{id}", nonExistentId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void getInvitation_Unauthorized() throws Exception {
                // Create an invitation in the database
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Try to get the invitation without authentication
                mockMvc.perform(get("/board-invitations/{id}", invitation.getId()))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
        }
        
        @Test
        void getPendingInvitationsForBoard_Success() throws Exception {
                // Create multiple invitations for the test board
                BoardInvitationEntity invitation1 = new BoardInvitationEntity();
                invitation1.setBoard(testBoard);
                invitation1.setInviterUserId(TEST_USER_ID);
                invitation1.setInviteeEmail("invitee1@example.com");
                invitation1.setStatus(InvitationStatus.PENDING);
                invitation1.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(invitation1);
                
                BoardInvitationEntity invitation2 = new BoardInvitationEntity();
                invitation2.setBoard(testBoard);
                invitation2.setInviterUserId(TEST_USER_ID);
                invitation2.setInviteeEmail("invitee2@example.com");
                invitation2.setStatus(InvitationStatus.PENDING);
                invitation2.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(invitation2);
                
                // Create an accepted invitation (should not be returned)
                BoardInvitationEntity acceptedInvitation = new BoardInvitationEntity();
                acceptedInvitation.setBoard(testBoard);
                acceptedInvitation.setInviterUserId(TEST_USER_ID);
                acceptedInvitation.setInviteeEmail("accepted@example.com");
                acceptedInvitation.setStatus(InvitationStatus.ACCEPTED);
                acceptedInvitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(acceptedInvitation);
                
                // Get pending invitations for the board
                mockMvc.perform(get("/board-invitations/board/{boardId}", testBoard.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response").isArray())
                                .andExpect(jsonPath("$.response", hasSize(2)))
                                .andExpect(jsonPath("$.response[0].inviteeEmail").value("invitee1@example.com"))
                                .andExpect(jsonPath("$.response[1].inviteeEmail").value("invitee2@example.com"));
        }
        
        @Test
        void getPendingInvitationsForBoard_EmptyList() throws Exception {
                // No invitations for the test board
                mockMvc.perform(get("/board-invitations/board/{boardId}", testBoard.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response").isArray())
                                .andExpect(jsonPath("$.response", hasSize(0)));
        }
        
        @Test
        void getPendingInvitationsForBoard_BoardNotFound() throws Exception {
                // Try to get invitations for a non-existent board
                mockMvc.perform(get("/board-invitations/board/{boardId}", "non-existent-board-id")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void getPendingInvitationsForBoard_AccessDenied() throws Exception {
                // Create a board owned by OTHER_USER_ID
                BoardEntity otherBoard = new BoardEntity();
                otherBoard.setName("Other User's Board");
                otherBoard.setOwnerId(OTHER_USER_ID);
                otherBoard.setCollaboratorIds(new HashSet<>());
                otherBoard = boardRepository.save(otherBoard);
                
                // Try to get invitations for a board the user doesn't own
                mockMvc.perform(get("/board-invitations/board/{boardId}", otherBoard.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void getMyPendingInvitations_Success() throws Exception {
                // Create invitations for the test user
                BoardInvitationEntity invitation1 = new BoardInvitationEntity();
                invitation1.setBoard(testBoard);
                invitation1.setInviterUserId(OTHER_USER_ID);
                invitation1.setInviteeEmail(TEST_USER_EMAIL);
                invitation1.setStatus(InvitationStatus.PENDING);
                invitation1.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(invitation1);
                
                BoardEntity otherBoard = new BoardEntity();
                otherBoard.setName("Other User's Board");
                otherBoard.setOwnerId(OTHER_USER_ID);
                otherBoard.setCollaboratorIds(new HashSet<>());
                otherBoard = boardRepository.save(otherBoard);
                
                BoardInvitationEntity invitation2 = new BoardInvitationEntity();
                invitation2.setBoard(otherBoard);
                invitation2.setInviterUserId(OTHER_USER_ID);
                invitation2.setInviteeEmail(TEST_USER_EMAIL);
                invitation2.setStatus(InvitationStatus.PENDING);
                invitation2.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitationRepository.save(invitation2);
                
                // Create an invitation for another user
                BoardInvitationEntity otherInvitation = new BoardInvitationEntity();
                otherInvitation.setBoard(testBoard);
                otherInvitation.setInviterUserId(TEST_USER_ID);
                otherInvitation.setInviteeEmail(OTHER_USER_EMAIL);
                otherInvitation.setStatus(InvitationStatus.PENDING);
                otherInvitation.setExpiresAt(LocalDateTime.now().plusDays(2));                
                invitationRepository.save(otherInvitation);
                
                // Get pending invitations for the test user
                mockMvc.perform(get("/board-invitations/my-invitations")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response").isArray())
                                .andExpect(jsonPath("$.response", hasSize(2)));
        }
        
        @Test
        void getMyPendingInvitations_EmptyList() throws Exception {
                // No invitations for the test user
                mockMvc.perform(get("/board-invitations/my-invitations")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response").isArray())
                                .andExpect(jsonPath("$.response", hasSize(0)));
        }
        
        @Test
        void acceptInvitation_Success() throws Exception {
                // Create an invitation for the test user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(OTHER_USER_ID);
                invitation.setInviteeEmail(TEST_USER_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Accept the invitation
                mockMvc.perform(put("/board-invitations/{id}/accept", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.id").value(invitation.getId()))
                                .andExpect(jsonPath("$.response.status").value("ACCEPTED"));
                
                // Verify the invitation was updated in the database
                BoardInvitationEntity updatedInvitation = invitationRepository.findById(invitation.getId()).orElse(null);
                assertNotNull(updatedInvitation);
                assertEquals(InvitationStatus.ACCEPTED, updatedInvitation.getStatus());
                
                // Verify the user was added as a collaborator to the board
                BoardEntity updatedBoard = boardRepository.findById(testBoard.getId()).orElse(null);
                assertNotNull(updatedBoard);
                assertTrue(updatedBoard.getCollaboratorIds().contains(TEST_USER_ID));
        }
        
        @Test
        void acceptInvitation_NotFound() throws Exception {
                // Try to accept a non-existent invitation
                String nonExistentId = "non-existent-id";
                
                mockMvc.perform(put("/board-invitations/{id}/accept", nonExistentId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void acceptInvitation_Unauthorized() throws Exception {
                // Create an invitation for another user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(OTHER_USER_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Try to accept an invitation meant for another user
                mockMvc.perform(put("/board-invitations/{id}/accept", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify the invitation was not updated
                BoardInvitationEntity unchangedInvitation = invitationRepository.findById(invitation.getId()).orElse(null);
                assertNotNull(unchangedInvitation);
                assertEquals(InvitationStatus.PENDING, unchangedInvitation.getStatus());
        }
        
        @Test
        void declineInvitation_Success() throws Exception {
                // Create an invitation for the test user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(OTHER_USER_ID);
                invitation.setInviteeEmail(TEST_USER_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Decline the invitation
                mockMvc.perform(put("/board-invitations/{id}/decline", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.id").value(invitation.getId()))
                                .andExpect(jsonPath("$.response.status").value("DECLINED"));
                
                // Verify the invitation was updated in the database
                BoardInvitationEntity updatedInvitation = invitationRepository.findById(invitation.getId()).orElse(null);
                assertNotNull(updatedInvitation);
                assertEquals(InvitationStatus.DECLINED, updatedInvitation.getStatus());
                
                // Verify the user was NOT added as a collaborator to the board
                BoardEntity updatedBoard = boardRepository.findById(testBoard.getId()).orElse(null);
                assertNotNull(updatedBoard);
                assertTrue(updatedBoard.getCollaboratorIds().isEmpty());
        }
        
        @Test
        void declineInvitation_NotFound() throws Exception {
                // Try to decline a non-existent invitation
                String nonExistentId = "non-existent-id";
                
                mockMvc.perform(put("/board-invitations/{id}/decline", nonExistentId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void declineInvitation_Unauthorized() throws Exception {
                // Create an invitation for another user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(OTHER_USER_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Try to decline an invitation meant for another user
                mockMvc.perform(put("/board-invitations/{id}/decline", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify the invitation was not updated
                BoardInvitationEntity unchangedInvitation = invitationRepository.findById(invitation.getId()).orElse(null);
                assertNotNull(unchangedInvitation);
                assertEquals(InvitationStatus.PENDING, unchangedInvitation.getStatus());
        }
        
        @Test
        void cancelInvitation_Success() throws Exception {
                // Create an invitation by the test user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Cancel the invitation
                mockMvc.perform(delete("/board-invitations/{id}", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true));
                
                // Verify the invitation was deleted from the database
                assertTrue(invitationRepository.findById(invitation.getId()).isEmpty());
        }
        
        @Test
        void cancelInvitation_NotFound() throws Exception {
                // Try to cancel a non-existent invitation
                String nonExistentId = "non-existent-id";
                
                mockMvc.perform(delete("/board-invitations/{id}", nonExistentId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void cancelInvitation_Unauthorized() throws Exception {
                // Create a board owned by OTHER_USER_ID
                BoardEntity otherBoard = new BoardEntity();
                otherBoard.setName("Other User's Board");
                otherBoard.setOwnerId(OTHER_USER_ID);
                otherBoard.setCollaboratorIds(new HashSet<>());
                otherBoard = boardRepository.save(otherBoard);
                
                // Create an invitation from another user
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(otherBoard);
                invitation.setInviterUserId(OTHER_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Try to cancel an invitation created by another user
                mockMvc.perform(delete("/board-invitations/{id}", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify the invitation was not deleted
                assertTrue(invitationRepository.findById(invitation.getId()).isPresent());
        }
        
        @Test
        void getInvitationByToken_Success() throws Exception {
                // Create an invitation with a token
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(testBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setToken("test-token");
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Get the invitation by token
                mockMvc.perform(get("/board-invitations/token/{token}", "test-token"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.id").value(invitation.getId()))
                                .andExpect(jsonPath("$.response.token").value("test-token"));
        }
        
        @Test
        void getInvitationByToken_NotFound() throws Exception {
                // Try to get an invitation with a non-existent token
                String nonExistentToken = "non-existent-token";
                
                mockMvc.perform(get("/board-invitations/token/{token}", nonExistentToken))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }
        
        @Test
        void getInvitationByToken_Expired() throws Exception {
                // Create an expired invitation
                BoardInvitationEntity expiredInvitation = new BoardInvitationEntity();
                expiredInvitation.setBoard(testBoard);
                expiredInvitation.setInviterUserId(TEST_USER_ID);
                expiredInvitation.setInviteeEmail(INVITEE_EMAIL);
                expiredInvitation.setStatus(InvitationStatus.PENDING);
                expiredInvitation.setToken("expired-token");
                expiredInvitation.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired
                expiredInvitation = invitationRepository.save(expiredInvitation);
                
                // Try to get the expired invitation
                mockMvc.perform(get("/board-invitations/token/{token}", "expired-token"))
                                .andDo(print())
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify the invitation status was updated to EXPIRED
                BoardInvitationEntity updatedInvitation = invitationRepository.findById(expiredInvitation.getId()).orElse(null);
                assertNotNull(updatedInvitation);
                assertEquals(InvitationStatus.EXPIRED, updatedInvitation.getStatus());
        }
        
        @Test
        void boardOwnerCanAccessInvitations() throws Exception {
                // Create a board with a collaborator
                Set<String> collaborators = new HashSet<>();
                collaborators.add(OTHER_USER_ID);
                
                BoardEntity collaborativeBoard = new BoardEntity();
                collaborativeBoard.setName("Collaborative Board");
                collaborativeBoard.setOwnerId(TEST_USER_ID);
                collaborativeBoard.setCollaboratorIds(collaborators);
                collaborativeBoard = boardRepository.save(collaborativeBoard);
                
                // Create an invitation for the collaborative board by a collaborator
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(collaborativeBoard);
                invitation.setInviterUserId(OTHER_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Board owner should be able to view the invitation
                mockMvc.perform(get("/board-invitations/{id}", invitation.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.response.id").value(invitation.getId()));
        }
        
        @Test
        void collaboratorCannotCancelOtherUserInvitations() throws Exception {
                // Create a board with a collaborator
                Set<String> collaborators = new HashSet<>();
                collaborators.add(OTHER_USER_ID);
                
                BoardEntity collaborativeBoard = new BoardEntity();
                collaborativeBoard.setName("Collaborative Board");
                collaborativeBoard.setOwnerId(TEST_USER_ID);
                collaborativeBoard.setCollaboratorIds(collaborators);
                collaborativeBoard = boardRepository.save(collaborativeBoard);
                
                // Create an invitation by the owner
                BoardInvitationEntity invitation = new BoardInvitationEntity();
                invitation.setBoard(collaborativeBoard);
                invitation.setInviterUserId(TEST_USER_ID);
                invitation.setInviteeEmail(INVITEE_EMAIL);
                invitation.setStatus(InvitationStatus.PENDING);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(2));
                invitation = invitationRepository.save(invitation);
                
                // Collaborator should not be able to cancel another user's invitation
                mockMvc.perform(delete("/board-invitations/{id}", invitation.getId())
                                .header("Authorization", "Bearer " + otherUserAccessToken))
                                .andDo(print())
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").exists());
                
                // Verify the invitation was not deleted
                assertTrue(invitationRepository.findById(invitation.getId()).isPresent());
        }
}