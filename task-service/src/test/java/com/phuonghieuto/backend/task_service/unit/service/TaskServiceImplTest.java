package com.phuonghieuto.backend.task_service.unit.service;

import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskEntityToTaskResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TaskRequestToTaskEntityMapper;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.impl.TaskServiceImpl;
import com.phuonghieuto.backend.task_service.util.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EntityAccessControlService accessControlService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private TaskRequestToTaskEntityMapper taskRequestToTaskEntityMapper;

    @Mock
    private TaskEntityToTaskResponseMapper taskEntityToTaskResponseMapper;

    private TaskServiceImpl taskService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_BOARD_ID = "test-board-id";
    private static final String TEST_TABLE_ID = "test-table-id";
    private static final String TEST_TASK_ID = "test-task-id";

    @BeforeEach
    void setUp() {
        // Use mocked static methods for mappers
        try (MockedStatic<TaskRequestToTaskEntityMapper> mockedRequestMapper = Mockito
                .mockStatic(TaskRequestToTaskEntityMapper.class);
                MockedStatic<TaskEntityToTaskResponseMapper> mockedResponseMapper = Mockito
                        .mockStatic(TaskEntityToTaskResponseMapper.class)) {

            mockedRequestMapper.when(TaskRequestToTaskEntityMapper::initialize)
                    .thenReturn(taskRequestToTaskEntityMapper);

            mockedResponseMapper.when(TaskEntityToTaskResponseMapper::initialize)
                    .thenReturn(taskEntityToTaskResponseMapper);

            taskService = new TaskServiceImpl(taskRepository, accessControlService, authUtils);
        }
    }

    @Test
    void createTask_WithNoOrderIndex_Success() {
        // Arrange
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Task Description");
        taskRequest.setTableId(TEST_TABLE_ID);
        taskRequest.setOrderIndex(0); // No order index specified
        taskRequest.setStatus(TaskStatus.TODO);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        List<TaskEntity> existingTasks = Arrays.asList(createTaskEntity("task-1", 1, tableEntity),
                createTaskEntity("task-2", 2, tableEntity));

        TaskEntity createdTaskEntity = new TaskEntity();
        createdTaskEntity.setId(TEST_TASK_ID);
        createdTaskEntity.setTitle("Test Task");
        createdTaskEntity.setDescription("Task Description");
        createdTaskEntity.setTable(tableEntity);
        createdTaskEntity.setOrderIndex(3); // Should be 3 (after existing tasks)
        createdTaskEntity.setStatus(TaskStatus.TODO);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Test Task");
        expectedResponse.setDescription("Task Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(3);
        expectedResponse.setStatus(TaskStatus.TODO.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID)).thenReturn(existingTasks);
        when(taskRequestToTaskEntityMapper.mapForCreation(any(TaskRequestDTO.class), eq(tableEntity)))
                .thenReturn(createdTaskEntity);
        when(taskRepository.save(createdTaskEntity)).thenReturn(createdTaskEntity);
        when(taskEntityToTaskResponseMapper.map(createdTaskEntity)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.createTask(taskRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals("Task Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(3, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(taskRepository).findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID);
        verify(taskRequestToTaskEntityMapper).mapForCreation(any(TaskRequestDTO.class), eq(tableEntity));
        verify(taskRepository).save(createdTaskEntity);
        verify(taskEntityToTaskResponseMapper).map(createdTaskEntity);
    }

    @Test
    void createTask_WithNoExistingTasks_Success() {
        // Arrange
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Task Description");
        taskRequest.setTableId(TEST_TABLE_ID);
        taskRequest.setOrderIndex(0); // No order index specified
        taskRequest.setStatus(TaskStatus.TODO);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        List<TaskEntity> existingTasks = Collections.emptyList(); // No existing tasks

        TaskEntity createdTaskEntity = new TaskEntity();
        createdTaskEntity.setId(TEST_TASK_ID);
        createdTaskEntity.setTitle("Test Task");
        createdTaskEntity.setDescription("Task Description");
        createdTaskEntity.setTable(tableEntity);
        createdTaskEntity.setOrderIndex(1); // Should be 1 (first task)
        createdTaskEntity.setStatus(TaskStatus.TODO);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Test Task");
        expectedResponse.setDescription("Task Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(1);
        expectedResponse.setStatus(TaskStatus.TODO.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID)).thenReturn(existingTasks);
        when(taskRequestToTaskEntityMapper.mapForCreation(any(TaskRequestDTO.class), eq(tableEntity)))
                .thenReturn(createdTaskEntity);
        when(taskRepository.save(createdTaskEntity)).thenReturn(createdTaskEntity);
        when(taskEntityToTaskResponseMapper.map(createdTaskEntity)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.createTask(taskRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals("Task Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(1, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(taskRepository).findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID);
        verify(taskRequestToTaskEntityMapper).mapForCreation(any(TaskRequestDTO.class), eq(tableEntity));
        verify(taskRepository).save(createdTaskEntity);
        verify(taskEntityToTaskResponseMapper).map(createdTaskEntity);
    }

    @Test
    void createTask_WithSpecifiedOrderIndex_Success() {
        // Arrange
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Task Description");
        taskRequest.setTableId(TEST_TABLE_ID);
        taskRequest.setOrderIndex(5); // Specific order index
        taskRequest.setStatus(TaskStatus.TODO);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity createdTaskEntity = new TaskEntity();
        createdTaskEntity.setId(TEST_TASK_ID);
        createdTaskEntity.setTitle("Test Task");
        createdTaskEntity.setDescription("Task Description");
        createdTaskEntity.setTable(tableEntity);
        createdTaskEntity.setOrderIndex(5);
        createdTaskEntity.setStatus(TaskStatus.TODO);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Test Task");
        expectedResponse.setDescription("Task Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(5);
        expectedResponse.setStatus(TaskStatus.TODO.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRequestToTaskEntityMapper.mapForCreation(taskRequest, tableEntity)).thenReturn(createdTaskEntity);
        when(taskRepository.save(createdTaskEntity)).thenReturn(createdTaskEntity);
        when(taskEntityToTaskResponseMapper.map(createdTaskEntity)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.createTask(taskRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals("Task Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(5, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(taskRequestToTaskEntityMapper).mapForCreation(taskRequest, tableEntity);
        verify(taskRepository).save(createdTaskEntity);
        verify(taskEntityToTaskResponseMapper).map(createdTaskEntity);
        // Should not query for existing tasks since order index is specified
        verify(taskRepository, never()).findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID);
    }

    @Test
    void getTaskById_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity taskEntity = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        taskEntity.setTitle("Test Task");
        taskEntity.setDescription("Task Description");
        taskEntity.setStatus(TaskStatus.TODO);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Test Task");
        expectedResponse.setDescription("Task Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(1);
        expectedResponse.setStatus(TaskStatus.TODO.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(taskEntity);
        when(taskEntityToTaskResponseMapper.map(taskEntity)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.getTaskById(TEST_TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals("Task Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(1, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskEntityToTaskResponseMapper).map(taskEntity);
    }

    @Test
    void getAllTasksByTableId_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setStatus(TaskStatus.TODO);

        TaskEntity task2 = createTaskEntity("task-2", 2, tableEntity);
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setStatus(TaskStatus.TODO);

        List<TaskEntity> taskEntities = Arrays.asList(task1, task2);

        TaskResponseDTO response1 = createTaskResponseDTO("task-1", "Task 1", "Description 1", 1, TaskStatus.TODO);
        TaskResponseDTO response2 = createTaskResponseDTO("task-2", "Task 2", "Description 2", 2,
                TaskStatus.TODO);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID)).thenReturn(taskEntities);
        when(taskEntityToTaskResponseMapper.map(task1)).thenReturn(response1);
        when(taskEntityToTaskResponseMapper.map(task2)).thenReturn(response2);

        // Act
        List<TaskResponseDTO> result = taskService.getAllTasksByTableId(TEST_TABLE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("task-1", result.get(0).getId());
        assertEquals("task-2", result.get(1).getId());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(taskRepository).findByTableIdOrderByOrderIndexAsc(TEST_TABLE_ID);
        verify(taskEntityToTaskResponseMapper).map(task1);
        verify(taskEntityToTaskResponseMapper).map(task2);
    }

    @Test
    void getAllTasksByAssignedUserId_SameUser_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        boardEntity.setCollaboratorIds(new HashSet<>());

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setStatus(TaskStatus.TODO);
        task1.setAssignedUserId(TEST_USER_ID);

        TaskEntity task2 = createTaskEntity("task-2", 2, tableEntity);
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setStatus(TaskStatus.TODO);
        task2.setAssignedUserId(TEST_USER_ID);

        List<TaskEntity> taskEntities = Arrays.asList(task1, task2);

        TaskResponseDTO response1 = createTaskResponseDTO("task-1", "Task 1", "Description 1", 1, TaskStatus.TODO);
        TaskResponseDTO response2 = createTaskResponseDTO("task-2", "Task 2", "Description 2", 2,
                TaskStatus.TODO);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(taskRepository.findByAssignedUserId(TEST_USER_ID)).thenReturn(taskEntities);
        when(taskEntityToTaskResponseMapper.map(task1)).thenReturn(response1);
        when(taskEntityToTaskResponseMapper.map(task2)).thenReturn(response2);

        // Act
        List<TaskResponseDTO> result = taskService.getAllTasksByAssignedUserId(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("task-1", result.get(0).getId());
        assertEquals("task-2", result.get(1).getId());

        verify(authUtils).getCurrentUserId();
        verify(taskRepository).findByAssignedUserId(TEST_USER_ID);
        verify(taskEntityToTaskResponseMapper).map(task1);
        verify(taskEntityToTaskResponseMapper).map(task2);
    }

    @Test
    void getAllTasksByAssignedUserId_DifferentUser_ThrowsException() {
        // Arrange
        String otherUserId = "other-user-id";

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.getAllTasksByAssignedUserId(otherUserId);
        });

        assertEquals("You can only view your own tasks", exception.getMessage());
        verify(authUtils).getCurrentUserId();
        verify(taskRepository, never()).findByAssignedUserId(anyString());
    }

    @Test
    void updateTask_SameTable_Success() {
        // Arrange
        LocalDateTime dueDate = LocalDateTime.now().plusDays(1);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity existingTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        existingTask.setTitle("Original Title");
        existingTask.setDescription("Original Description");
        existingTask.setStatus(TaskStatus.TODO);
        existingTask.setAssignedUserId("old-assignee");

        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(TEST_TABLE_ID); // Same table
        updateRequest.setOrderIndex(3); // New order index
        updateRequest.setStatus(TaskStatus.TODO);
        updateRequest.setAssignedUserId("new-assignee");
        updateRequest.setDueDate(dueDate);

        TaskEntity updatedTask = createTaskEntity(TEST_TASK_ID, 3, tableEntity);
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.TODO);
        updatedTask.setAssignedUserId("new-assignee");
        updatedTask.setDueDate(dueDate);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Updated Title");
        expectedResponse.setDescription("Updated Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(3);
        expectedResponse.setStatus(TaskStatus.TODO.name());
        expectedResponse.setAssignedUserId("new-assignee");
        expectedResponse.setDueDate(dueDate.toString());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(existingTask);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);
        when(taskEntityToTaskResponseMapper.map(updatedTask)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.updateTask(TEST_TASK_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(3, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());
        assertEquals("new-assignee", result.getAssignedUserId());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskEntityToTaskResponseMapper).map(updatedTask);
        // No need to check table access again since table hasn't changed
        verify(accessControlService, never()).findTableAndCheckAccess(eq(TEST_TABLE_ID), anyString());
    }

    @Test
    void updateTask_DifferentTable_Success() {
        // Arrange
        String newTableId = "new-table-id";

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity originalTableEntity = new TableEntity();
        originalTableEntity.setId(TEST_TABLE_ID);
        originalTableEntity.setName("Original Table");
        originalTableEntity.setBoard(boardEntity);

        TableEntity newTableEntity = new TableEntity();
        newTableEntity.setId(newTableId);
        newTableEntity.setName("New Table");
        newTableEntity.setBoard(boardEntity);

        TaskEntity existingTask = createTaskEntity(TEST_TASK_ID, 1, originalTableEntity);
        existingTask.setTitle("Original Title");
        existingTask.setDescription("Original Description");
        existingTask.setStatus(TaskStatus.TODO);

        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(newTableId); // Different table
        updateRequest.setOrderIndex(2); // New order index
        updateRequest.setStatus(TaskStatus.TODO);

        TaskEntity updatedTask = createTaskEntity(TEST_TASK_ID, 2, newTableEntity);
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.TODO);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Updated Title");
        expectedResponse.setDescription("Updated Description");
        expectedResponse.setTableId(newTableId);
        expectedResponse.setOrderIndex(2);
        expectedResponse.setStatus(TaskStatus.TODO.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(existingTask);
        when(accessControlService.findTableAndCheckAccess(newTableId, TEST_USER_ID)).thenReturn(newTableEntity);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);
        when(taskEntityToTaskResponseMapper.map(updatedTask)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.updateTask(TEST_TASK_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(newTableId, result.getTableId());
        assertEquals(2, result.getOrderIndex());
        assertEquals(TaskStatus.TODO.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(accessControlService).findTableAndCheckAccess(newTableId, TEST_USER_ID);
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskEntityToTaskResponseMapper).map(updatedTask);
    }

    @Test
    void updateTask_WithOverdueDueDate_SetsOverdueStatus() {
        // Arrange
        LocalDateTime pastDueDate = LocalDateTime.now().minusDays(1); // Past due date

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity existingTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        existingTask.setTitle("Original Title");
        existingTask.setDescription("Original Description");
        existingTask.setStatus(TaskStatus.TODO);

        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(TEST_TABLE_ID);
        updateRequest.setDueDate(pastDueDate); // Past due date

        TaskEntity updatedTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.OVERDUE); // Should be set to OVERDUE
        updatedTask.setDueDate(pastDueDate);

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Updated Title");
        expectedResponse.setDescription("Updated Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(1);
        expectedResponse.setStatus(TaskStatus.OVERDUE.name());
        expectedResponse.setDueDate(pastDueDate.toString());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(existingTask);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);
        when(taskEntityToTaskResponseMapper.map(updatedTask)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.updateTask(TEST_TASK_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(TaskStatus.OVERDUE.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskEntityToTaskResponseMapper).map(updatedTask);
    }

    @Test
    void updateTask_MarkingCompleted_ResetsReminderFlag() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity existingTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        existingTask.setTitle("Original Title");
        existingTask.setDescription("Original Description");
        existingTask.setStatus(TaskStatus.TODO);
        existingTask.setReminderSent(true); // Reminder was sent

        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(TEST_TABLE_ID);
        updateRequest.setStatus(TaskStatus.COMPLETED); // Mark as completed

        TaskEntity updatedTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.COMPLETED);
        updatedTask.setReminderSent(false); // Reminder flag should be reset

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Updated Title");
        expectedResponse.setDescription("Updated Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(1);
        expectedResponse.setStatus(TaskStatus.COMPLETED.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(existingTask);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);
        when(taskEntityToTaskResponseMapper.map(updatedTask)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.updateTask(TEST_TASK_ID, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(TaskStatus.COMPLETED.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskEntityToTaskResponseMapper).map(updatedTask);
    }

    @Test
    void deleteTask_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity taskEntity = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        taskEntity.setTitle("Test Task");
        taskEntity.setDescription("Task Description");

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(taskEntity);

        // Act
        taskService.deleteTask(TEST_TASK_ID);

        // Assert
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskRepository).delete(taskEntity);
    }

    @Test
    void reorderTasks_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 3, tableEntity);
        TaskEntity task2 = createTaskEntity("task-2", 1, tableEntity);
        TaskEntity task3 = createTaskEntity("task-3", 2, tableEntity);

        List<String> newTaskOrder = Arrays.asList("task-2", "task-3", "task-1");

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task1));
        when(taskRepository.findById("task-2")).thenReturn(Optional.of(task2));
        when(taskRepository.findById("task-3")).thenReturn(Optional.of(task3));

        // Act
        taskService.reorderTasks(TEST_TABLE_ID, newTaskOrder);

        // Assert
        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID);
        verify(taskRepository).findById("task-1");
        verify(taskRepository).findById("task-2");
        verify(taskRepository).findById("task-3");

        // Verify tasks were saved with new order indexes
        verify(taskRepository).save(argThat(task -> task.getId().equals("task-2") && task.getOrderIndex() == 1));
        verify(taskRepository).save(argThat(task -> task.getId().equals("task-3") && task.getOrderIndex() == 2));
        verify(taskRepository).save(argThat(task -> task.getId().equals("task-1") && task.getOrderIndex() == 3));
    }

    @Test
    void reorderTasks_TaskNotFound_ThrowsException() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        List<String> taskIds = Arrays.asList("task-1", "nonexistent-task", "task-3");

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task1));
        when(taskRepository.findById("nonexistent-task")).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.reorderTasks(TEST_TABLE_ID, taskIds);
        });

        assertTrue(exception.getMessage().contains("Task not found with ID: nonexistent-task"));
        verify(taskRepository).findById("task-1");
        verify(taskRepository).findById("nonexistent-task");
        // Should save the first task before encountering the error
        verify(taskRepository).save(any(TaskEntity.class));
    }

    @Test
    void reorderTasks_TaskFromDifferentTable_ThrowsException() {
        // Arrange
        String otherTableId = "other-table-id";

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TableEntity otherTableEntity = new TableEntity();
        otherTableEntity.setId(otherTableId);
        otherTableEntity.setName("Other Table");
        otherTableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);
        TaskEntity task2 = createTaskEntity("task-2", 1, otherTableEntity); // Different table

        List<String> taskIds = Arrays.asList("task-1", "task-2");

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTableAndCheckAccess(TEST_TABLE_ID, TEST_USER_ID)).thenReturn(tableEntity);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task1));
        when(taskRepository.findById("task-2")).thenReturn(Optional.of(task2));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.reorderTasks(TEST_TABLE_ID, taskIds);
        });

        assertEquals("Task does not belong to the specified table", exception.getMessage());
        verify(taskRepository).findById("task-1");
        verify(taskRepository).findById("task-2");
        // Should save the first task before encountering the error
        verify(taskRepository).save(task1);
    }

    @Test
    void findByDueDateBetween_Success() {
        // Arrange
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(7);

        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        boardEntity.setCollaboratorIds(new HashSet<>());

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);
        task1.setTitle("Task 1");
        task1.setDueDate(LocalDateTime.now().plusDays(2));

        TaskEntity task2 = createTaskEntity("task-2", 2, tableEntity);
        task2.setTitle("Task 2");
        task2.setDueDate(LocalDateTime.now().plusDays(5));

        List<TaskEntity> taskEntities = Arrays.asList(task1, task2);

        TaskResponseDTO response1 = createTaskResponseDTO("task-1", "Task 1", "Description 1", 1, TaskStatus.TODO);
        TaskResponseDTO response2 = createTaskResponseDTO("task-2", "Task 2", "Description 2", 2, TaskStatus.TODO);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(taskRepository.findByDueDateBetween(start, end)).thenReturn(taskEntities);
        when(taskEntityToTaskResponseMapper.map(task1)).thenReturn(response1);
        when(taskEntityToTaskResponseMapper.map(task2)).thenReturn(response2);

        // Act
        List<TaskResponseDTO> result = taskService.findByDueDateBetween(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("task-1", result.get(0).getId());
        assertEquals("task-2", result.get(1).getId());

        verify(authUtils).getCurrentUserId();
        verify(taskRepository).findByDueDateBetween(start, end);
        verify(taskEntityToTaskResponseMapper).map(task1);
        verify(taskEntityToTaskResponseMapper).map(task2);
    }

    @Test
    void updateTaskStatus_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity existingTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        existingTask.setTitle("Test Task");
        existingTask.setDescription("Task Description");
        existingTask.setStatus(TaskStatus.TODO);
        existingTask.setReminderSent(true);

        TaskEntity updatedTask = createTaskEntity(TEST_TASK_ID, 1, tableEntity);
        updatedTask.setTitle("Test Task");
        updatedTask.setDescription("Task Description");
        updatedTask.setStatus(TaskStatus.COMPLETED);
        updatedTask.setReminderSent(false); // Should be reset when completed

        TaskResponseDTO expectedResponse = new TaskResponseDTO();
        expectedResponse.setId(TEST_TASK_ID);
        expectedResponse.setTitle("Test Task");
        expectedResponse.setDescription("Task Description");
        expectedResponse.setTableId(TEST_TABLE_ID);
        expectedResponse.setOrderIndex(1);
        expectedResponse.setStatus(TaskStatus.COMPLETED.name());

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(accessControlService.findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID)).thenReturn(existingTask);
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);
        when(taskEntityToTaskResponseMapper.map(updatedTask)).thenReturn(expectedResponse);

        // Act
        TaskResponseDTO result = taskService.updateTaskStatus(TEST_TASK_ID, TaskStatus.COMPLETED);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_TASK_ID, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals(TEST_TABLE_ID, result.getTableId());
        assertEquals(TaskStatus.COMPLETED.name(), result.getStatus());

        verify(authUtils).getCurrentUserId();
        verify(accessControlService).findTaskAndCheckAccess(TEST_TASK_ID, TEST_USER_ID);
        verify(taskRepository).save(any(TaskEntity.class));
        verify(taskEntityToTaskResponseMapper).map(updatedTask);
    }

    @Test
    void getAllTasksByStatus_Success() {
        // Arrange
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setId(TEST_BOARD_ID);
        boardEntity.setName("Test Board");
        boardEntity.setOwnerId(TEST_USER_ID);
        boardEntity.setCollaboratorIds(new HashSet<>());

        TableEntity tableEntity = new TableEntity();
        tableEntity.setId(TEST_TABLE_ID);
        tableEntity.setName("Test Table");
        tableEntity.setBoard(boardEntity);

        TaskEntity task1 = createTaskEntity("task-1", 1, tableEntity);
        task1.setTitle("Task 1");
        task1.setStatus(TaskStatus.TODO);

        TaskEntity task2 = createTaskEntity("task-2", 2, tableEntity);
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.TODO);

        List<TaskEntity> taskEntities = Arrays.asList(task1, task2);

        TaskResponseDTO response1 = createTaskResponseDTO("task-1", "Task 1", "Description 1", 1,
                TaskStatus.TODO);
        TaskResponseDTO response2 = createTaskResponseDTO("task-2", "Task 2", "Description 2", 2,
                TaskStatus.TODO);

        when(authUtils.getCurrentUserId()).thenReturn(TEST_USER_ID);
        when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(taskEntities);
        when(taskEntityToTaskResponseMapper.map(task1)).thenReturn(response1);
        when(taskEntityToTaskResponseMapper.map(task2)).thenReturn(response2);

        // Act
        List<TaskResponseDTO> result = taskService.getAllTasksByStatus(TaskStatus.TODO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("task-1", result.get(0).getId());
        assertEquals("task-2", result.get(1).getId());

        verify(authUtils).getCurrentUserId();
        verify(taskRepository).findByStatus(TaskStatus.TODO);
        verify(taskEntityToTaskResponseMapper).map(task1);
        verify(taskEntityToTaskResponseMapper).map(task2);
    }

    @Test
    void getTaskStatistics_Success() {
        // Arrange
        when(taskRepository.countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.TODO)).thenReturn(3L);
        when(taskRepository.countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.COMPLETED)).thenReturn(10L);
        when(taskRepository.countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.OVERDUE)).thenReturn(2L);

        // Act
        Map<TaskStatus, Long> result = taskService.getTaskStatistics(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(3L, result.get(TaskStatus.TODO));
        assertEquals(10L, result.get(TaskStatus.COMPLETED));
        assertEquals(2L, result.get(TaskStatus.OVERDUE));

        verify(taskRepository).countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.TODO);
        verify(taskRepository).countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.TODO);
        verify(taskRepository).countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.COMPLETED);
        verify(taskRepository).countByAssignedUserIdAndStatus(TEST_USER_ID, TaskStatus.OVERDUE);
    }

    // Helper methods
    private TaskEntity createTaskEntity(String id, int orderIndex, TableEntity table) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setOrderIndex(orderIndex);
        task.setTable(table);
        task.setStatus(TaskStatus.TODO);
        return task;
    }

    private TaskResponseDTO createTaskResponseDTO(String id, String title, String description, int orderIndex,
            TaskStatus status) {
        TaskResponseDTO response = new TaskResponseDTO();
        response.setId(id);
        response.setTitle(title);
        response.setDescription(description);
        response.setOrderIndex(orderIndex);
        response.setTableId(TEST_TABLE_ID);
        response.setStatus(status.name());
        return response;
    }
}