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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.task_service.controller.BoardController;
import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.service.BoardService;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private BoardService boardService;

  @Mock
  private Authentication authentication;

  @Mock
  private Jwt jwt;

  @InjectMocks
  private BoardController boardController;

  private static final String TEST_USER_ID = "test-user-id";
  private static final String TEST_BOARD_ID = "test-board-id";
  private BoardRequestDTO boardRequest;
  private BoardResponseDTO boardResponse;
  private List<BoardResponseDTO> boardResponseList;

  @BeforeEach
  void setUp() {
    // Set up MockMvc with exception handler
    mockMvc = MockMvcBuilders.standaloneSetup(boardController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
        
    objectMapper = new ObjectMapper();

    // Setup test data
    Set<String> collaboratorIds = new HashSet<>();
    collaboratorIds.add("collaborator-1");

    // Create request DTO
    boardRequest = new BoardRequestDTO();
    boardRequest.setName("Test Board");
    boardRequest.setCollaboratorIds(collaboratorIds);

    // Create response DTO
    boardResponse = new BoardResponseDTO();
    boardResponse.setId(TEST_BOARD_ID);
    boardResponse.setName("Test Board");
    boardResponse.setOwnerId(TEST_USER_ID);
    boardResponse.setCollaboratorIds(collaboratorIds);

    // Create second board for list tests
    BoardResponseDTO boardResponse2 = new BoardResponseDTO();
    boardResponse2.setId("board-id-2");
    boardResponse2.setName("Second Board");
    boardResponse2.setOwnerId(TEST_USER_ID);
    
    // Create list of boards
    boardResponseList = Arrays.asList(boardResponse, boardResponse2);

    // Setup authentication mock
    
  }

  @Test
  void createBoard_Success() throws Exception {
    when(boardService.createBoard(any(BoardRequestDTO.class))).thenReturn(boardResponse);

    mockMvc.perform(post("/boards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(boardRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(TEST_BOARD_ID))
        .andExpect(jsonPath("$.response.name").value("Test Board"))
        .andExpect(jsonPath("$.response.ownerId").value(TEST_USER_ID))
        .andExpect(jsonPath("$.response.collaboratorIds").isArray())
        .andExpect(jsonPath("$.response.collaboratorIds[0]").value("collaborator-1"));

    verify(boardService, times(1)).createBoard(any(BoardRequestDTO.class));
  }

  @Test
  void createBoard_InvalidRequest() throws Exception {
    // Create invalid request (empty name)
    BoardRequestDTO invalidRequest = new BoardRequestDTO();
    invalidRequest.setCollaboratorIds(new HashSet<>());
    // Name is missing

    mockMvc.perform(post("/boards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    verify(boardService, times(0)).createBoard(any(BoardRequestDTO.class));
  }

  @Test
  void getBoardById_Success() throws Exception {
    when(boardService.getBoardById(TEST_BOARD_ID)).thenReturn(boardResponse);

    mockMvc.perform(get("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(TEST_BOARD_ID))
        .andExpect(jsonPath("$.response.name").value("Test Board"))
        .andExpect(jsonPath("$.response.ownerId").value(TEST_USER_ID));

    verify(boardService, times(1)).getBoardById(TEST_BOARD_ID);
  }

  @Test
  void getBoardById_NotFound() throws Exception {
    when(boardService.getBoardById(TEST_BOARD_ID))
        .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

    mockMvc.perform(get("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

    verify(boardService, times(1)).getBoardById(TEST_BOARD_ID);
  }

  @Test
  void getBoardById_Unauthorized() throws Exception {
    when(boardService.getBoardById(TEST_BOARD_ID))
        .thenThrow(new UnauthorizedAccessException("User does not have access to this resource"));

    mockMvc.perform(get("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("User does not have access to this resource"));

    verify(boardService, times(1)).getBoardById(TEST_BOARD_ID);
  }

  @Test
  void getMyBoards_Success() throws Exception {
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
    when(boardService.getAllBoardsByUserId(TEST_USER_ID)).thenReturn(boardResponseList);

    mockMvc.perform(get("/boards")
        .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response", hasSize(2)))
        .andExpect(jsonPath("$.response[0].id").value(TEST_BOARD_ID))
        .andExpect(jsonPath("$.response[0].name").value("Test Board"))
        .andExpect(jsonPath("$.response[1].id").value("board-id-2"))
        .andExpect(jsonPath("$.response[1].name").value("Second Board"));

    verify(authentication, times(1)).getPrincipal();
    verify(jwt, times(1)).getClaim("userId");
    verify(boardService, times(1)).getAllBoardsByUserId(TEST_USER_ID);
  }

  @Test
  void getMyBoards_EmptyList() throws Exception {
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
    when(boardService.getAllBoardsByUserId(TEST_USER_ID)).thenReturn(List.of());

    mockMvc.perform(get("/boards")
        .principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response", hasSize(0)));

    verify(authentication, times(1)).getPrincipal();
    verify(jwt, times(1)).getClaim("userId");
    verify(boardService, times(1)).getAllBoardsByUserId(TEST_USER_ID);
  }

  @Test
  void getMyBoards_ServiceError() throws Exception {
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
    when(boardService.getAllBoardsByUserId(TEST_USER_ID))
        .thenThrow(new RuntimeException("Error occurred while fetching boards"));

    mockMvc.perform(get("/boards")
        .principal(authentication))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("Error occurred while fetching boards"));

    verify(authentication, times(1)).getPrincipal();
    verify(jwt, times(1)).getClaim("userId");
    verify(boardService, times(1)).getAllBoardsByUserId(TEST_USER_ID);
  }

  @Test
  void updateBoard_Success() throws Exception {
    when(boardService.updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class))).thenReturn(boardResponse);

    mockMvc.perform(put("/boards/{id}", TEST_BOARD_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(boardRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response.id").value(TEST_BOARD_ID))
        .andExpect(jsonPath("$.response.name").value("Test Board"))
        .andExpect(jsonPath("$.response.ownerId").value(TEST_USER_ID));

    verify(boardService, times(1)).updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class));
  }

  @Test
  void updateBoard_NotFound() throws Exception {
    when(boardService.updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class)))
        .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

    mockMvc.perform(put("/boards/{id}", TEST_BOARD_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(boardRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

    verify(boardService, times(1)).updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class));
  }

  @Test
  void updateBoard_Unauthorized() throws Exception {
    when(boardService.updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class)))
        .thenThrow(new UnauthorizedAccessException("User does not have access to this resource"));

    mockMvc.perform(put("/boards/{id}", TEST_BOARD_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(boardRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("User does not have access to this resource"));

    verify(boardService, times(1)).updateBoard(eq(TEST_BOARD_ID), any(BoardRequestDTO.class));
  }

  @Test
  void updateBoard_InvalidRequest() throws Exception {
    // Create invalid request (empty name)
    BoardRequestDTO invalidRequest = new BoardRequestDTO();
    invalidRequest.setCollaboratorIds(new HashSet<>());
    // Name is missing

    mockMvc.perform(put("/boards/{id}", TEST_BOARD_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    verify(boardService, times(0)).updateBoard(anyString(), any(BoardRequestDTO.class));
  }

  @Test
  void deleteBoard_Success() throws Exception {
    doNothing().when(boardService).deleteBoard(TEST_BOARD_ID);

    mockMvc.perform(delete("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.response").doesNotExist());

    verify(boardService, times(1)).deleteBoard(TEST_BOARD_ID);
  }

  @Test
  void deleteBoard_NotFound() throws Exception {
    doThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID))
        .when(boardService).deleteBoard(TEST_BOARD_ID);

    mockMvc.perform(delete("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

    verify(boardService, times(1)).deleteBoard(TEST_BOARD_ID);
  }

  @Test
  void deleteBoard_Unauthorized() throws Exception {
    doThrow(new UnauthorizedAccessException("Only the owner can delete this board"))
        .when(boardService).deleteBoard(TEST_BOARD_ID);

    mockMvc.perform(delete("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").value("Only the owner can delete this board"));

    verify(boardService, times(1)).deleteBoard(TEST_BOARD_ID);
  }

  @Test
  void deleteBoard_InternalServerError() throws Exception {
    doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"))
        .when(boardService).deleteBoard(TEST_BOARD_ID);

    mockMvc.perform(delete("/boards/{id}", TEST_BOARD_ID))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.message").exists());

    verify(boardService, times(1)).deleteBoard(TEST_BOARD_ID);
  }
}