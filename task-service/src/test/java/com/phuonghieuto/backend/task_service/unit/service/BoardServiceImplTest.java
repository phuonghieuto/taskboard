package com.phuonghieuto.backend.task_service.unit.service;

import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardEntityToBoardResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardRequestToBoardEntityMapper;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.impl.BoardServiceImpl;
import com.phuonghieuto.backend.task_service.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private EntityAccessControlService accessControlService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private BoardRequestToBoardEntityMapper boardRequestToBoardEntityMapper;

    @Mock
    private BoardEntityToBoardResponseMapper boardEntityToBoardResponseMapper;

    private BoardServiceImpl boardService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_BOARD_ID = "test-board-id";

    @BeforeEach
    void setUp() {
        // Use mocked static methods for mappers
        try (MockedStatic<BoardRequestToBoardEntityMapper> mockedRequestMapper = Mockito
                .mockStatic(BoardRequestToBoardEntityMapper.class);
                MockedStatic<BoardEntityToBoardResponseMapper> mockedResponseMapper = Mockito
                        .mockStatic(BoardEntityToBoardResponseMapper.class)) {

            mockedRequestMapper.when(BoardRequestToBoardEntityMapper::initialize)
                    .thenReturn(boardRequestToBoardEntityMapper);

            mockedResponseMapper.when(BoardEntityToBoardResponseMapper::initialize)
                    .thenReturn(boardEntityToBoardResponseMapper);

            boardService = new BoardServiceImpl(boardRepository, accessControlService, authUtils);
        }
    }

    @Test
    void createBoard_Success() {
        // Arrange
        BoardRequestDTO boardRequest = new BoardRequestDTO();
        boardRequest.setName("Test Board");
        Set<String> collaboratorIds = new HashSet<>();
        collaboratorIds.add("collaborator-1");
        boardRequest.setCollaboratorIds(collaboratorIds);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        boardEntity.setCollaboratorIds(collaboratorIds);

        BoardResponseDTO expectedResponse = new BoardResponseDTO();
        expectedResponse.setId(TEST_BOARD_ID);
        expectedResponse.setName("Test Board");
        expectedResponse.setOwnerId(TEST_USER_ID);
        expectedResponse.setCollaboratorIds(collaboratorIds);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(boardRequestToBoardEntityMapper.mapForCreation(boardRequest, TEST_USER_ID)).thenReturn(boardEntity);
        when(boardRepository.save(boardEntity)).thenReturn(boardEntity);
        when(boardEntityToBoardResponseMapper.map(boardEntity)).thenReturn(expectedResponse);

        // Act
        BoardResponseDTO result = boardService.createBoard(boardRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());
        assertEquals("Test Board", result.getName());
        assertEquals(TEST_USER_ID, result.getOwnerId());
        assertEquals(collaboratorIds, result.getCollaboratorIds());

        verify(authUtils).getCurrentUserId();
        verify(boardRequestToBoardEntityMapper).mapForCreation(boardRequest, TEST_USER_ID);
        verify(boardRepository).save(boardEntity);
        verify(boardEntityToBoardResponseMapper).map(boardEntity);
    }

    @Test
    void getBoardById_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        BoardResponseDTO expectedResponse = new BoardResponseDTO();
        expectedResponse.setId(TEST_BOARD_ID);
        expectedResponse.setName("Test Board");
        expectedResponse.setOwnerId(TEST_USER_ID);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(boardEntityToBoardResponseMapper.map(boardEntity)).thenReturn(expectedResponse);

        // Act
        BoardResponseDTO result = boardService.getBoardById(TEST_BOARD_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());
        assertEquals("Test Board", result.getName());
        assertEquals(TEST_USER_ID, result.getOwnerId());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardEntityToBoardResponseMapper).map(boardEntity);
    }

    @Test
    void getAllBoardsByUserId_Success() {
        // Arrange
        BoardEntity board1 = new BoardEntity();
        board1.setId("board-1");
        board1.setName("Board 1");
        board1.setOwnerId(TEST_USER_ID);

        BoardEntity board2 = new BoardEntity();
        board2.setId("board-2");
        board2.setName("Board 2");
        board2.setOwnerId("other-user");
        board2.setCollaboratorIds(Set.of(TEST_USER_ID));

        List<BoardEntity> boardEntities = Arrays.asList(board1, board2);

        BoardResponseDTO response1 = new BoardResponseDTO();
        response1.setId("board-1");
        response1.setName("Board 1");
        response1.setOwnerId(TEST_USER_ID);

        BoardResponseDTO response2 = new BoardResponseDTO();
        response2.setId("board-2");
        response2.setName("Board 2");
        response2.setOwnerId("other-user");
        response2.setCollaboratorIds(Set.of(TEST_USER_ID));

        when(boardRepository.findByOwnerIdOrCollaboratorIdsContains(TEST_USER_ID, TEST_USER_ID))
                .thenReturn(boardEntities);
        when(boardEntityToBoardResponseMapper.map(board1)).thenReturn(response1);
        when(boardEntityToBoardResponseMapper.map(board2)).thenReturn(response2);

        // Act
        List<BoardResponseDTO> result = boardService.getAllBoardsByUserId(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("board-1", result.get(0).getId());
        assertEquals("board-2", result.get(1).getId());

        verify(boardRepository).findByOwnerIdOrCollaboratorIdsContains(TEST_USER_ID, TEST_USER_ID);
        verify(boardEntityToBoardResponseMapper).map(board1);
        verify(boardEntityToBoardResponseMapper).map(board2);
    }

    @Test
    void getAllBoardsByUserId_DatabaseError_ThrowsException() {
        // Arrange
        when(boardRepository.findByOwnerIdOrCollaboratorIdsContains(TEST_USER_ID, TEST_USER_ID))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            boardService.getAllBoardsByUserId(TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("Error occurred while fetching boards for user"));
        verify(boardRepository).findByOwnerIdOrCollaboratorIdsContains(TEST_USER_ID, TEST_USER_ID);
    }

    @Test
    void updateBoard_Success() {
        // Arrange
        BoardRequestDTO updateRequest = new BoardRequestDTO();
        updateRequest.setName("Updated Board");
        Set<String> collaboratorIds = new HashSet<>();
        collaboratorIds.add("collaborator-1");
        collaboratorIds.add("collaborator-2");
        updateRequest.setCollaboratorIds(collaboratorIds);

        BoardEntity existingBoard = new BoardEntity();
        existingBoard.setId(TEST_BOARD_ID);
        existingBoard.setName("Original Board");
        existingBoard.setOwnerId(TEST_USER_ID);
        existingBoard.setCollaboratorIds(new HashSet<>());

        BoardEntity updatedBoard = new BoardEntity();
        updatedBoard.setId(TEST_BOARD_ID);
        updatedBoard.setName("Updated Board");
        updatedBoard.setOwnerId(TEST_USER_ID);
        updatedBoard.setCollaboratorIds(collaboratorIds);

        BoardResponseDTO expectedResponse = new BoardResponseDTO();
        expectedResponse.setId(TEST_BOARD_ID);
        expectedResponse.setName("Updated Board");
        expectedResponse.setOwnerId(TEST_USER_ID);
        expectedResponse.setCollaboratorIds(collaboratorIds);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(existingBoard);
        when(boardRepository.save(any(BoardEntity.class))).thenReturn(updatedBoard);
        when(boardEntityToBoardResponseMapper.map(updatedBoard)).thenReturn(expectedResponse);

        // Act
        BoardResponseDTO result = boardService.updateBoard(TEST_BOARD_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());
        assertEquals("Updated Board", result.getName());
        assertEquals(TEST_USER_ID, result.getOwnerId());
        assertEquals(collaboratorIds, result.getCollaboratorIds());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardRepository).save(any(BoardEntity.class));
        verify(boardEntityToBoardResponseMapper).map(updatedBoard);
    }

    @Test
    void deleteBoard_ByOwner_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);

        // Act
        boardService.deleteBoard(TEST_BOARD_ID);

        // Assert
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardRepository).delete(boardEntity);
    }

    @Test
    void deleteBoard_ByCollaborator_ThrowsUnauthorizedException() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId("other-owner-id");
        boardEntity.setCollaboratorIds(Set.of(TEST_USER_ID));

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            boardService.deleteBoard(TEST_BOARD_ID);
        });

        assertEquals("Only the owner can delete this board", exception.getMessage());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(boardRepository, never()).delete(any());
    }
}