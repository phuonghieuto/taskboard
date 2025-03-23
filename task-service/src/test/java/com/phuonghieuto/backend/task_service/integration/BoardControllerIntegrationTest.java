package com.phuonghieuto.backend.task_service.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.task_service.config.TestTokenConfigurationParameter;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import io.jsonwebtoken.Jwts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;

@AutoConfigureMockMvc
public class BoardControllerIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

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

  private String accessToken;
  private String otherUserAccessToken;

  @BeforeEach
  void setUp() {
    // Clean up the database before each test
    boardRepository.deleteAll();

    // Create test tokens
    accessToken = generateToken(TEST_USER_ID, TEST_USER_EMAIL);
    otherUserAccessToken = generateToken(OTHER_USER_ID, OTHER_USER_EMAIL);
  }

  public String generateToken(String userId, String userEmail) {
    final long currentTimeMillis = System.currentTimeMillis();
    final Date tokenIssuedAt = new Date(currentTimeMillis);
    final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis), 30);

    final String accessToken = Jwts.builder().setHeaderParam(TokenClaims.TYP.getValue(), "Bearer")
        .setId(UUID.randomUUID().toString()).setIssuedAt(tokenIssuedAt).setExpiration(accessTokenExpiresAt)
        .signWith(tokenConfigurationParameter.getPrivateKey()).addClaims(Map.of("userId", userId, "userEmail", userEmail)).compact();
    return accessToken;
  }

  @Test
  void createBoard_Success() throws Exception {
    // Create a board request
    BoardRequestDTO boardRequest = new BoardRequestDTO();
    boardRequest.setName("Test Board");
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add("collaborator-1");
    boardRequest.setCollaboratorIds(collaboratorIds);

    // Create the board
    mockMvc
        .perform(post("/boards").header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(boardRequest)))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.name").value("Test Board"))
        .andExpect(jsonPath("$.response.ownerId").value(TEST_USER_ID))
        .andExpect(jsonPath("$.response.collaboratorIds").isArray())
        .andExpect(jsonPath("$.response.collaboratorIds[0]").value("collaborator-1"));

    // Verify the board was created in the database
    List<BoardEntity> boards = boardRepository.findAll();
    assertEquals(1, boards.size());
    BoardEntity savedBoard = boards.get(0);
    assertEquals("Test Board", savedBoard.getName());
    assertEquals(TEST_USER_ID, savedBoard.getOwnerId());
    assertEquals(1, savedBoard.getCollaboratorIds().size());
    assertTrue(savedBoard.getCollaboratorIds().contains("collaborator-1"));
  }

  @Test
  void createBoard_InvalidRequest() throws Exception {
    // Create invalid request (empty name)
    BoardRequestDTO invalidRequest = new BoardRequestDTO();
    invalidRequest.setCollaboratorIds(new HashSet<>());
    // Name is missing

    mockMvc
        .perform(post("/boards").header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
        .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    // Verify no board was created
    assertEquals(0, boardRepository.findAll().size());
  }

  @Test
  void createBoard_Unauthorized() throws Exception {
    // Try to create a board without authentication
    BoardRequestDTO boardRequest = new BoardRequestDTO();
    boardRequest.setName("Test Board");
    boardRequest.setCollaboratorIds(new HashSet<>());

    mockMvc
        .perform(post("/boards").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(boardRequest)))
        .andDo(print()).andExpect(status().isUnauthorized());

    // Verify no board was created
    assertEquals(0, boardRepository.findAll().size());
  }

  @Test
  void getBoardById_Success() throws Exception {
    // Create a board in the database
    BoardEntity board = new BoardEntity();
    board.setName("Test Board");
    board.setOwnerId(TEST_USER_ID);
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add("collaborator-1");
    board.setCollaboratorIds(collaboratorIds);
    BoardEntity savedBoard = boardRepository.save(board);

    // Get the board by ID
    mockMvc.perform(get("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(savedBoard.getId()))
        .andExpect(jsonPath("$.response.name").value("Test Board"))
        .andExpect(jsonPath("$.response.ownerId").value(TEST_USER_ID))
        .andExpect(jsonPath("$.response.collaboratorIds").isArray())
        .andExpect(jsonPath("$.response.collaboratorIds[0]").value("collaborator-1"));
  }

  @Test
  void getBoardById_NotFound() throws Exception {
    // Try to get a non-existent board
    String nonExistentId = "non-existent-id";

    mockMvc.perform(get("/boards/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken)).andDo(print())
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void getBoardById_Unauthorized() throws Exception {
    // Create a board in the database
    BoardEntity board = new BoardEntity();
    board.setName("Test Board");
    board.setOwnerId(TEST_USER_ID);
    BoardEntity savedBoard = boardRepository.save(board);

    // Try to get the board without authentication
    mockMvc.perform(get("/boards/{id}", savedBoard.getId())).andDo(print()).andExpect(status().isUnauthorized());
  }

  @Test
  void getBoardById_AccessDenied() throws Exception {
    // Create a board owned by TEST_USER_ID
    BoardEntity board = new BoardEntity();
    board.setName("Test Board");
    board.setOwnerId(TEST_USER_ID);
    // No collaborators
    board.setCollaboratorIds(new HashSet<>());
    BoardEntity savedBoard = boardRepository.save(board);

    // Try to get the board as another user
    mockMvc.perform(get("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + otherUserAccessToken))
        .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void getMyBoards_Success() throws Exception {
    // Create multiple boards owned by the test user
    BoardEntity board1 = new BoardEntity();
    board1.setName("Board 1");
    board1.setOwnerId(TEST_USER_ID);
    board1.setCollaboratorIds(new HashSet<>());
    boardRepository.save(board1);

    BoardEntity board2 = new BoardEntity();
    board2.setName("Board 2");
    board2.setOwnerId(TEST_USER_ID);
    board2.setCollaboratorIds(new HashSet<>());
    boardRepository.save(board2);

    // Create a board owned by another user but with test user as collaborator
    BoardEntity board3 = new BoardEntity();
    board3.setName("Board 3");
    board3.setOwnerId(OTHER_USER_ID);
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add(TEST_USER_ID);
    board3.setCollaboratorIds(collaboratorIds);
    boardRepository.save(board3);

    // Create a board owned by another user with test user not as collaborator
    BoardEntity board4 = new BoardEntity();
    board4.setName("Board 4");
    board4.setOwnerId(OTHER_USER_ID);
    board4.setCollaboratorIds(new HashSet<>());
    boardRepository.save(board4);

    // Get boards for the test user
    mockMvc.perform(get("/boards").header("Authorization", "Bearer " + accessToken)).andDo(print())
        .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(3))); // Should return 3
                                                                                                    // boards (2 owned +
                                                                                                    // 1 collaborator)
  }

  @Test
  void getMyBoards_EmptyList() throws Exception {
    // No boards for the test user
    mockMvc.perform(get("/boards").header("Authorization", "Bearer " + accessToken)).andDo(print())
        .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(0)));
  }

  @Test
  void updateBoard_Success() throws Exception {
    // Create a board in the database
    BoardEntity board = new BoardEntity();
    board.setName("Original Board");
    board.setOwnerId(TEST_USER_ID);
    board.setCollaboratorIds(new HashSet<>());
    BoardEntity savedBoard = boardRepository.save(board);

    // Update request
    BoardRequestDTO updateRequest = new BoardRequestDTO();
    updateRequest.setName("Updated Board");
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add("new-collaborator");
    updateRequest.setCollaboratorIds(collaboratorIds);

    // Update the board
    mockMvc
        .perform(put("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(savedBoard.getId()))
        .andExpect(jsonPath("$.response.name").value("Updated Board"))
        .andExpect(jsonPath("$.response.collaboratorIds[0]").value("new-collaborator"));

    // Verify the board was updated in the database
    BoardEntity updatedBoard = boardRepository.findById(savedBoard.getId()).orElse(null);
    assertNotNull(updatedBoard);
    assertEquals("Updated Board", updatedBoard.getName());
    assertEquals(1, updatedBoard.getCollaboratorIds().size());
    assertTrue(updatedBoard.getCollaboratorIds().contains("new-collaborator"));
  }

  @Test
  void updateBoard_NotFound() throws Exception {
    // Try to update a non-existent board
    String nonExistentId = "non-existent-id";

    BoardRequestDTO updateRequest = new BoardRequestDTO();
    updateRequest.setName("Updated Board");
    updateRequest.setCollaboratorIds(new HashSet<>());

    mockMvc
        .perform(put("/boards/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void updateBoard_AccessDenied() throws Exception {
    // Create a board owned by OTHER_USER_ID
    BoardEntity board = new BoardEntity();
    board.setName("Other User's Board");
    board.setOwnerId(OTHER_USER_ID);
    board.setCollaboratorIds(new HashSet<>());
    BoardEntity savedBoard = boardRepository.save(board);

    // Update request
    BoardRequestDTO updateRequest = new BoardRequestDTO();
    updateRequest.setName("Trying to Update");
    updateRequest.setCollaboratorIds(new HashSet<>());

    // Try to update the board as a different user
    mockMvc
        .perform(put("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    // Verify the board was not updated
    BoardEntity unchangedBoard = boardRepository.findById(savedBoard.getId()).orElse(null);
    assertNotNull(unchangedBoard);
    assertEquals("Other User's Board", unchangedBoard.getName());
  }

  @Test
  void updateBoard_InvalidRequest() throws Exception {
    // Create a board in the database
    BoardEntity board = new BoardEntity();
    board.setName("Original Board");
    board.setOwnerId(TEST_USER_ID);
    BoardEntity savedBoard = boardRepository.save(board);

    // Create invalid request (empty name)
    BoardRequestDTO invalidRequest = new BoardRequestDTO();
    invalidRequest.setCollaboratorIds(new HashSet<>());
    // Name is missing

    mockMvc
        .perform(put("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
        .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    // Verify the board was not updated
    BoardEntity unchangedBoard = boardRepository.findById(savedBoard.getId()).orElse(null);
    assertNotNull(unchangedBoard);
    assertEquals("Original Board", unchangedBoard.getName());
  }

  @Test
  void deleteBoard_Success() throws Exception {
    // Create a board in the database
    BoardEntity board = new BoardEntity();
    board.setName("Board to Delete");
    board.setOwnerId(TEST_USER_ID);
    BoardEntity savedBoard = boardRepository.save(board);

    // Delete the board
    mockMvc.perform(delete("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));

    // Verify the board was deleted
    assertTrue(boardRepository.findById(savedBoard.getId()).isEmpty());
  }

  @Test
  void deleteBoard_NotFound() throws Exception {
    // Try to delete a non-existent board
    String nonExistentId = "non-existent-id";

    mockMvc.perform(delete("/boards/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken))
        .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void deleteBoard_AccessDenied() throws Exception {
    // Create a board owned by OTHER_USER_ID
    BoardEntity board = new BoardEntity();
    board.setName("Other User's Board");
    board.setOwnerId(OTHER_USER_ID);
    BoardEntity savedBoard = boardRepository.save(board);

    // Try to delete the board as a different user
    mockMvc.perform(delete("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + accessToken))
        .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    // Verify the board was not deleted
    assertTrue(boardRepository.findById(savedBoard.getId()).isPresent());
  }

  @Test
  void collaboratorCanAccessBoard() throws Exception {
    // Create a board owned by TEST_USER_ID with OTHER_USER as collaborator
    BoardEntity board = new BoardEntity();
    board.setName("Shared Board");
    board.setOwnerId(TEST_USER_ID);
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add(OTHER_USER_ID);
    board.setCollaboratorIds(collaboratorIds);
    BoardEntity savedBoard = boardRepository.save(board);

    // OTHER_USER should be able to access the board
    mockMvc.perform(get("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + otherUserAccessToken))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(savedBoard.getId()))
        .andExpect(jsonPath("$.response.name").value("Shared Board"));
  }

  @Test
  void collaboratorCannotDeleteBoard() throws Exception {
    // Create a board owned by TEST_USER_ID with OTHER_USER as collaborator
    BoardEntity board = new BoardEntity();
    board.setName("Shared Board");
    board.setOwnerId(TEST_USER_ID);
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add(OTHER_USER_ID);
    board.setCollaboratorIds(collaboratorIds);
    BoardEntity savedBoard = boardRepository.save(board);

    // OTHER_USER should NOT be able to delete the board
    mockMvc
        .perform(delete("/boards/{id}", savedBoard.getId()).header("Authorization", "Bearer " + otherUserAccessToken))
        .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    // Verify the board was not deleted
    assertTrue(boardRepository.findById(savedBoard.getId()).isPresent());
  }
}