package com.phuonghieuto.backend.task_service.unit.service;

import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.TableEntityToTableResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TableRequestToTableEntityMapper;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.impl.TableServiceImpl;
import com.phuonghieuto.backend.task_service.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private EntityAccessControlService accessControlService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private TableRequestToTableEntityMapper tableRequestToTableEntityMapper;

    @Mock
    private TableEntityToTableResponseMapper tableEntityToTableResponseMapper;

    private TableServiceImpl tableService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_BOARD_ID = "test-board-id";
    private static final String TEST_TABLE_ID = "test-table-id";

    @BeforeEach
    void setUp() {
        // Use mocked static methods for mappers
        try (MockedStatic<TableRequestToTableEntityMapper> mockedRequestMapper = Mockito
                .mockStatic(TableRequestToTableEntityMapper.class);
                MockedStatic<TableEntityToTableResponseMapper> mockedResponseMapper = Mockito
                        .mockStatic(TableEntityToTableResponseMapper.class)) {

            mockedRequestMapper.when(TableRequestToTableEntityMapper::initialize)
                    .thenReturn(tableRequestToTableEntityMapper);

            mockedResponseMapper.when(TableEntityToTableResponseMapper::initialize)
                    .thenReturn(tableEntityToTableResponseMapper);

            tableService = new TableServiceImpl(tableRepository, accessControlService, authUtils);
        }
    }

    @Test
    void createTable_WithNoOrderIndex_Success() {
        // Arrange
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setBoardId(TEST_BOARD_ID);
        tableRequest.setOrderIndex(0); // No order index specified

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        List<TableEntity> existingTables = Arrays.asList(createTableEntity("table-1", 1),
                createTableEntity("table-2", 2));

        TableEntity createdTableEntity = new TableEntity();
        createdTableEntity.setId(TEST_TABLE_ID);
        createdTableEntity.setName("Test Table");
        createdTableEntity.setOrderIndex(3); // Should be 3 (after existing tables)
        createdTableEntity.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Test Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(3);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID)).thenReturn(existingTables);
        when(tableRequestToTableEntityMapper.mapForCreation(any(TableRequestDTO.class), eq(boardEntity)))
                .thenReturn(createdTableEntity);
        when(tableRepository.save(createdTableEntity)).thenReturn(createdTableEntity);
        when(tableEntityToTableResponseMapper.map(createdTableEntity)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.createTable(tableRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Test Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(3, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(tableRepository).findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID);
        verify(tableRequestToTableEntityMapper).mapForCreation(any(TableRequestDTO.class), eq(boardEntity));
        verify(tableRepository).save(createdTableEntity);
        verify(tableEntityToTableResponseMapper).map(createdTableEntity);
    }

    @Test
    void createTable_WithNoExistingTables_Success() {
        // Arrange
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setBoardId(TEST_BOARD_ID);
        tableRequest.setOrderIndex(0); // No order index specified

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        List<TableEntity> existingTables = Collections.emptyList(); // No existing tables

        TableEntity createdTableEntity = new TableEntity();
        createdTableEntity.setId(TEST_TABLE_ID);
        createdTableEntity.setName("Test Table");
        createdTableEntity.setOrderIndex(1); // Should be 1 (first table)
        createdTableEntity.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Test Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(1);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID)).thenReturn(existingTables);
        when(tableRequestToTableEntityMapper.mapForCreation(any(TableRequestDTO.class), eq(boardEntity)))
                .thenReturn(createdTableEntity);
        when(tableRepository.save(createdTableEntity)).thenReturn(createdTableEntity);
        when(tableEntityToTableResponseMapper.map(createdTableEntity)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.createTable(tableRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Test Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(1, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(tableRepository).findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID);
        verify(tableRequestToTableEntityMapper).mapForCreation(any(TableRequestDTO.class), eq(boardEntity));
        verify(tableRepository).save(createdTableEntity);
        verify(tableEntityToTableResponseMapper).map(createdTableEntity);
    }

    @Test
    void createTable_WithSpecifiedOrderIndex_Success() {
        // Arrange
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setBoardId(TEST_BOARD_ID);
        tableRequest.setOrderIndex(5); // Specific order index

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity createdTableEntity = new TableEntity();
        createdTableEntity.setId(TEST_TABLE_ID);
        createdTableEntity.setName("Test Table");
        createdTableEntity.setOrderIndex(5);
        createdTableEntity.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Test Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(5);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRequestToTableEntityMapper.mapForCreation(tableRequest, boardEntity)).thenReturn(createdTableEntity);
        when(tableRepository.save(createdTableEntity)).thenReturn(createdTableEntity);
        when(tableEntityToTableResponseMapper.map(createdTableEntity)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.createTable(tableRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Test Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(5, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(tableRequestToTableEntityMapper).mapForCreation(tableRequest, boardEntity);
        verify(tableRepository).save(createdTableEntity);
        verify(tableEntityToTableResponseMapper).map(createdTableEntity);
        // Should not query for existing tables since order index is specified
        verify(tableRepository, never()).findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID);
    }

    @Test
    void getTableById_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setOrderIndex(1);
        tableEntity.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Test Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(1);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(tableEntityToTableResponseMapper.map(tableEntity)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.getTableById(TEST_TABLE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Test Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(1, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(tableEntityToTableResponseMapper).map(tableEntity);
    }

    @Test
    void getAllTablesByBoardId_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity table1 = createTableEntity("table-1", 1);
        table1.setBoard(boardEntity);
        table1.setName("Table 1");

        TableEntity table2 = createTableEntity("table-2", 2);
        table2.setBoard(boardEntity);
        table2.setName("Table 2");

        List<TableEntity> tableEntities = Arrays.asList(table1, table2);

        TableResponseDTO response1 = createTableResponseDTO("table-1", "Table 1", 1);
        TableResponseDTO response2 = createTableResponseDTO("table-2", "Table 2", 2);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID)).thenReturn(tableEntities);
        when(tableEntityToTableResponseMapper.map(table1)).thenReturn(response1);
        when(tableEntityToTableResponseMapper.map(table2)).thenReturn(response2);

        // Act
        List<TableResponseDTO> result = tableService.getAllTablesByBoardId(TEST_BOARD_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("table-1", result.get(0).getId());
        assertEquals("table-2", result.get(1).getId());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(tableRepository).findByBoardIdOrderByOrderIndexAsc(TEST_BOARD_ID);
        verify(tableEntityToTableResponseMapper).map(table1);
        verify(tableEntityToTableResponseMapper).map(table2);
    }

    @Test
    void updateTable_SameBoard_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity existingTable = new TableEntity();
        existingTable.setId(TEST_TABLE_ID);
        existingTable.setName("Original Table");
        existingTable.setOrderIndex(1);
        existingTable.setBoard(boardEntity);

        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table");
        updateRequest.setBoardId(TEST_BOARD_ID); // Same board
        updateRequest.setOrderIndex(3); // New order index

        TableEntity updatedTable = new TableEntity();
        updatedTable.setId(TEST_TABLE_ID);
        updatedTable.setName("Updated Table");
        updatedTable.setOrderIndex(3);
        updatedTable.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Updated Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(3);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(existingTable);
        when(tableRepository.save(any(TableEntity.class))).thenReturn(updatedTable);
        when(tableEntityToTableResponseMapper.map(updatedTable)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.updateTable(TEST_TABLE_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Updated Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(3, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(tableRepository).save(any(TableEntity.class));
        verify(tableEntityToTableResponseMapper).map(updatedTable);
        // No need to check board access again since board hasn't changed
        verify(accessControlService, never()).findBoardAndCheckAccess(eq(TEST_BOARD_ID), anyString());
    }

    @Test
    void updateTable_DifferentBoard_Success() {
        // Arrange
        String newBoardId = "new-board-id";

        BoardEntity originalBoardEntity = new BoardEntity();
        originalBoardEntity.setId(TEST_BOARD_ID);
        originalBoardEntity.setName("Original Board");
        originalBoardEntity.setOwnerId(TEST_USER_ID);

        BoardEntity newBoardEntity = new BoardEntity();
        newBoardEntity.setId(newBoardId);
        newBoardEntity.setName("New Board");
        newBoardEntity.setOwnerId(TEST_USER_ID);

        TableEntity existingTable = new TableEntity();
        existingTable.setId(TEST_TABLE_ID);
        existingTable.setName("Original Table");
        existingTable.setOrderIndex(1);
        existingTable.setBoard(originalBoardEntity);

        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table");
        updateRequest.setBoardId(newBoardId); // Different board
        updateRequest.setOrderIndex(2); // New order index

        TableEntity updatedTable = new TableEntity();
        updatedTable.setId(TEST_TABLE_ID);
        updatedTable.setName("Updated Table");
        updatedTable.setOrderIndex(2);
        updatedTable.setBoard(newBoardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Updated Table");
        expectedResponse.setBoardId(newBoardId);
        expectedResponse.setOrderIndex(2);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(existingTable);
        when(accessControlService.findBoardAndCheckAccess(newBoardId, TEST_USER_ID)).thenReturn(newBoardEntity);
        when(tableRepository.save(any(TableEntity.class))).thenReturn(updatedTable);
        when(tableEntityToTableResponseMapper.map(updatedTable)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.updateTable(TEST_TABLE_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Updated Table", result.getName());
        assertEquals(newBoardId, result.getBoardId());
        assertEquals(2, result.getOrderIndex());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(accessControlService).findBoardAndCheckAccess(newBoardId, TEST_USER_ID);
        verify(tableRepository).save(any(TableEntity.class));
        verify(tableEntityToTableResponseMapper).map(updatedTable);
    }

    @Test
    void updateTable_NoOrderIndexChange_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity existingTable = new TableEntity();
        existingTable.setId(TEST_TABLE_ID);
        existingTable.setName("Original Table");
        existingTable.setOrderIndex(5);
        existingTable.setBoard(boardEntity);

        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table");
        updateRequest.setBoardId(TEST_BOARD_ID);
        updateRequest.setOrderIndex(0); // No order index change specified

        TableEntity updatedTable = new TableEntity();
        updatedTable.setId(TEST_TABLE_ID);
        updatedTable.setName("Updated Table");
        updatedTable.setOrderIndex(5); // Order index should remain the same
        updatedTable.setBoard(boardEntity);

        TableResponseDTO expectedResponse = new TableResponseDTO();
        expectedResponse.setId(TEST_TABLE_ID);
        expectedResponse.setName("Updated Table");
        expectedResponse.setBoardId(TEST_BOARD_ID);
        expectedResponse.setOrderIndex(5);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(existingTable);
        when(tableRepository.save(any(TableEntity.class))).thenReturn(updatedTable);
        when(tableEntityToTableResponseMapper.map(updatedTable)).thenReturn(expectedResponse);

        // Act
        TableResponseDTO result = tableService.updateTable(TEST_TABLE_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TABLE_ID, result.getId());
        assertEquals("Updated Table", result.getName());
        assertEquals(TEST_BOARD_ID, result.getBoardId());
        assertEquals(5, result.getOrderIndex()); // Order index should remain unchanged

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(tableRepository).save(any(TableEntity.class));
        verify(tableEntityToTableResponseMapper).map(updatedTable);
    }

    @Test
    void deleteTable_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setOrderIndex(1);
        tableEntity.setBoard(boardEntity);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);

        // Act
        tableService.deleteTable(TEST_TABLE_ID);

        // Assert
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(tableRepository).delete(tableEntity);
    }

    @Test
    void reorderTables_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity table1 = createTableEntity("table-1", 3);
        table1.setBoard(boardEntity);

        TableEntity table2 = createTableEntity("table-2", 1);
        table2.setBoard(boardEntity);

        TableEntity table3 = createTableEntity("table-3", 2);
        table3.setBoard(boardEntity);

        List<String> newTableOrder = Arrays.asList("table-2", "table-3", "table-1");

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findById("table-1")).thenReturn(Optional.of(table1));
        when(tableRepository.findById("table-2")).thenReturn(Optional.of(table2));
        when(tableRepository.findById("table-3")).thenReturn(Optional.of(table3));

        // Act
        tableService.reorderTables(TEST_BOARD_ID, newTableOrder);

        // Assert
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID);
        verify(tableRepository).findById("table-1");
        verify(tableRepository).findById("table-2");
        verify(tableRepository).findById("table-3");

        // Verify tables were saved with new order indexes
        verify(tableRepository).save(argThat(table -> table.getId().equals("table-2") && table.getOrderIndex() == 1));
        verify(tableRepository).save(argThat(table -> table.getId().equals("table-3") && table.getOrderIndex() == 2));
        verify(tableRepository).save(argThat(table -> table.getId().equals("table-1") && table.getOrderIndex() == 3));
    }

    @Test
    void reorderTables_TableNotFound_ThrowsException() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        List<String> tableIds = Arrays.asList("table-1", "nonexistent-table", "table-3");
        TableEntity table1 = createTableEntity("table-1", 1);
        table1.setBoard(boardEntity);
        TableEntity table3 = createTableEntity("table-3", 3);
        table3.setBoard(boardEntity);
        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findById("table-1")).thenReturn(Optional.of(table1));
        when(tableRepository.findById("nonexistent-table")).thenReturn(Optional.empty());

        // Act & Assert
        TableNotFoundException exception = assertThrows(TableNotFoundException.class, () -> {
            tableService.reorderTables(TEST_BOARD_ID, tableIds);
        });

        assertTrue(exception.getMessage().contains("Table not found with ID: nonexistent-table"));
        verify(tableRepository).findById("table-1");
        verify(tableRepository).findById("nonexistent-table");
        // Should save the first table before encountering the error
        verify(tableRepository).save(any(TableEntity.class));
    }

    @Test
    void reorderTables_TableFromDifferentBoard_ThrowsException() {
        // Arrange
        String otherBoardId = "other-board-id";

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        BoardEntity otherBoardEntity = new BoardEntity();
        otherBoardEntity.setId(otherBoardId);
        otherBoardEntity.setName("Other Board");
        otherBoardEntity.setOwnerId(TEST_USER_ID);

        TableEntity table1 = createTableEntity("table-1", 1);
        table1.setBoard(boardEntity);

        TableEntity table2 = createTableEntity("table-2", 1);
        table2.setBoard(otherBoardEntity); // Different board

        List<String> tableIds = Arrays.asList("table-1", "table-2");

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findBoardAndCheckAccess(TEST_BOARD_ID, TEST_USER_ID)).thenReturn(boardEntity);
        when(tableRepository.findById("table-1")).thenReturn(Optional.of(table1));
        when(tableRepository.findById("table-2")).thenReturn(Optional.of(table2));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            tableService.reorderTables(TEST_BOARD_ID, tableIds);
        });

        assertEquals("Table does not belong to the specified board", exception.getMessage());
        verify(tableRepository).findById("table-1");
        verify(tableRepository).findById("table-2");
        // Should save the first table before encountering the error
        verify(tableRepository).save(table1);
    }

    // Helper methods
    private TableEntity createTableEntity(String id, int orderIndex) {
        TableEntity table = new TableEntity();
        table.setId(id);
        table.setOrderIndex(orderIndex);
        return table;
    }

    private TableResponseDTO createTableResponseDTO(String id, String name, int orderIndex) {
        TableResponseDTO response = new TableResponseDTO();
        response.setId(id);
        response.setName(name);
        response.setOrderIndex(orderIndex);
        response.setBoardId(TEST_BOARD_ID);
        return response;
    }
}