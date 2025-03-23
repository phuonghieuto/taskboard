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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phuonghieuto.backend.task_service.controller.TaskController;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TaskNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.service.TaskService;


@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private TaskController taskController;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_TASK_ID = "test-task-id";
    private static final String TEST_TABLE_ID = "test-table-id";
    private TaskRequestDTO taskRequest;
    private TaskResponseDTO taskResponse;
    private List<TaskResponseDTO> taskResponseList;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
            
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test data
        // Create request DTO
        taskRequest = new TaskRequestDTO();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Task Description");
        taskRequest.setTableId(TEST_TABLE_ID);
        taskRequest.setOrderIndex(1);
        taskRequest.setAssignedUserId(TEST_USER_ID);
        taskRequest.setDueDate(LocalDateTime.now().plusDays(1));
        taskRequest.setStatus(TaskStatus.TODO);

        // Create response DTO
        taskResponse = new TaskResponseDTO();
        taskResponse.setId(TEST_TASK_ID);
        taskResponse.setTitle("Test Task");
        taskResponse.setDescription("Task Description");
        taskResponse.setTableId(TEST_TABLE_ID);
        taskResponse.setOrderIndex(1);
        taskResponse.setAssignedUserId(TEST_USER_ID);
        taskResponse.setDueDate(LocalDateTime.now().plusDays(1).toString());
        taskResponse.setStatus(TaskStatus.TODO.name());

        // Create second task for list tests
        TaskResponseDTO taskResponse2 = new TaskResponseDTO();
        taskResponse2.setId("task-id-2");
        taskResponse2.setTitle("Second Task");
        taskResponse2.setDescription("Second Description");
        taskResponse2.setTableId(TEST_TABLE_ID);
        taskResponse2.setOrderIndex(2);
        taskResponse2.setAssignedUserId(TEST_USER_ID);
        taskResponse2.setDueDate(LocalDateTime.now().plusDays(2).toString());
        taskResponse2.setStatus(TaskStatus.TODO.name());
        
        // Create list of tasks
        taskResponseList = Arrays.asList(taskResponse, taskResponse2);

        // Setup authentication mock
        
    }

    @Test
    void createTask_Success() throws Exception {
        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn(taskResponse);

        mockMvc.perform(post("/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response.title").value("Test Task"))
            .andExpect(jsonPath("$.response.description").value("Task Description"))
            .andExpect(jsonPath("$.response.tableId").value(TEST_TABLE_ID))
            .andExpect(jsonPath("$.response.assignedUserId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.response.status").value(TaskStatus.TODO.name()));

        verify(taskService, times(1)).createTask(any(TaskRequestDTO.class));
    }

    @Test
    void createTask_InvalidRequest() throws Exception {
        // Create invalid request (empty title)
        TaskRequestDTO invalidRequest = new TaskRequestDTO();
        invalidRequest.setDescription("Task Description");
        invalidRequest.setTableId(TEST_TABLE_ID);
        // Title is missing

        mockMvc.perform(post("/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").exists());

        verify(taskService, times(0)).createTask(any(TaskRequestDTO.class));
    }

    @Test
    void createTask_TableNotFound() throws Exception {
        when(taskService.createTask(any(TaskRequestDTO.class)))
            .thenThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID));

        mockMvc.perform(post("/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(taskService, times(1)).createTask(any(TaskRequestDTO.class));
    }

    @Test
    void getTaskById_Success() throws Exception {
        when(taskService.getTaskById(TEST_TASK_ID)).thenReturn(taskResponse);

        mockMvc.perform(get("/tasks/{id}", TEST_TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response.title").value("Test Task"))
            .andExpect(jsonPath("$.response.description").value("Task Description"))
            .andExpect(jsonPath("$.response.tableId").value(TEST_TABLE_ID));

        verify(taskService, times(1)).getTaskById(TEST_TASK_ID);
    }

    @Test
    void getTaskById_NotFound() throws Exception {
        when(taskService.getTaskById(TEST_TASK_ID))
            .thenThrow(new TaskNotFoundException("Task not found with ID: " + TEST_TASK_ID));

        mockMvc.perform(get("/tasks/{id}", TEST_TASK_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Task not found with ID: " + TEST_TASK_ID));

        verify(taskService, times(1)).getTaskById(TEST_TASK_ID);
    }

    @Test
    void getAllTasksByTableId_Success() throws Exception {
        when(taskService.getAllTasksByTableId(TEST_TABLE_ID)).thenReturn(taskResponseList);

        mockMvc.perform(get("/tasks/table/{tableId}", TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response", hasSize(2)))
            .andExpect(jsonPath("$.response[0].id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response[1].id").value("task-id-2"));

        verify(taskService, times(1)).getAllTasksByTableId(TEST_TABLE_ID);
    }

    @Test
    void getAllTasksByTableId_EmptyList() throws Exception {
        when(taskService.getAllTasksByTableId(TEST_TABLE_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tasks/table/{tableId}", TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response").isEmpty());

        verify(taskService, times(1)).getAllTasksByTableId(TEST_TABLE_ID);
    }

    @Test
    void getAllTasksByTableId_TableNotFound() throws Exception {
        when(taskService.getAllTasksByTableId(TEST_TABLE_ID))
            .thenThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID));

        mockMvc.perform(get("/tasks/table/{tableId}", TEST_TABLE_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(taskService, times(1)).getAllTasksByTableId(TEST_TABLE_ID);
    }

    @Test
    void getMyTasks_Success() throws Exception {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
        when(taskService.getAllTasksByAssignedUserId(TEST_USER_ID)).thenReturn(taskResponseList);

        mockMvc.perform(get("/tasks/my-tasks")
            .principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response", hasSize(2)))
            .andExpect(jsonPath("$.response[0].id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response[1].id").value("task-id-2"));

        verify(authentication, times(1)).getPrincipal();
        verify(jwt, times(1)).getClaim("userId");
        verify(taskService, times(1)).getAllTasksByAssignedUserId(TEST_USER_ID);
    }

    @Test
    void getMyTasks_EmptyList() throws Exception {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
        when(taskService.getAllTasksByAssignedUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tasks/my-tasks")
            .principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response").isEmpty());

        verify(authentication, times(1)).getPrincipal();
        verify(jwt, times(1)).getClaim("userId");
        verify(taskService, times(1)).getAllTasksByAssignedUserId(TEST_USER_ID);
    }

    @Test
    void updateTask_Success() throws Exception {
        // Create an updated request
        TaskRequestDTO updateRequest = new TaskRequestDTO();
        updateRequest.setTitle("Updated Task Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setTableId(TEST_TABLE_ID);
        updateRequest.setOrderIndex(3);
        updateRequest.setAssignedUserId("other-user-id");
        updateRequest.setStatus(TaskStatus.TODO);

        // Create expected response
        TaskResponseDTO updatedResponse = new TaskResponseDTO();
        updatedResponse.setId(TEST_TASK_ID);
        updatedResponse.setTitle("Updated Task Title");
        updatedResponse.setDescription("Updated Description");
        updatedResponse.setTableId(TEST_TABLE_ID);
        updatedResponse.setOrderIndex(3);
        updatedResponse.setAssignedUserId("other-user-id");
        updatedResponse.setStatus(TaskStatus.TODO.name());

        when(taskService.updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/tasks/{id}", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response.title").value("Updated Task Title"))
            .andExpect(jsonPath("$.response.description").value("Updated Description"))
            .andExpect(jsonPath("$.response.orderIndex").value(3))
            .andExpect(jsonPath("$.response.assignedUserId").value("other-user-id"))
            .andExpect(jsonPath("$.response.status").value(TaskStatus.TODO.name()));

        verify(taskService, times(1)).updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class));
    }

    @Test
    void updateTask_NotFound() throws Exception {
        when(taskService.updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class)))
            .thenThrow(new TaskNotFoundException("Task not found with ID: " + TEST_TASK_ID));

        mockMvc.perform(put("/tasks/{id}", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Task not found with ID: " + TEST_TASK_ID));

        verify(taskService, times(1)).updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class));
    }

    @Test
    void updateTask_Unauthorized() throws Exception {
        when(taskService.updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class)))
            .thenThrow(new UnauthorizedAccessException("You don't have permission to update this task"));

        mockMvc.perform(put("/tasks/{id}", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("You don't have permission to update this task"));

        verify(taskService, times(1)).updateTask(eq(TEST_TASK_ID), any(TaskRequestDTO.class));
    }

    @Test
    void updateTask_InvalidRequest() throws Exception {
        // Create invalid request (empty title)
        TaskRequestDTO invalidRequest = new TaskRequestDTO();
        invalidRequest.setDescription("Updated Description");
        invalidRequest.setTableId(TEST_TABLE_ID);
        // Title is missing

        mockMvc.perform(put("/tasks/{id}", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").exists());

        verify(taskService, times(0)).updateTask(anyString(), any(TaskRequestDTO.class));
    }

    @Test
    void deleteTask_Success() throws Exception {
        doNothing().when(taskService).deleteTask(TEST_TASK_ID);

        mockMvc.perform(delete("/tasks/{id}", TEST_TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true));

        verify(taskService, times(1)).deleteTask(TEST_TASK_ID);
    }

    @Test
    void deleteTask_NotFound() throws Exception {
        doThrow(new TaskNotFoundException("Task not found with ID: " + TEST_TASK_ID))
            .when(taskService).deleteTask(TEST_TASK_ID);

        mockMvc.perform(delete("/tasks/{id}", TEST_TASK_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Task not found with ID: " + TEST_TASK_ID));

        verify(taskService, times(1)).deleteTask(TEST_TASK_ID);
    }

    @Test
    void deleteTask_Unauthorized() throws Exception {
        doThrow(new UnauthorizedAccessException("You don't have permission to delete this task"))
            .when(taskService).deleteTask(TEST_TASK_ID);

        mockMvc.perform(delete("/tasks/{id}", TEST_TASK_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("You don't have permission to delete this task"));

        verify(taskService, times(1)).deleteTask(TEST_TASK_ID);
    }

    @Test
    void reorderTasks_Success() throws Exception {
        List<String> taskIds = Arrays.asList(TEST_TASK_ID, "task-id-2");
        
        doNothing().when(taskService).reorderTasks(TEST_TABLE_ID, taskIds);

        mockMvc.perform(put("/tasks/table/{tableId}/reorder", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskIds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true));

        verify(taskService, times(1)).reorderTasks(TEST_TABLE_ID, taskIds);
    }

    @Test
    void reorderTasks_TableNotFound() throws Exception {
        List<String> taskIds = Arrays.asList(TEST_TASK_ID, "task-id-2");
        
        doThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID))
            .when(taskService).reorderTasks(TEST_TABLE_ID, taskIds);

        mockMvc.perform(put("/tasks/table/{tableId}/reorder", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskIds)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(taskService, times(1)).reorderTasks(TEST_TABLE_ID, taskIds);
    }

    @Test
    void reorderTasks_TaskNotFound() throws Exception {
        List<String> taskIds = Arrays.asList(TEST_TASK_ID, "nonexistent-task-id");
        
        doThrow(new TaskNotFoundException("Task not found with ID: nonexistent-task-id"))
            .when(taskService).reorderTasks(TEST_TABLE_ID, taskIds);

        mockMvc.perform(put("/tasks/table/{tableId}/reorder", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskIds)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Task not found with ID: nonexistent-task-id"));

        verify(taskService, times(1)).reorderTasks(TEST_TABLE_ID, taskIds);
    }

    @Test
    void getUpcomingTasks_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        // LocalDateTime nextDay = now.plusHours(24);
        
        when(taskService.findByDueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(taskResponseList);

        mockMvc.perform(get("/tasks/upcoming"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$[1].id").value("task-id-2"));

        verify(taskService, times(1)).findByDueDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void updateTaskStatus_Success() throws Exception {
        TaskStatus newStatus = TaskStatus.COMPLETED;
        
        // Updated response with completed status
        TaskResponseDTO completedTask = new TaskResponseDTO();
        completedTask.setId(TEST_TASK_ID);
        completedTask.setTitle("Test Task");
        completedTask.setStatus(TaskStatus.COMPLETED.name());
        completedTask.setTableId(TEST_TABLE_ID);
        
        when(taskService.updateTaskStatus(TEST_TASK_ID, newStatus)).thenReturn(completedTask);

        mockMvc.perform(patch("/tasks/{id}/status", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("\"COMPLETED\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response.status").value(TaskStatus.COMPLETED.name()));

        verify(taskService, times(1)).updateTaskStatus(TEST_TASK_ID, newStatus);
    }

    @Test
    void updateTaskStatus_TaskNotFound() throws Exception {
        when(taskService.updateTaskStatus(TEST_TASK_ID, TaskStatus.COMPLETED))
            .thenThrow(new TaskNotFoundException("Task not found with ID: " + TEST_TASK_ID));

        mockMvc.perform(patch("/tasks/{id}/status", TEST_TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("\"COMPLETED\""))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Task not found with ID: " + TEST_TASK_ID));

        verify(taskService, times(1)).updateTaskStatus(TEST_TASK_ID, TaskStatus.COMPLETED);
    }

    @Test
    void getTasksByStatus_Success() throws Exception {
        when(taskService.getAllTasksByStatus(TaskStatus.TODO)).thenReturn(List.of(taskResponse));

        mockMvc.perform(get("/tasks/status/{status}", TaskStatus.TODO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response", hasSize(1)))
            .andExpect(jsonPath("$.response[0].id").value(TEST_TASK_ID))
            .andExpect(jsonPath("$.response[0].status").value(TaskStatus.TODO.name()));

        verify(taskService, times(1)).getAllTasksByStatus(TaskStatus.TODO);
    }

    @Test
    void getTasksByStatus_EmptyList() throws Exception {
        when(taskService.getAllTasksByStatus(TaskStatus.COMPLETED)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tasks/status/{status}", TaskStatus.COMPLETED))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response").isEmpty());

        verify(taskService, times(1)).getAllTasksByStatus(TaskStatus.COMPLETED);
    }

    @Test
    void getTaskStatistics_Success() throws Exception {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
        Map<TaskStatus, Long> statistics = new HashMap<>();
        statistics.put(TaskStatus.TODO, 2L);
        statistics.put(TaskStatus.COMPLETED, 5L);
        
        when(taskService.getTaskStatistics(TEST_USER_ID)).thenReturn(statistics);

        mockMvc.perform(get("/tasks/statistics")
            .principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.TODO").value(2))
            .andExpect(jsonPath("$.COMPLETED").value(5));

        verify(authentication, times(1)).getPrincipal();
        verify(jwt, times(1)).getClaim("userId");
        verify(taskService, times(1)).getTaskStatistics(TEST_USER_ID);
    }

    @Test
    void getTaskStatistics_EmptyStatistics() throws Exception {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
        Map<TaskStatus, Long> statistics = new HashMap<>();
        
        when(taskService.getTaskStatistics(TEST_USER_ID)).thenReturn(statistics);

        mockMvc.perform(get("/tasks/statistics")
            .principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        verify(authentication, times(1)).getPrincipal();
        verify(jwt, times(1)).getClaim("userId");
        verify(taskService, times(1)).getTaskStatistics(TEST_USER_ID);
    }
}