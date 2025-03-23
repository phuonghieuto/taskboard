package com.phuonghieuto.backend.task_service.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.impl.EntityAccessControlServiceImpl;

@ExtendWith(MockitoExtension.class)
class EntityAccessControlServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private TableRepository tableRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private EntityAccessControlServiceImpl entityAccessControlService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_BOARD_ID = "test-board-id";
    private static final String TEST_TABLE_ID = "test-table-id";
    private static final String TEST_TASK_ID = "test-task-id";
    private static final String OTHER_USER_ID = "other-user-id";

    private BoardEntity boardEntity;
    private TableEntity tableEntity;
    private TaskEntity taskEntity;
    private Set<String> collaboratorIds;

    @BeforeEach
    void setUp() {
        // Set up collaborator IDs
        collaboratorIds = new HashSet<>();
        collaboratorIds.add(OTHER_USER_ID);

        // Create a board owned by TEST_USER_ID
        boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        boardEntity.setCollaboratorIds(collaboratorIds);

        // Create a table belonging to the board
        tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        // Create a task belonging to the table
        taskEntity = new TaskEntity();
        taskEntity.setId(TEST_TASK_ID);
        taskEntity.setTitle("Test Task");
        taskEntity.setTable(tableEntity);
    }

    @Test
    void findBoardAndCheckAccess_OwnerAccess_Success() {
        // Arrange
        when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.of(boardEntity));

        // Act
        BoardEntity result = entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());
        assertEquals(TEST_USER_ID, result.getOwnerId());

        // Verify repository was called
        verify(boardRepository).findById(TEST_BOARD_ID);
    }

    @Test
    void findBoardAndCheckAccess_CollaboratorAccess_Success() {
        // Arrange
        when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.of(boardEntity));

        // Act
        BoardEntity result = entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, OTHER_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());
        assertEquals(TEST_USER_ID, result.getOwnerId());
        assertEquals(collaboratorIds, result.getCollaboratorIds());

        // Verify repository was called
        verify(boardRepository).findById(TEST_BOARD_ID);
    }

    @Test
    void findBoardAndCheckAccess_BoardNotFound() {
        // Arrange
        when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class,
                () -> entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID));

        // Verify exception message and repository call
        assertEquals("Board not found", exception.getMessage());
        verify(boardRepository).findById(TEST_BOARD_ID);
    }

    @Test
    void findBoardAndCheckAccess_UnauthorizedAccess() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";
        when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.of(boardEntity));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, unauthorizedUserId));

        // Verify exception message and repository call
        assertEquals("User does not have access to this resource", exception.getMessage());
        verify(boardRepository).findById(TEST_BOARD_ID);
    }

    @Test
    void findTableAndCheckAccess_Success() {
        // Arrange
        when(tableRepository.findById(TEST_TABLE_ID)).thenReturn(Optional.of(tableEntity));

        // Act
        TableEntity result = entityAccessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals(TEST_BOARD_ID, result.getBoard().getId());

        // Verify repository was called
        verify(tableRepository).findById(TEST_TABLE_ID);
    }

    @Test
    void findTableAndCheckAccess_TableNotFound() {
        // Arrange
        when(tableRepository.findById(TEST_TABLE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        TableNotFoundException exception = assertThrows(TableNotFoundException.class,
                () -> entityAccessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID));

        // Verify exception message and repository call
        assertEquals("Table not found with ID: " + TEST_TABLE_ID, exception.getMessage());
        verify(tableRepository).findById(TEST_TABLE_ID);
    }

    @Test
    void findTableAndCheckAccess_UnauthorizedAccess() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";
        when(tableRepository.findById(TEST_TABLE_ID)).thenReturn(Optional.of(tableEntity));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> entityAccessControlService.findTableAndCheckAccess(TEST_TABLE_ID, unauthorizedUserId));

        // Verify exception message and repository call
        assertEquals("User does not have access to this resource", exception.getMessage());
        verify(tableRepository).findById(TEST_TABLE_ID);
    }

    @Test
    void findTableAndCheckAccess_CollaboratorAccess_Success() {
        // Arrange
        when(tableRepository.findById(TEST_TABLE_ID)).thenReturn(Optional.of(tableEntity));

        // Act
        TableEntity result = entityAccessControlService.findTableAndCheckAccess(TEST_TABLE_ID, OTHER_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals(TEST_BOARD_ID, result.getBoard().getId());

        // Verify repository was called
        verify(tableRepository).findById(TEST_TABLE_ID);
    }

    @Test
    void findTaskAndCheckAccess_Success() {
        // Arrange
        when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(taskEntity));

        // Act
        TaskEntity result = entityAccessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals(TEST_TABLE_ID, result.getTable().getId());
        assertEquals(TEST_BOARD_ID, result.getTable().getBoard().getId());

        // Verify repository was called
        verify(taskRepository).findById(TEST_TASK_ID);
    }

    @Test
    void findTaskAndCheckAccess_TaskNotFound() {
        // Arrange
        when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class,
                () -> entityAccessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID));

        // Verify exception message and repository call
        assertEquals("Task not found with ID: " + TEST_TASK_ID, exception.getMessage());
        verify(taskRepository).findById(TEST_TASK_ID);
    }

    @Test
    void findTaskAndCheckAccess_UnauthorizedAccess() {
        // Arrange
        String unauthorizedUserId = "unauthorized-user-id";
        when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(taskEntity));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> entityAccessControlService.findTaskAndCheckAccess(TEST_TASK_ID, unauthorizedUserId));

        // Verify exception message and repository call
        assertEquals("User does not have access to this resource", exception.getMessage());
        verify(taskRepository).findById(TEST_TASK_ID);
    }

    @Test
    void findTaskAndCheckAccess_CollaboratorAccess_Success() {
        // Arrange
        when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(taskEntity));

        // Act
        TaskEntity result = entityAccessControlService.findTaskAndCheckAccess(TEST_TASK_ID, OTHER_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals(TEST_TABLE_ID, result.getTable().getId());

        // Verify repository was called
        verify(taskRepository).findById(TEST_TASK_ID);
    }

    @Test
    void checkBoardAccess_NullBoard() {
        // Arrange
        boardEntity = null;

        // Act & Assert
        BoardNotFoundException exception = assertThrows(BoardNotFoundException.class, () -> {
            // We need to call a public method that uses checkBoardAccess internally
            // Create a board with ID but null board entity for the repository to return
            when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.empty());
            entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        });

        assertEquals("Board not found", exception.getMessage());
    }

    @Test
    void checkBoardAccess_NullCollaboratorIds() {
        // Arrange
        boardEntity.setCollaboratorIds(null);
        when(boardRepository.findById(TEST_BOARD_ID)).thenReturn(Optional.of(boardEntity));

        // Act & Assert - Owner should still have access
        BoardEntity result = entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);

        assertNotNull(result);
        assertEquals(TEST_BOARD_ID, result.getId());

        // But other user should not
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> entityAccessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, OTHER_USER_ID));

        assertEquals("User does not have access to this resource", exception.getMessage());
    }
}