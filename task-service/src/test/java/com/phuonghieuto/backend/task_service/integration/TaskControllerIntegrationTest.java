package com.phuonghieuto.backend.task_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.repository.TaskRepository;

import io.jsonwebtoken.Jwts;

@AutoConfigureMockMvc
public class TaskControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokenConfigurationParameter tokenConfigurationParameter;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String OTHER_USER_ID = "other-user-id";
    // private static final String OTHER_USER_EMAIL = "other@example.com";

    private String accessToken;
    // private String otherUserAccessToken;
    private BoardEntity testBoard;
    private TableEntity testTable;
    // private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        taskRepository.deleteAll();
        tableRepository.deleteAll();
        boardRepository.deleteAll();

        // Create test tokens
        accessToken = generateToken(TEST_USER_ID, TEST_USER_EMAIL);
        // otherUserAccessToken = generateToken(OTHER_USER_ID, OTHER_USER_EMAIL);

        // Create a test board owned by the test user
        testBoard = new BoardEntity();
        testBoard.setName("Test Board");
        testBoard.setOwnerId(TEST_USER_ID);
        testBoard.setCollaboratorIds(new HashSet<>());
        testBoard = boardRepository.save(testBoard);

        // Create a test table in the test board
        testTable = new TableEntity();
        testTable.setName("Test Table");
        testTable.setOrderIndex(1);
        testTable.setBoard(testBoard);
        testTable = tableRepository.save(testTable);
    }

    public String generateToken(String userId, String userEmail) {
        final long currentTimeMillis = System.currentTimeMillis();
        final Date tokenIssuedAt = new Date(currentTimeMillis);
        final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis), 30);

        return Jwts.builder().setHeaderParam(TokenClaims.TYP.getValue(), "Bearer").setId(UUID.randomUUID().toString())
                .setIssuedAt(tokenIssuedAt).setExpiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .addClaims(Map.of("userId", userId, "userEmail", userEmail)).compact();
    }

    @Test
    void createTask_Success() throws Exception {
        // Create a task request
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
        taskRequest.setTableId(testTable.getId());
        taskRequest.setOrderIndex(1);
        taskRequest.setAssignedUserId(TEST_USER_ID);
        taskRequest.setStatus(TaskStatus.TODO);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(2);
        taskRequest.setDueDate(dueDate);

        // Create the task
        mockMvc.perform(post("/tasks").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(taskRequest)))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.title").value("Test Task"))
                .andExpect(jsonPath("$.response.description").value("Test Description"))
                .andExpect(jsonPath("$.response.tableId").value(testTable.getId()))
                .andExpect(jsonPath("$.response.orderIndex").value(1))
                .andExpect(jsonPath("$.response.assignedUserId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.response.status").value("TODO"));

        // Verify the task was created in the database
        List<TaskEntity> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());
        TaskEntity savedTask = tasks.get(0);
        assertEquals("Test Task", savedTask.getTitle());
        assertEquals("Test Description", savedTask.getDescription());
        assertEquals(testTable.getId(), savedTask.getTable().getId());
        assertEquals(1, savedTask.getOrderIndex());
        assertEquals(TEST_USER_ID, savedTask.getAssignedUserId());
        assertEquals(TaskStatus.TODO, savedTask.getStatus());
    }

    @Test
    void createTask_InvalidRequest() throws Exception {
        // Create invalid request (empty title)
        TaskRequestDTO invalidRequest = new TaskRequestDTO();
        invalidRequest.setDescription("Test Description");
        invalidRequest.setTableId(testTable.getId());
        invalidRequest.setOrderIndex(1);
        // Title is missing

        mockMvc.perform(post("/tasks").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no task was created
        assertEquals(0, taskRepository.findAll().size());
    }

    @Test
    void createTask_TableNotFound() throws Exception {
        // Create a task request with non-existent table ID
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
        taskRequest.setTableId("non-existent-table-id");
        taskRequest.setOrderIndex(1);
        taskRequest.setAssignedUserId(TEST_USER_ID);

        mockMvc.perform(post("/tasks").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(taskRequest)))
                .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify no task was created
        assertEquals(0, taskRepository.findAll().size());
    }

    @Test
    void createTask_Unauthorized() throws Exception {
        // Try to create a task without authentication
        TaskRequestDTO taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
        taskRequest.setTableId(testTable.getId());
        taskRequest.setOrderIndex(1);

        mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest))).andDo(print())
                .andExpect(status().isUnauthorized());

        // Verify no task was created
        assertEquals(0, taskRepository.findAll().size());
    }

    @Test
    void getTaskById_Success() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        TaskEntity savedTask = taskRepository.save(task);

        // Get the task by ID
        mockMvc.perform(get("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.response.title").value("Test Task"))
                .andExpect(jsonPath("$.response.description").value("Test Description"))
                .andExpect(jsonPath("$.response.tableId").value(testTable.getId()))
                .andExpect(jsonPath("$.response.orderIndex").value(1))
                .andExpect(jsonPath("$.response.assignedUserId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.response.status").value("TODO"));
    }

    @Test
    void getTaskById_NotFound() throws Exception {
        // Try to get a non-existent task
        String nonExistentId = "non-existent-id";

        mockMvc.perform(get("/tasks/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTaskById_Unauthorized() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Try to get the task without authentication
        mockMvc.perform(get("/tasks/{id}", savedTask.getId())).andDo(print()).andExpect(status().isUnauthorized());
    }

    @Test
    void getAllTasksByTableId_Success() throws Exception {
        // Create multiple tasks in the test table
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        task2.setStatus(TaskStatus.TODO);
        task2.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task2);

        // Get tasks for the table
        mockMvc.perform(
                get("/tasks/table/{tableId}", testTable.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(2)))
                .andExpect(jsonPath("$.response[0].title").value("Task 1"))
                .andExpect(jsonPath("$.response[1].title").value("Task 2"));
    }

    @Test
    void getAllTasksByTableId_EmptyList() throws Exception {
        // No tasks for the test table
        mockMvc.perform(
                get("/tasks/table/{tableId}", testTable.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(0)));
    }

    @Test
    void getAllTasksByTableId_TableNotFound() throws Exception {
        // Try to get tasks for a non-existent table
        String nonExistentTableId = "non-existent-table-id";

        mockMvc.perform(
                get("/tasks/table/{tableId}", nonExistentTableId).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAllTasksByTableId_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);

        // Create a table in the other user's board
        TableEntity otherTable = new TableEntity();
        otherTable.setName("Other User's Table");
        otherTable.setOrderIndex(1);
        otherTable.setBoard(otherBoard);
        otherTable = tableRepository.save(otherTable);

        // Try to get tasks for the other user's table
        mockMvc.perform(
                get("/tasks/table/{tableId}", otherTable.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getMyTasks_Success() throws Exception {
        // Create multiple tasks assigned to the test user
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        task2.setStatus(TaskStatus.TODO);
        task2.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task2);

        // Create a task assigned to a different user
        TaskEntity task3 = new TaskEntity();
        task3.setTitle("Task 3");
        task3.setDescription("Description 3");
        task3.setTable(testTable);
        task3.setOrderIndex(3);
        task3.setAssignedUserId(OTHER_USER_ID);
        task3.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task3);

        // Get tasks for the test user
        mockMvc.perform(get("/tasks/my-tasks").header("Authorization", "Bearer " + accessToken)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(2)))
                .andExpect(jsonPath("$.response[0].title").value("Task 1"))
                .andExpect(jsonPath("$.response[1].title").value("Task 2"));
    }

    @Test
    void getMyTasks_EmptyList() throws Exception {
        // No tasks assigned to the test user
        mockMvc.perform(get("/tasks/my-tasks").header("Authorization", "Bearer " + accessToken)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(0)));
    }

    @Test
    void updateTask_Success() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Original Task");
        task.setDescription("Original Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        task.setStatus(TaskStatus.TODO);
        TaskEntity savedTask = taskRepository.save(task);

        // Update request
        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(testTable.getId());
        updateRequest.setOrderIndex(2);
        updateRequest.setAssignedUserId(TEST_USER_ID);
        updateRequest.setStatus(TaskStatus.TODO);
        updateRequest.setDueDate(LocalDateTime.now().plusDays(3));

        // Update the task
        mockMvc.perform(put("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.response.title").value("Updated Task"))
                .andExpect(jsonPath("$.response.description").value("Updated Description"))
                .andExpect(jsonPath("$.response.orderIndex").value(2))
                .andExpect(jsonPath("$.response.status").value("TODO"));

        // Verify the task was updated in the database
        TaskEntity updatedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(updatedTask);
        assertEquals("Updated Task", updatedTask.getTitle());
        assertEquals("Updated Description", updatedTask.getDescription());
        assertEquals(2, updatedTask.getOrderIndex());
        assertEquals(TaskStatus.TODO, updatedTask.getStatus());
    }

    @Test
    void updateTask_NotFound() throws Exception {
        // Try to update a non-existent task
        String nonExistentId = "non-existent-id";

        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(testTable.getId());
        updateRequest.setOrderIndex(2);

        mockMvc.perform(put("/tasks/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateTask_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);

        // Create a table in the other user's board
        TableEntity otherTable = new TableEntity();
        otherTable.setName("Other User's Table");
        otherTable.setOrderIndex(1);
        otherTable.setBoard(otherBoard);
        otherTable = tableRepository.save(otherTable);

        // Create a task in the other user's table
        TaskEntity task = new TaskEntity();
        task.setTitle("Other User's Task");
        task.setDescription("Other User's Description");
        task.setTable(otherTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(OTHER_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Update request
        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Trying to Update");
        updateRequest.setDescription("Trying to Update Description");
        updateRequest.setTableId(otherTable.getId());
        updateRequest.setOrderIndex(2);

        // Try to update the task as a different user
        mockMvc.perform(put("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify the task was not updated
        TaskEntity unchangedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(unchangedTask);
        assertEquals("Other User's Task", unchangedTask.getTitle());
    }

    @Test
    void updateTask_InvalidRequest() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Original Task");
        task.setDescription("Original Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Create invalid request (empty title)
        TaskRequestDTO invalidRequest = new TaskRequestDTO();
        invalidRequest.setDescription("Updated Description");
        invalidRequest.setTableId(testTable.getId());
        invalidRequest.setOrderIndex(2);
        // Title is missing

        mockMvc.perform(put("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify the task was not updated
        TaskEntity unchangedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(unchangedTask);
        assertEquals("Original Task", unchangedTask.getTitle());
    }

    @Test
    void deleteTask_Success() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Task to Delete");
        task.setDescription("Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Delete the task
        mockMvc.perform(delete("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));

        // Verify the task was deleted
        assertTrue(taskRepository.findById(savedTask.getId()).isEmpty());
    }

    @Test
    void deleteTask_NotFound() throws Exception {
        // Try to delete a non-existent task
        String nonExistentId = "non-existent-id";

        mockMvc.perform(delete("/tasks/{id}", nonExistentId).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteTask_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);

        // Create a table in the other user's board
        TableEntity otherTable = new TableEntity();
        otherTable.setName("Other User's Table");
        otherTable.setOrderIndex(1);
        otherTable.setBoard(otherBoard);
        otherTable = tableRepository.save(otherTable);

        // Create a task in the other user's table
        TaskEntity task = new TaskEntity();
        task.setTitle("Other User's Task");
        task.setDescription("Other User's Description");
        task.setTable(otherTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(OTHER_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Try to delete the task as a different user
        mockMvc.perform(delete("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());

        // Verify the task was not deleted
        assertTrue(taskRepository.findById(savedTask.getId()).isPresent());
    }

    @Test
    void reorderTasks_Success() throws Exception {
        // Create multiple tasks in the test table
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask1 = taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask2 = taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setTitle("Task 3");
        task3.setDescription("Description 3");
        task3.setTable(testTable);
        task3.setOrderIndex(3);
        task3.setAssignedUserId(TEST_USER_ID);
        TaskEntity savedTask3 = taskRepository.save(task3);

        // Create reorder request (reverse the order)
        List<String> reorderedTaskIds = new ArrayList<>();
        reorderedTaskIds.add(savedTask3.getId());
        reorderedTaskIds.add(savedTask2.getId());
        reorderedTaskIds.add(savedTask1.getId());

        // Reorder the tasks
        mockMvc.perform(put("/tasks/table/{tableId}/reorder", testTable.getId())
                .header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reorderedTaskIds))).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // Verify the tasks were reordered in the database
        TaskEntity updatedTask1 = taskRepository.findById(savedTask1.getId()).orElse(null);
        TaskEntity updatedTask2 = taskRepository.findById(savedTask2.getId()).orElse(null);
        TaskEntity updatedTask3 = taskRepository.findById(savedTask3.getId()).orElse(null);

        assertNotNull(updatedTask1);
        assertNotNull(updatedTask2);
        assertNotNull(updatedTask3);

        assertEquals(3, updatedTask1.getOrderIndex()); // Was 1, now 3
        assertEquals(2, updatedTask2.getOrderIndex()); // Was 2, still 2
        assertEquals(1, updatedTask3.getOrderIndex()); // Was 3, now 1
    }

    @Test
    void reorderTasks_TableNotFound() throws Exception {
        // Try to reorder tasks for a non-existent table
        String nonExistentTableId = "non-existent-table-id";
        List<String> taskIds = new ArrayList<>();
        taskIds.add("task-id-1");

        mockMvc.perform(put("/tasks/table/{tableId}/reorder", nonExistentTableId)
                .header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskIds))).andDo(print()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false)).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void reorderTasks_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);

        // Create a table in the other user's board
        TableEntity otherTable = new TableEntity();
        otherTable.setName("Other User's Table");
        otherTable.setOrderIndex(1);
        otherTable.setBoard(otherBoard);
        otherTable = tableRepository.save(otherTable);

        // Create a task in the other user's table
        TaskEntity task = new TaskEntity();
        task.setTitle("Other User's Task");
        task.setDescription("Other User's Description");
        task.setTable(otherTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(OTHER_USER_ID);
        TaskEntity savedTask = taskRepository.save(task);

        // Create reorder request
        List<String> taskIds = new ArrayList<>();
        taskIds.add(savedTask.getId());

        // Try to reorder tasks for the other user's table
        mockMvc.perform(put("/tasks/table/{tableId}/reorder", otherTable.getId())
                .header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskIds))).andDo(print()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false)).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getUpcomingTasks_Success() throws Exception {
        // Create tasks with due dates in the next 24 hours
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Upcoming Task 1");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        task1.setDueDate(LocalDateTime.now().plusHours(2));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Upcoming Task 2");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        task2.setDueDate(LocalDateTime.now().plusHours(12));
        taskRepository.save(task2);

        // Create a task with a due date beyond 24 hours
        TaskEntity task3 = new TaskEntity();
        task3.setTitle("Non-Upcoming Task");
        task3.setDescription("Description 3");
        task3.setTable(testTable);
        task3.setOrderIndex(3);
        task3.setAssignedUserId(TEST_USER_ID);
        task3.setDueDate(LocalDateTime.now().plusHours(25));
        taskRepository.save(task3);

        // Get upcoming tasks
        mockMvc.perform(get("/tasks/upcoming").header("Authorization", "Bearer " + accessToken)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Upcoming Task 1"))
                .andExpect(jsonPath("$[1].title").value("Upcoming Task 2"));
    }

    @Test
    void updateTaskStatus_Success() throws Exception {
        // Create a task in the database
        TaskEntity task = new TaskEntity();
        task.setTitle("Task to Update Status");
        task.setDescription("Description");
        task.setTable(testTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(TEST_USER_ID);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        TaskEntity savedTask = taskRepository.save(task);

        // Update the task status
        mockMvc.perform(patch("/tasks/{id}/status", savedTask.getId()).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("\"COMPLETED\"")).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.response.status").value("COMPLETED"));

        // Verify the task status was updated in the database
        TaskEntity updatedTask = taskRepository.findById(savedTask.getId()).orElse(null);
        assertNotNull(updatedTask);
        assertEquals(TaskStatus.COMPLETED, updatedTask.getStatus());
    }

    @Test
    void updateTaskStatus_NotFound() throws Exception {
        // Try to update status of a non-existent task
        String nonExistentId = "non-existent-id";

        mockMvc.perform(patch("/tasks/{id}/status", nonExistentId).header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content("\"COMPLETED\"")).andDo(print())
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTasksByStatus_Success() throws Exception {
        // Create tasks with different statuses
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Todo Task 1");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Todo Task 2");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        task2.setStatus(TaskStatus.TODO);
        task2.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setTitle("Completed Task");
        task3.setDescription("Description 3");
        task3.setTable(testTable);
        task3.setOrderIndex(3);
        task3.setAssignedUserId(TEST_USER_ID);
        task3.setStatus(TaskStatus.COMPLETED);
        task3.setDueDate(LocalDateTime.now().plusDays(1));
        taskRepository.save(task3);

        // Get tasks by TODO status
        mockMvc.perform(get("/tasks/status/{status}", TaskStatus.TODO).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(2)))
                .andExpect(jsonPath("$.response[0].title").value("Todo Task 1"))
                .andExpect(jsonPath("$.response[1].title").value("Todo Task 2"));

        // Get tasks by TODO status
        mockMvc.perform(
                get("/tasks/status/{status}", TaskStatus.COMPLETED).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(1)))
                .andExpect(jsonPath("$.response[0].title").value("Completed Task"));
    }

    @Test
    void getTasksByStatus_EmptyList() throws Exception {
        // No tasks with COMPLETED status
        mockMvc.perform(
                get("/tasks/status/{status}", TaskStatus.COMPLETED).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray()).andExpect(jsonPath("$.response", hasSize(0)));
    }

    @Test
    void getTaskStatistics_Success() throws Exception {
        // Create tasks with different statuses
        TaskEntity task1 = new TaskEntity();
        task1.setTitle("Todo Task");
        task1.setDescription("Description 1");
        task1.setTable(testTable);
        task1.setOrderIndex(1);
        task1.setAssignedUserId(TEST_USER_ID);
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setTitle("Completed Task");
        task2.setDescription("Description 2");
        task2.setTable(testTable);
        task2.setOrderIndex(2);
        task2.setAssignedUserId(TEST_USER_ID);
        task2.setStatus(TaskStatus.COMPLETED);
        task2.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setTitle("Completed Task 1");
        task3.setDescription("Description 3");
        task3.setTable(testTable);
        task3.setOrderIndex(3);
        task3.setAssignedUserId(TEST_USER_ID);
        task3.setStatus(TaskStatus.COMPLETED);
        task3.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task3);

        TaskEntity task4 = new TaskEntity();
        task4.setTitle("Completed Task 2");
        task4.setDescription("Description 4");
        task4.setTable(testTable);
        task4.setOrderIndex(4);
        task4.setAssignedUserId(TEST_USER_ID);
        task4.setStatus(TaskStatus.COMPLETED);
        task4.setDueDate(LocalDateTime.now().plusDays(3));
        taskRepository.save(task4);

        // Get task statistics
        mockMvc.perform(get("/tasks/statistics").header("Authorization", "Bearer " + accessToken)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.TODO").value(1))
                .andExpect(jsonPath("$.TODO").value(1)).andExpect(jsonPath("$.COMPLETED").value(3));
    }

    @Test
    void getTaskStatistics_EmptyStatistics() throws Exception {
        // No tasks for the user
        mockMvc.perform(get("/tasks/statistics").header("Authorization", "Bearer " + accessToken)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.TODO").value(0)).andExpect(jsonPath("$.COMPLETED").value(0)).andExpect(jsonPath("$.OVERDUE").value(0));
    }

    @Test
    void collaboratorCanAccessTask() throws Exception {
        // Create a board with TEST_USER_ID as collaborator
        BoardEntity collaborativeBoard = new BoardEntity();
        collaborativeBoard.setName("Collaborative Board");
        collaborativeBoard.setOwnerId(OTHER_USER_ID);
        Set<String> collaboratorIds = new HashSet<>();
        collaboratorIds.add(TEST_USER_ID);
        collaborativeBoard.setCollaboratorIds(collaboratorIds);
        collaborativeBoard = boardRepository.save(collaborativeBoard);

        // Create a table in the collaborative board
        TableEntity collaborativeTable = new TableEntity();
        collaborativeTable.setName("Collaborative Table");
        collaborativeTable.setOrderIndex(1);
        collaborativeTable.setBoard(collaborativeBoard);
        collaborativeTable = tableRepository.save(collaborativeTable);

        // Create a task in the collaborative table
        TaskEntity task = new TaskEntity();
        task.setTitle("Collaborative Task");
        task.setDescription("Collaborative Description");
        task.setTable(collaborativeTable);
        task.setOrderIndex(1);
        task.setAssignedUserId(OTHER_USER_ID);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        TaskEntity savedTask = taskRepository.save(task);

        // TEST_USER_ID should be able to access the task as a collaborator
        mockMvc.perform(get("/tasks/{id}", savedTask.getId()).header("Authorization", "Bearer " + accessToken))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTask.getId()))
                .andExpect(jsonPath("$.response.title").value("Collaborative Task"));
    }
}
